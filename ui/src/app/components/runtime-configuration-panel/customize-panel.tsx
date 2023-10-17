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

import { UIAppType } from 'app/components/apps-panel/utils';
import { LinkButton } from 'app/components/buttons';
import { EnvironmentInformedActionPanel } from 'app/components/common-env-conf-panels/environment-informed-action-panel';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ErrorMessage, WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  Machine,
} from 'app/utils/machines';
import {
  AnalysisConfig,
  isActionable,
  PanelContent,
  RuntimeStatusRequest,
  UpdateMessaging,
  withAnalysisConfigDefaults,
} from 'app/utils/runtime-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CustomizePanelFooter } from './customize-panel-footer';
import { DataProcConfigSelector } from './dataproc-config-selector';
import { DiskSelector } from './disk-selector';
import { GpuConfigSelector } from './gpu-config-selector';
import { MachineSelector } from './machine-selector';
import { PresetSelector } from './preset-selector';

interface Props {
  allowDataproc: boolean;
  analysisConfig: AnalysisConfig;
  attachedPdExists: boolean;
  creatorFreeCreditsRemaining: number;
  currentRuntime: Runtime;
  environmentChanged: boolean;
  existingAnalysisConfig: AnalysisConfig;
  gcePersistentDisk: Disk;
  getErrorMessageContent: () => JSX.Element[];
  getWarningMessageContent: () => JSX.Element[];
  onClose: () => void;
  profile: Profile;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  runtimeCanBeUpdated: boolean;
  runtimeExists: boolean;
  setAnalysisConfig: (config: AnalysisConfig) => void;
  setPanelContent: (pc: PanelContent) => void;
  setRuntimeStatusRequest: (rs: RuntimeStatusRequest) => Promise<void>;
  status: RuntimeStatus;
  updateMessaging: UpdateMessaging;
  validMainMachineTypes: Machine[];
  workspaceData: WorkspaceData;
}
export const CustomizePanel = ({
  allowDataproc,
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
  workspaceData,
}: Props) => {
  const disableControls = runtimeExists && !isActionable(status);

  const unattachedPdExists = !!gcePersistentDisk && !attachedPdExists;
  const unattachedDiskNeedsRecreate =
    unattachedPdExists &&
    analysisConfig.diskConfig.detachable &&
    (gcePersistentDisk.size > analysisConfig.diskConfig.size ||
      gcePersistentDisk.diskType !== analysisConfig.diskConfig.detachableType);

  return (
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
          onPause={() => setRuntimeStatusRequest(RuntimeStatusRequest.Stop)}
          onResume={() => setRuntimeStatusRequest(RuntimeStatusRequest.Start)}
          appType={UIAppType.JUPYTER}
        />
        {currentRuntime?.errors && currentRuntime.errors.length > 0 && (
          <ErrorMessage iconPosition={'top'} iconSize={16}>
            <div>
              An error was encountered with your cloud environment. Please
              re-attempt creation of the environment and contact support if the
              error persists.
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
                value={analysisConfig.computeType || ComputeType.Standard}
                onChange={({ value: computeType }) =>
                  // When the compute type changes, we need to normalize the config and potentially restore defaults.
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
                    onClick={() => setPanelContent(PanelContent.SparkConsole)}
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
            dataprocExists={
              runtimeExists && existingAnalysisConfig.dataprocConfig !== null
            }
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
              options={Array.from(AutopauseMinuteThresholds.entries()).map(
                (entry) => ({
                  label: entry[1],
                  value: entry[0],
                })
              )}
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
            detachedDisk: diskConfig.detachable ? null : gcePersistentDisk,
          })
        }
        disabled={disableControls}
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
      <CustomizePanelFooter
        {...{
          analysisConfig,
          currentRuntime,
          disableControls,
          existingAnalysisConfig,
          onClose,
          requestAnalysisConfig,
          runtimeCanBeCreated,
          runtimeCanBeUpdated,
          runtimeExists,
          setPanelContent,
          unattachedDiskNeedsRecreate,
          unattachedPdExists,
        }}
      />
    </div>
  );
};
