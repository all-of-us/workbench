import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import {
  DataprocConfig,
  Disk,
  GpuConfig,
  Profile,
  Runtime,
  RuntimeStatus,
} from 'generated/fetch';

import { cond, switchCase } from '@terra-ui-packages/core-utils';
import { disksApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  Machine,
} from 'app/utils/machines';
import {
  AnalysisConfig,
  PanelContent,
  RuntimeStatusRequest,
  UpdateMessaging,
  withAnalysisConfigDefaults,
} from 'app/utils/runtime-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { UIAppType } from './apps-panel/utils';
import { LinkButton } from './buttons';
import { ConfirmDelete } from './common-env-conf-panels/confirm-delete';
import { ConfirmDeleteEnvironmentWithPD } from './common-env-conf-panels/confirm-delete-environment-with-pd';
import { ConfirmDeleteUnattachedPD } from './common-env-conf-panels/confirm-delete-unattached-pd';
import { DeletePersistentDiskButton } from './common-env-conf-panels/delete-persistent-disk-button';
import { DisabledPanel } from './common-env-conf-panels/disabled-panel';
import { EnvironmentInformedActionPanel } from './common-env-conf-panels/environment-informed-action-panel';
import { styles } from './common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from './flex';
import { ErrorMessage, WarningMessage } from './messages';
import { TooltipTrigger } from './popups';
import { ConfirmUpdatePanel } from './runtime-configuration-panel/confirm-update-panel';
import { DataProcConfigSelector } from './runtime-configuration-panel/dataproc-config-selector';
import { DiskSelector } from './runtime-configuration-panel/disk-selector';
import { GpuConfigSelector } from './runtime-configuration-panel/gpu-config-selector';
import { MachineSelector } from './runtime-configuration-panel/machine-selector';
import { OfferDeleteDiskWithUpdate } from './runtime-configuration-panel/offer-delete-disk-with-update';
import { PresetSelector } from './runtime-configuration-panel/preset-selector';
import { SparkConsolePanel } from './runtime-configuration-panel/spark-console-panel';
import { CreatePanel } from './runtime-confiuration-panel/create-panel';

interface Props {
  allowDataproc: boolean;
  analysisConfig: AnalysisConfig;
  attachedPdExists: boolean;
  creatorFreeCreditsRemaining: number;
  currentRuntime: Runtime;
  dataprocExists: boolean;
  disableControls: boolean;
  disableDetachableReason: string;
  environmentChanged: boolean;
  existingAnalysisConfig: AnalysisConfig;
  gcePersistentDisk: Disk;
  getErrorMessageContent: () => JSX.Element[];
  getWarningMessageContent: () => JSX.Element[];
  onClose?: () => void;
  panelContent: PanelContent;
  profile: Profile;
  renderCreateButton: () => JSX.Element;
  renderNextUpdateButton: () => JSX.Element;
  renderNextWithDiskDeleteButton: () => JSX.Element;
  renderTryAgainButton: () => JSX.Element;
  renderUpdateButton: () => JSX.Element;
  requestAnalysisConfig: (config: AnalysisConfig) => void;
  runtimeExists: boolean;
  setAnalysisConfig: (config: AnalysisConfig) => void;
  setPanelContent: (pc: PanelContent) => void;
  setRuntimeStatusRequest: (rs: RuntimeStatusRequest) => Promise<void>;
  status: RuntimeStatus;
  unattachedDiskNeedsRecreate: boolean;
  unattachedPdExists: boolean;
  updateMessaging: UpdateMessaging;
  usingDataproc: boolean;
  validMainMachineTypes: Machine[];
  workspaceData: WorkspaceData;
}
export const RuntimeConfigurationPanelRenderer = ({
  allowDataproc,
  analysisConfig,
  attachedPdExists,
  creatorFreeCreditsRemaining,
  currentRuntime,
  dataprocExists,
  disableControls,
  disableDetachableReason,
  environmentChanged,
  existingAnalysisConfig,
  gcePersistentDisk,
  getErrorMessageContent,
  getWarningMessageContent,
  onClose,
  panelContent,
  profile,
  renderCreateButton,
  renderNextUpdateButton,
  renderNextWithDiskDeleteButton,
  renderTryAgainButton,
  renderUpdateButton,
  requestAnalysisConfig,
  runtimeExists,
  setAnalysisConfig,
  setPanelContent,
  setRuntimeStatusRequest,
  status,
  unattachedDiskNeedsRecreate,
  unattachedPdExists,
  updateMessaging,
  usingDataproc,
  validMainMachineTypes,
  workspaceData,
}: Props) => {
  return (
    <div id='runtime-panel'>
      {cond<React.ReactNode>(
        [
          [PanelContent.Create, PanelContent.Customize].includes(panelContent),
          () => (
            <div style={{ marginBottom: '1.5rem' }}>
              Your analysis environment consists of an application and compute
              resources. Your cloud environment is unique to this workspace and
              not shared with other users.
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
            <>
              <CreatePanel
                {...{
                  profile,
                  setPanelContent,
                  analysisConfig,
                  creatorFreeCreditsRemaining,
                  status,
                  setRuntimeStatusRequest,
                }}
                workspace={workspaceData}
              />
              <FlexRow
                style={{ justifyContent: 'flex-end', marginTop: '1.5rem' }}
              >
                {renderCreateButton()}
              </FlexRow>
            </>
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
                  workspaceData.namespace,
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
                  workspaceData.namespace,
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
                    analysisConfig,
                    status,
                    environmentChanged,
                  }}
                  workspace={workspaceData}
                  onPause={() =>
                    setRuntimeStatusRequest(RuntimeStatusRequest.Stop)
                  }
                  onResume={() =>
                    setRuntimeStatusRequest(RuntimeStatusRequest.Start)
                  }
                  appType={UIAppType.JUPYTER}
                />
                {currentRuntime?.errors && currentRuntime.errors.length > 0 && (
                  <ErrorMessage iconPosition={'top'} iconSize={16}>
                    <div>
                      An error was encountered with your cloud environment.
                      Please re-attempt creation of the environment and contact
                      support if the error persists.
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
                      {analysisConfig.computeType === ComputeType.Dataproc && (
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
                    onClick={() => setPanelContent(PanelContent.DeleteRuntime)}
                  >
                    Delete Environment
                  </LinkButton>
                  {cond<React.ReactNode>(
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
          () => <SparkConsolePanel {...workspaceData} />,
        ]
      )}
    </div>
  );
};
