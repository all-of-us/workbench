import * as React from 'react';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';
import { validate } from 'validate.js';

import {
  DataprocConfig,
  GpuConfig,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';

import { cond, switchCase } from '@terra-ui-packages/core-utils';
import { Button, LinkButton } from 'app/components/buttons';
import { DeletePersistentDiskButton } from 'app/components/delete-persistent-disk-button';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ErrorMessage, WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import { ConfirmDelete } from 'app/components/runtime-configuration-panel/confirm-delete';
import { ConfirmDeleteEnvironmentWithPD } from 'app/components/runtime-configuration-panel/confirm-delete-environment-with-pd';
import { ConfirmDeleteUnattachedPD } from 'app/components/runtime-configuration-panel/confirm-delete-unattached-pd';
import { ConfirmUpdatePanel } from 'app/components/runtime-configuration-panel/confirm-update-panel';
import { DataProcConfigSelector } from 'app/components/runtime-configuration-panel/dataproc-config-selector';
import { DisabledPanel } from 'app/components/runtime-configuration-panel/disabled-panel';
import { DiskSelector } from 'app/components/runtime-configuration-panel/disk-selector';
import { GpuConfigSelector } from 'app/components/runtime-configuration-panel/gpu-config-selector';
import { MachineSelector } from 'app/components/runtime-configuration-panel/machine-selector';
import { OfferDeleteDiskWithUpdate } from 'app/components/runtime-configuration-panel/offer-delete-disk-with-update';
import { SparkConsolePanel } from 'app/components/runtime-configuration-panel/spark-console-panel';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { RuntimeSummary } from 'app/components/runtime-summary';
import { Spinner } from 'app/components/spinners';
import { disksApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  summarizeErrors,
  withCdrVersions,
  withCurrentWorkspace,
} from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  Machine,
  machineRunningCost,
  MIN_DISK_SIZE_GB,
  validLeoDataprocMasterMachineTypes,
  validLeoGceMachineTypes,
} from 'app/utils/machines';
import { applyPresetOverride, runtimePresets } from 'app/utils/runtime-presets';
import {
  AnalysisConfig,
  AnalysisDiff,
  diffsToUpdateMessaging,
  fromAnalysisConfig,
  getAnalysisConfigDiffs,
  isActionable,
  isVisible,
  maybeWithExistingDisk,
  PanelContent,
  RuntimeStatusRequest,
  toAnalysisConfig,
  UpdateMessaging,
  useCustomRuntime,
  useRuntimeStatus,
  withAnalysisConfigDefaults,
} from 'app/utils/runtime-utils';
import {
  ProfileStore,
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { UIAppType } from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';

const { useState, useEffect, Fragment } = React;

const CreatePanel = ({
  profile,
  setPanelContent,
  workspace,
  analysisConfig,
  creatorFreeCreditsRemaining,
  status,
  setRuntimeStatus,
}) => {
  const displayName =
    analysisConfig.computeType === ComputeType.Dataproc
      ? runtimePresets.hailAnalysis.displayName
      : runtimePresets.generalAnalysis.displayName;

  return (
    <div data-test-id='runtime-create-panel' style={styles.controlSection}>
      <EnvironmentInformedActionPanel
        {...{
          creatorFreeCreditsRemaining,
          profile,
          workspace,
          analysisConfig,
          status,
        }}
        onPause={() => setRuntimeStatus(RuntimeStatusRequest.Stop)}
        onResume={() => setRuntimeStatus(RuntimeStatusRequest.Start)}
        appType={UIAppType.JUPYTER}
      />
      <FlexRow
        style={{ justifyContent: 'space-between', alignItems: 'center' }}
      >
        <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
          Recommended Environment for {displayName}
        </h3>
        <Button
          type='secondarySmall'
          onClick={() => setPanelContent(PanelContent.Customize)}
          aria-label='Customize'
        >
          Customize
        </Button>
      </FlexRow>
      <RuntimeSummary analysisConfig={analysisConfig} />
    </div>
  );
};

// Select a recommended preset configuration.
export const PresetSelector = ({
  allowDataproc,
  setAnalysisConfig,
  disabled,
  gcePersistentDisk,
}) => {
  return (
    <Dropdown
      id='runtime-presets-menu'
      appendTo='self'
      disabled={disabled}
      style={{
        marginTop: '21px',
        color: colors.primary,
      }}
      placeholder='Recommended environments'
      options={fp.flow(
        fp.values,
        fp.filter(
          ({ runtimeTemplate }) =>
            allowDataproc || !runtimeTemplate.dataprocConfig
        ),
        fp.map(({ displayName, runtimeTemplate }) => ({
          label: displayName,
          value: runtimeTemplate,
        }))
      )(runtimePresets)}
      onChange={({ value }) => {
        setAnalysisConfig(toAnalysisConfig(value, gcePersistentDisk));

        // Return false to skip the normal handling of the value selection. We're
        // abusing the dropdown here to act as if it were a menu instead.
        // Therefore, we never want the empty "placeholder" text to change to a
        // selected value (it should always read "recommended environments"). The presets
        // are not persistent state, they just snap the rest of the form to a particular configuration.
        // See RW-5996 for more details.
        return false;
      }}
    />
  );
};

const PanelMain = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(
  ({
    cdrVersionTiersResponse,
    workspace,
    profileState,
    onClose = () => {},
    initialPanelContent,
    creatorFreeCreditsRemaining,
  }) => {
    const { profile } = profileState;
    const { namespace, cdrVersionId, googleProject } = workspace;

    const { hasWgsData: allowDataproc } = findCdrVersion(
      cdrVersionId,
      cdrVersionTiersResponse
    ) || { hasWgsData: false };

    const { gcePersistentDisk } = useStore(runtimeDiskStore);
    let [{ currentRuntime, pendingRuntime }, setRuntimeRequest] =
      useCustomRuntime(namespace, gcePersistentDisk);

    // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
    if (currentRuntime && currentRuntime.status === RuntimeStatus.DELETED) {
      currentRuntime = applyPresetOverride(
        // The attached disk information is lost for deleted runtimes. In any case,
        // by default we want to offer that the user reattach their existing disk,
        // if any and if the configuration allows it.
        maybeWithExistingDisk(currentRuntime, gcePersistentDisk)
      );
    }

    const [status, setRuntimeStatus] = useRuntimeStatus(
      namespace,
      googleProject
    );

    // Prioritize the "pendingRuntime", if any. When an update is pending, we want
    // to render the target runtime details, which  may not match the current runtime.
    const existingRuntime =
      pendingRuntime || currentRuntime || ({} as Partial<Runtime>);
    const existingAnalysisConfig = toAnalysisConfig(
      existingRuntime,
      gcePersistentDisk
    );

    const [analysisConfig, setAnalysisConfig] = useState(
      withAnalysisConfigDefaults(existingAnalysisConfig, gcePersistentDisk)
    );

    // TODO: simplify the state logic here!  At least, try to understand why this is happening.
    // Somehow, only setting the analysisConfig in the above useState() step means that
    // a gcePersistentDisk update from the diskStore does not always cause analysisConfig to be updated.
    // So we must set this explicit dependency trigger instead.
    useEffect(
      () =>
        setAnalysisConfig(
          withAnalysisConfigDefaults(analysisConfig, gcePersistentDisk)
        ),
      [gcePersistentDisk]
    );

    const requestAnalysisConfig = (config: AnalysisConfig) =>
      setRuntimeRequest({
        runtime: fromAnalysisConfig(config),
        detachedDisk: config.detachedDisk,
      });

    const initializePanelContent = (): PanelContent =>
      cond<PanelContent>(
        [!!initialPanelContent, () => initialPanelContent],
        // If there's a pendingRuntime, this means there's already a create/update
        // in progress, even if the runtime store doesn't actively reflect this yet.
        // Show the customize panel in this event.
        [!!pendingRuntime, () => PanelContent.Customize],
        [
          currentRuntime === null ||
            currentRuntime === undefined ||
            status === RuntimeStatus.UNKNOWN,
          () => PanelContent.Create,
        ],
        [
          // General Analysis consist of GCE + PD. Display create page only if
          // 1) currentRuntime + pd both are deleted and
          // 2) configurationType is either GeneralAnalysis or HailGenomicAnalysis
          currentRuntime?.status === RuntimeStatus.DELETED &&
            !currentRuntime?.gceWithPdConfig &&
            (
              [
                RuntimeConfigurationType.GENERAL_ANALYSIS,
                RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
              ] as Array<RuntimeConfigurationType>
            ).includes(currentRuntime?.configurationType),
          () => PanelContent.Create,
        ],
        () => PanelContent.Customize
      );

    const [panelContent, setPanelContent] = useState<PanelContent>(
      initializePanelContent()
    );

    const validMainMachineTypes =
      analysisConfig.computeType === ComputeType.Standard
        ? validLeoGceMachineTypes
        : validLeoDataprocMasterMachineTypes;
    // The compute type affects the set of valid machine types, so revert to the
    // default machine type if switching compute types would invalidate the main
    // machine type choice.
    useEffect(() => {
      if (
        !validMainMachineTypes.find(
          ({ name }) => name === analysisConfig.machine.name
        )
      ) {
        setAnalysisConfig({
          ...analysisConfig,
          machine: existingAnalysisConfig.machine,
        });
      }
    }, [analysisConfig.computeType]);

    const runtimeExists = (status && isVisible(status)) || !!pendingRuntime;
    const disableControls = runtimeExists && !isActionable(status);

    const dataprocExists =
      runtimeExists && existingAnalysisConfig.dataprocConfig !== null;

    const attachedPdExists =
      !!gcePersistentDisk &&
      runtimeExists &&
      existingAnalysisConfig.diskConfig.detachable;
    const unattachedPdExists = !!gcePersistentDisk && !attachedPdExists;
    const unattachedDiskNeedsRecreate =
      unattachedPdExists &&
      analysisConfig.diskConfig.detachable &&
      (gcePersistentDisk.size > analysisConfig.diskConfig.size ||
        gcePersistentDisk.diskType !==
          analysisConfig.diskConfig.detachableType);

    const disableDetachableReason = cond<string>(
      [
        analysisConfig.computeType === ComputeType.Dataproc,
        () => 'Reattachable disks are unsupported for this compute type',
      ],
      () => null
    );

    let configDiffs: AnalysisDiff[] = [];
    let updateMessaging: UpdateMessaging;
    if (runtimeExists) {
      configDiffs = getAnalysisConfigDiffs(
        existingAnalysisConfig,
        analysisConfig
      );
      updateMessaging = diffsToUpdateMessaging(configDiffs);
    }
    const environmentChanged = configDiffs.length > 0;

    // Leonardo enforces a minimum limit for disk size, 4000 GB is our arbitrary limit for not making a
    // disk that is way too big and expensive on free tier ($.22 an hour). 64 TB is the GCE limit on
    // persistent disk.
    const diskSizeValidatorWithMessage = (
      diskType = 'standard' || 'master' || 'worker'
    ) => {
      const maxDiskSize = isUsingFreeTierBillingAccount(workspace)
        ? 4000
        : 64000;
      const minDiskSize =
        analysisConfig.computeType === ComputeType.Dataproc
          ? DATAPROC_MIN_DISK_SIZE_GB
          : MIN_DISK_SIZE_GB;
      const message = {
        standard: `^Disk size must be between ${minDiskSize} and ${maxDiskSize} GB`,
        master: `^Master disk size must be between ${DATAPROC_MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
        worker: `^Worker disk size must be between ${DATAPROC_MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
      };

      return {
        numericality: {
          greaterThanOrEqualTo:
            analysisConfig.computeType === ComputeType.Dataproc
              ? DATAPROC_MIN_DISK_SIZE_GB
              : MIN_DISK_SIZE_GB,
          lessThanOrEqualTo: maxDiskSize,
          message: message[diskType],
        },
      };
    };

    const costErrorsAsWarnings =
      !isUsingFreeTierBillingAccount(workspace) ||
      // We've increased the workspace creator's free credits. This means they may be expecting to run
      // a more expensive analysis, and the program has extended some further trust for free credit
      // use. Allow them to provision a larger runtime (still warn them). Block them if they get below
      // the default amount of free credits because (1) this can result in overspend and (2) we have
      // easy access to remaining credits, and not the creator's quota.
      creatorFreeCreditsRemaining >
        serverConfigStore.get().config.defaultFreeCreditsDollarLimit;

    const runningCostValidatorWithMessage = () => {
      const maxRunningCost = isUsingFreeTierBillingAccount(workspace)
        ? 25
        : 150;
      const message = costErrorsAsWarnings
        ? '^Your runtime is expensive. Are you sure you wish to proceed?'
        : `^Your runtime is too expensive. To proceed using free credits, reduce your running costs below $${maxRunningCost}/hr.`;
      return {
        numericality: {
          lessThan: maxRunningCost,
          message: message,
        },
      };
    };

    const currentRunningCost = machineRunningCost(analysisConfig);

    const diskValidator = {
      diskSize: diskSizeValidatorWithMessage('standard'),
    };

    const runningCostValidator = {
      currentRunningCost: runningCostValidatorWithMessage(),
    };
    // We don't clear dataproc config when we change compute type so we can't combine this with the
    // above or else we can end up with phantom validation fails
    const dataprocValidators = {
      masterDiskSize: diskSizeValidatorWithMessage('master'),
      workerDiskSize: diskSizeValidatorWithMessage('worker'),
      numberOfWorkers: {
        numericality: {
          greaterThanOrEqualTo: 2,
          message: 'Dataproc requires at least 2 worker nodes',
        },
      },
    };

    const { masterDiskSize, workerDiskSize, numberOfWorkers } =
      analysisConfig.dataprocConfig || {};
    const diskErrors = validate(
      { diskSize: analysisConfig.diskConfig.size },
      diskValidator
    );
    const runningCostErrors = validate(
      { currentRunningCost },
      runningCostValidator
    );
    const dataprocErrors =
      analysisConfig.computeType === ComputeType.Dataproc
        ? validate(
            { masterDiskSize, workerDiskSize, numberOfWorkers },
            dataprocValidators
          )
        : undefined;

    const getErrorMessageContent = () => {
      const errorDivs = [];
      if (diskErrors) {
        errorDivs.push(summarizeErrors(diskErrors));
      }
      if (dataprocErrors) {
        errorDivs.push(summarizeErrors(dataprocErrors));
      }
      if (!costErrorsAsWarnings && runningCostErrors) {
        errorDivs.push(summarizeErrors(runningCostErrors));
      }
      return errorDivs;
    };

    const getWarningMessageContent = () => {
      const warningDivs = [];
      if (costErrorsAsWarnings && runningCostErrors) {
        warningDivs.push(summarizeErrors(runningCostErrors));
      }
      return warningDivs;
    };

    // For computeType Standard: We are moving away from storage disk as Standard
    // As part of RW-9167, we are disabling Standard storage disk if computeType is standard
    // Eventually we will be removing this option altogether
    const runtimeCanBeCreated =
      !(getErrorMessageContent().length > 0) &&
      ((analysisConfig.computeType === ComputeType.Standard &&
        analysisConfig.diskConfig.detachable) ||
        (analysisConfig.computeType === ComputeType.Dataproc &&
          !analysisConfig.diskConfig.detachable));
    // Casting to RuntimeStatus here because it can't easily be done at the destructuring level
    // where we get 'status' from
    const runtimeCanBeUpdated =
      environmentChanged &&
      (
        [RuntimeStatus.RUNNING, RuntimeStatus.STOPPED] as Array<RuntimeStatus>
      ).includes(status as RuntimeStatus) &&
      runtimeCanBeCreated;

    const renderUpdateButton = () => {
      return (
        <Button
          aria-label='Update'
          disabled={!runtimeCanBeUpdated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          {updateMessaging.applyAction}
        </Button>
      );
    };

    const renderCreateButton = () => {
      return (
        <Button
          aria-label='Create'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          Create
        </Button>
      );
    };

    const renderNextWithDiskDeleteButton = () => {
      return (
        <Button
          aria-label='Next'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            setPanelContent(PanelContent.DeleteUnattachedPdAndCreate);
          }}
        >
          Next
        </Button>
      );
    };

    const renderTryAgainButton = () => {
      return (
        <Button
          aria-label='Try Again'
          disabled={!runtimeCanBeCreated}
          onClick={() => {
            requestAnalysisConfig(analysisConfig);
            onClose();
          }}
        >
          Try Again
        </Button>
      );
    };

    const updateYieldsUnusedDisk =
      existingAnalysisConfig.diskConfig.detachable &&
      !analysisConfig.diskConfig.detachable;
    const renderNextUpdateButton = () => {
      return (
        <Button
          aria-label='Next'
          disabled={!runtimeCanBeUpdated}
          onClick={() => {
            if (updateYieldsUnusedDisk) {
              setPanelContent(PanelContent.ConfirmUpdateWithDiskDelete);
            } else {
              setPanelContent(PanelContent.ConfirmUpdate);
            }
          }}
        >
          Next
        </Button>
      );
    };

    const usingDataproc = analysisConfig.computeType === ComputeType.Dataproc;

    return (
      <div id='runtime-panel'>
        {cond<React.ReactNode>(
          [
            [PanelContent.Create, PanelContent.Customize].includes(
              panelContent
            ),
            () => (
              <div style={{ marginBottom: '1.5rem' }}>
                Your analysis environment consists of an application and compute
                resources. Your cloud environment is unique to this workspace
                and not shared with other users.
              </div>
            ),
          ],
          () => null
        )}
        {switchCase(
          panelContent,
          [
            PanelContent.Create,
            () => (
              <Fragment>
                <CreatePanel
                  {...{
                    analysisConfig,
                    creatorFreeCreditsRemaining,
                    profile,
                    setPanelContent,
                    setRuntimeStatus,
                    status,
                    workspace,
                  }}
                />
                <FlexRow
                  style={{ justifyContent: 'flex-end', marginTop: '1.5rem' }}
                >
                  {renderCreateButton()}
                </FlexRow>
              </Fragment>
            ),
          ],
          [
            PanelContent.DeleteRuntime,
            () => {
              if (attachedPdExists) {
                return (
                  <ConfirmDeleteEnvironmentWithPD
                    onConfirm={async (deletePDSelected) => {
                      const runtimeStatusReq = cond(
                        [
                          !deletePDSelected,
                          () => RuntimeStatusRequest.DeleteRuntime,
                        ],
                        [
                          deletePDSelected && !usingDataproc,
                          () => RuntimeStatusRequest.DeleteRuntimeAndPD,
                        ],
                        [
                          // TODO: this configuration is not supported.  Remove?
                          deletePDSelected && usingDataproc,
                          () => RuntimeStatusRequest.DeletePD,
                        ]
                      );
                      await setRuntimeStatus(runtimeStatusReq);
                      onClose();
                    }}
                    onCancel={() => setPanelContent(PanelContent.Customize)}
                    appType={UIAppType.JUPYTER}
                    usingDataproc={usingDataproc}
                    disk={gcePersistentDisk}
                  />
                );
              } else {
                return (
                  <ConfirmDelete
                    onConfirm={async () => {
                      await setRuntimeStatus(
                        RuntimeStatusRequest.DeleteRuntime
                      );
                      onClose();
                    }}
                    onCancel={() => setPanelContent(PanelContent.Customize)}
                  />
                );
              }
            },
          ],
          [
            PanelContent.DeleteUnattachedPd,
            () => (
              <ConfirmDeleteUnattachedPD
                appType={UIAppType.JUPYTER}
                onConfirm={async () => {
                  await disksApi().deleteDisk(
                    namespace,
                    gcePersistentDisk.name
                  );
                  onClose();
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
              />
            ),
          ],
          [
            PanelContent.DeleteUnattachedPdAndCreate,
            () => (
              <ConfirmDeleteUnattachedPD
                appType={UIAppType.JUPYTER}
                showCreateMessaging
                onConfirm={async () => {
                  await disksApi().deleteDisk(
                    namespace,
                    gcePersistentDisk.name
                  );
                  requestAnalysisConfig(analysisConfig);
                  onClose();
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
              />
            ),
          ],
          [
            PanelContent.Customize,
            () => (
              <div style={{ marginBottom: '10px' }}>
                <div style={styles.controlSection}>
                  <EnvironmentInformedActionPanel
                    {...{
                      creatorFreeCreditsRemaining,
                      profile,
                      workspace,
                      analysisConfig,
                      status,
                      environmentChanged,
                    }}
                    onPause={() => setRuntimeStatus(RuntimeStatusRequest.Stop)}
                    onResume={() =>
                      setRuntimeStatus(RuntimeStatusRequest.Start)
                    }
                    appType={UIAppType.JUPYTER}
                  />
                  {currentRuntime?.errors &&
                    currentRuntime.errors.length > 0 && (
                      <ErrorMessage iconPosition={'top'} iconSize={16}>
                        <div>
                          An error was encountered with your cloud environment.
                          Please re-attempt creation of the environment and
                          contact support if the error persists.
                        </div>
                        <div>Error details:</div>
                        {currentRuntime.errors.map((err, idx) => {
                          return (
                            <div style={{ fontFamily: 'monospace' }} key={idx}>
                              {err.errorMessage}
                            </div>
                          );
                        })}
                      </ErrorMessage>
                    )}
                  <PresetSelector
                    {...{
                      allowDataproc,
                      setAnalysisConfig,
                      gcePersistentDisk,
                      disabled: disableControls,
                    }}
                  />
                  {/* Runtime customization: change detailed machine configuration options. */}
                  <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
                    Cloud compute profile
                  </h3>
                  <div style={styles.formGrid3}>
                    <MachineSelector
                      idPrefix='runtime'
                      disabled={disableControls}
                      selectedMachine={analysisConfig.machine}
                      onChange={(machine: Machine) =>
                        setAnalysisConfig({ ...analysisConfig, machine })
                      }
                      validMachineTypes={validMainMachineTypes}
                      machineType={analysisConfig.machine.name}
                    />
                  </div>
                  {analysisConfig.computeType === ComputeType.Standard && (
                    <GpuConfigSelector
                      disabled={disableControls}
                      onChange={(gpuConfig: GpuConfig) =>
                        setAnalysisConfig({ ...analysisConfig, gpuConfig })
                      }
                      selectedMachine={analysisConfig.machine}
                      gpuConfig={analysisConfig.gpuConfig}
                    />
                  )}
                  <FlexRow
                    style={{
                      marginTop: '1.5rem',
                      justifyContent: 'space-between',
                    }}
                  >
                    <FlexColumn>
                      <label style={styles.label} htmlFor='runtime-compute'>
                        Compute type
                      </label>
                      <FlexRow style={{ gap: '10px', alignItems: 'center' }}>
                        <Dropdown
                          id='runtime-compute'
                          appendTo='self'
                          disabled={!allowDataproc || disableControls}
                          style={{ width: '15rem' }}
                          options={[ComputeType.Standard, ComputeType.Dataproc]}
                          value={
                            analysisConfig.computeType || ComputeType.Standard
                          }
                          onChange={({ value: computeType }) =>
                            // When the compute type changes, we need to normalize the config and potentially restore defualts.
                            setAnalysisConfig(
                              withAnalysisConfigDefaults(
                                { ...analysisConfig, computeType },
                                gcePersistentDisk
                              )
                            )
                          }
                        />
                        {analysisConfig.computeType ===
                          ComputeType.Dataproc && (
                          <TooltipTrigger
                            content={
                              status !== RuntimeStatus.RUNNING
                                ? 'Start your Dataproc cluster to access the Spark console'
                                : null
                            }
                          >
                            <LinkButton
                              data-test-id='manage-spark-console'
                              disabled={
                                status !== RuntimeStatus.RUNNING ||
                                existingAnalysisConfig.computeType !==
                                  ComputeType.Dataproc
                              }
                              onClick={() =>
                                setPanelContent(PanelContent.SparkConsole)
                              }
                            >
                              Manage and monitor Spark console
                            </LinkButton>
                          </TooltipTrigger>
                        )}
                      </FlexRow>
                    </FlexColumn>
                  </FlexRow>
                  {analysisConfig.computeType === ComputeType.Dataproc && (
                    <DataProcConfigSelector
                      disabled={disableControls}
                      runtimeStatus={status}
                      dataprocExists={dataprocExists}
                      onChange={(dataprocConfig: DataprocConfig) =>
                        setAnalysisConfig({ ...analysisConfig, dataprocConfig })
                      }
                      dataprocConfig={analysisConfig.dataprocConfig}
                    />
                  )}
                  <FlexRow
                    style={{
                      marginTop: '1.5rem',
                      justifyContent: 'space-between',
                    }}
                  >
                    <FlexColumn>
                      <label style={styles.label} htmlFor='runtime-autopause'>
                        Automatically pause after idle for
                      </label>
                      <Dropdown
                        id='runtime-autopause'
                        appendTo='self'
                        disabled={disableControls}
                        style={{ width: '15rem' }}
                        options={Array.from(
                          AutopauseMinuteThresholds.entries()
                        ).map((entry) => ({
                          label: entry[1],
                          value: entry[0],
                        }))}
                        value={
                          analysisConfig.autopauseThreshold ||
                          DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
                        }
                        onChange={({ value: autopauseThreshold }) =>
                          setAnalysisConfig({
                            ...analysisConfig,
                            autopauseThreshold,
                          })
                        }
                      />
                    </FlexColumn>
                  </FlexRow>
                </div>
                <DiskSelector
                  diskConfig={analysisConfig.diskConfig}
                  onChange={(diskConfig) =>
                    setAnalysisConfig({
                      ...analysisConfig,
                      diskConfig,
                      detachedDisk: diskConfig.detachable
                        ? null
                        : gcePersistentDisk,
                    })
                  }
                  disabled={disableControls}
                  disableDetachableReason={disableDetachableReason}
                  existingDisk={gcePersistentDisk}
                  computeType={analysisConfig.computeType}
                />
                {runtimeExists && updateMessaging.warn && (
                  <WarningMessage iconSize={30} iconPosition={'center'}>
                    <div>{updateMessaging.warn}</div>
                  </WarningMessage>
                )}
                {getErrorMessageContent().length > 0 && (
                  <ErrorMessage
                    iconSize={16}
                    iconPosition={'top'}
                    data-test-id={'runtime-error-messages'}
                  >
                    {getErrorMessageContent()}
                  </ErrorMessage>
                )}
                {getWarningMessageContent().length > 0 && (
                  <WarningMessage
                    iconSize={16}
                    iconPosition={'top'}
                    data-test-id={'runtime-warning-messages'}
                  >
                    {getWarningMessageContent()}
                  </WarningMessage>
                )}
                {unattachedPdExists ? (
                  <FlexRow
                    style={{
                      justifyContent: 'space-between',
                      marginTop: '1.125rem',
                    }}
                  >
                    <DeletePersistentDiskButton
                      onClick={() =>
                        setPanelContent(PanelContent.DeleteUnattachedPd)
                      }
                    />
                    {unattachedDiskNeedsRecreate
                      ? renderNextWithDiskDeleteButton()
                      : renderCreateButton()}
                  </FlexRow>
                ) : (
                  <FlexRow
                    style={{
                      justifyContent: 'space-between',
                      marginTop: '1.125rem',
                    }}
                  >
                    <LinkButton
                      style={{
                        ...styles.deleteLink,
                        ...(disableControls || !runtimeExists
                          ? { color: colorWithWhiteness(colors.dark, 0.4) }
                          : {}),
                      }}
                      aria-label='Delete Environment'
                      disabled={disableControls || !runtimeExists}
                      onClick={() =>
                        setPanelContent(PanelContent.DeleteRuntime)
                      }
                    >
                      Delete Environment
                    </LinkButton>
                    {cond(
                      [runtimeExists, () => renderNextUpdateButton()],
                      [
                        unattachedDiskNeedsRecreate,
                        () => renderNextWithDiskDeleteButton(),
                      ],
                      [
                        currentRuntime?.errors &&
                          currentRuntime.errors.length > 0,
                        () => renderTryAgainButton(),
                      ],
                      () => renderCreateButton()
                    )}
                  </FlexRow>
                )}
              </div>
            ),
          ],
          [
            PanelContent.ConfirmUpdate,
            () => (
              <ConfirmUpdatePanel
                existingAnalysisConfig={existingAnalysisConfig}
                newAnalysisConfig={analysisConfig}
                onCancel={() => {
                  setPanelContent(PanelContent.Customize);
                }}
                updateButton={renderUpdateButton()}
              />
            ),
          ],
          [
            PanelContent.ConfirmUpdateWithDiskDelete,
            () => (
              <OfferDeleteDiskWithUpdate
                onNext={(deleteDetachedDisk: boolean) => {
                  if (deleteDetachedDisk) {
                    setAnalysisConfig({
                      ...analysisConfig,
                      detachedDisk: null,
                    });
                  }
                  setPanelContent(PanelContent.ConfirmUpdate);
                }}
                onCancel={() => setPanelContent(PanelContent.Customize)}
                disk={gcePersistentDisk}
              />
            ),
          ],
          [PanelContent.Disabled, () => <DisabledPanel />],
          [
            PanelContent.SparkConsole,
            () => <SparkConsolePanel {...workspace} />,
          ]
        )}
      </div>
    );
  }
);

export interface RuntimeConfigurationPanelProps {
  onClose?: () => void;
  initialPanelContent?: PanelContent;
  creatorFreeCreditsRemaining?: number;
  profileState: ProfileStore;
}

export const RuntimeConfigurationPanel = ({
  onClose = () => {},
  initialPanelContent = null,
  creatorFreeCreditsRemaining = null,
  profileState,
}: RuntimeConfigurationPanelProps) => {
  const { runtimeLoaded } = useStore(runtimeStore);
  if (!runtimeLoaded) {
    return <Spinner style={{ width: '100%', marginTop: '7.5rem' }} />;
  }

  // TODO: can we remove this indirection?
  return (
    <PanelMain
      {...{
        onClose,
        initialPanelContent,
        creatorFreeCreditsRemaining,
        profileState,
      }}
    />
  );
};
