import * as React from 'react';
import * as fp from 'lodash/fp';
import { validate } from 'validate.js';

import {
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';

import { cond, switchCase } from '@terra-ui-packages/core-utils';
import { Spinner } from 'app/components/spinners';
import { disksApi } from 'app/services/swagger-fetch-clients';
import {
  summarizeErrors,
  WithCdrVersions,
  withCdrVersions,
  WithCurrentWorkspace,
  withCurrentWorkspace,
} from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import {
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  machineRunningCost,
  MIN_DISK_SIZE_GB,
  validLeoDataprocMasterMachineTypes,
  validLeoGceMachineTypes,
} from 'app/utils/machines';
import { applyPresetOverride } from 'app/utils/runtime-presets';
import {
  AnalysisConfig,
  AnalysisDiff,
  diffsToUpdateMessaging,
  fromAnalysisConfig,
  getAnalysisConfigDiffs,
  isVisible,
  maybeWithPersistentDisk,
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
import { ConfirmDelete } from './common-env-conf-panels/confirm-delete';
import { ConfirmDeleteEnvironmentWithPD } from './common-env-conf-panels/confirm-delete-environment-with-pd';
import { ConfirmDeleteUnattachedPD } from './common-env-conf-panels/confirm-delete-unattached-pd';
import { DisabledPanel } from './common-env-conf-panels/disabled-panel';
import { ConfirmUpdatePanel } from './runtime-configuration-panel/confirm-update-panel';
import { CreateButton } from './runtime-configuration-panel/create-button';
import { CreatePanel } from './runtime-configuration-panel/create-panel';
import { CustomizePanel } from './runtime-configuration-panel/customize-panel';
import { OfferDeleteDiskWithUpdate } from './runtime-configuration-panel/offer-delete-disk-with-update';
import { SparkConsolePanel } from './runtime-configuration-panel/spark-console-panel';
import { UpdateButton } from './runtime-configuration-panel/update-button';

const { useState, useEffect } = React;

export interface RuntimeConfigurationPanelProps {
  onClose?: () => void;
  initialPanelContent?: PanelContent;
  creatorFreeCreditsRemaining?: number;
  profileState: ProfileStore;
}
export const RuntimeConfigurationPanel = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)(
  ({
    onClose = () => {},
    initialPanelContent = null,
    creatorFreeCreditsRemaining = null,
    profileState,
    cdrVersionTiersResponse,
    workspace,
  }: RuntimeConfigurationPanelProps &
    WithCdrVersions &
    WithCurrentWorkspace) => {
    const { runtimeLoaded } = useStore(runtimeStore);
    if (!runtimeLoaded) {
      return <Spinner style={{ width: '100%', marginTop: '7.5rem' }} />;
    }

    const { profile } = profileState;
    const { namespace, cdrVersionId, googleProject } = workspace;

    const { gcePersistentDisk } = useStore(runtimeDiskStore);
    let [{ currentRuntime, pendingRuntime }, setRuntimeRequest] =
      useCustomRuntime(namespace, gcePersistentDisk);

    // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
    if (currentRuntime && currentRuntime.status === RuntimeStatus.DELETED) {
      currentRuntime = applyPresetOverride(
        // The attached disk information is lost for deleted runtimes. In any case,
        // by default we want to offer that the user reattach their existing disk,
        // if any and if the configuration allows it.
        maybeWithPersistentDisk(currentRuntime, gcePersistentDisk)
      );
    }

    const [status, setRuntimeStatusRequest] = useRuntimeStatus(
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

    const attachedPdExists =
      !!gcePersistentDisk &&
      runtimeExists &&
      existingAnalysisConfig.diskConfig.detachable;

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
              <CreatePanel
                {...{
                  analysisConfig,
                  creatorFreeCreditsRemaining,
                  onClose,
                  profile,
                  requestAnalysisConfig,
                  runtimeCanBeCreated,
                  setPanelContent,
                  status,
                  workspace,
                }}
                onPause={() =>
                  setRuntimeStatusRequest(RuntimeStatusRequest.Stop)
                }
                onResume={() =>
                  setRuntimeStatusRequest(RuntimeStatusRequest.Start)
                }
              />
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
                      await setRuntimeStatusRequest(runtimeStatusReq);
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
                      await setRuntimeStatusRequest(
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
              <CustomizePanel
                {...{
                  analysisConfig,
                  attachedPdExists,
                  creatorFreeCreditsRemaining,
                  currentRuntime,
                  environmentChanged,
                  existingAnalysisConfig,
                  gcePersistentDisk,
                  getErrorMessageContent,
                  getWarningMessageContent,
                  onClose,
                  profile,
                  requestAnalysisConfig,
                  runtimeCanBeCreated,
                  runtimeCanBeUpdated,
                  runtimeExists,
                  setAnalysisConfig,
                  setPanelContent,
                  setRuntimeStatusRequest,
                  status,
                  updateMessaging,
                  validMainMachineTypes,
                }}
                allowDataproc={
                  findCdrVersion(cdrVersionId, cdrVersionTiersResponse)
                    ?.hasWgsData
                }
                workspaceData={workspace}
              />
            ),
          ],
          [
            PanelContent.ConfirmUpdate,
            () => (
              <ConfirmUpdatePanel
                {...{
                  existingAnalysisConfig,
                }}
                newAnalysisConfig={analysisConfig}
                onCancel={() => {
                  setPanelContent(PanelContent.Customize);
                }}
              >
                <UpdateButton
                  {...{
                    analysisConfig,
                    requestAnalysisConfig,
                    runtimeCanBeCreated,
                    onClose,
                    runtimeCanBeUpdated,
                    updateMessaging,
                  }}
                />
              </ConfirmUpdatePanel>
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
