import * as React from 'react';
import { useEffect } from 'react';
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
import { MachineSelector } from 'app/components/common-env-conf-panels/machine-selector';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ErrorMessage, WarningMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import {
  AnalysisConfig,
  maybeWithExistingDiskName,
  withAnalysisConfigDefaults,
} from 'app/utils/analysis-config';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  Machine,
  validLeoDataprocMasterMachineTypes,
  validLeoGceMachineTypes,
} from 'app/utils/machines';
import {
  canUpdateRuntime,
  DiskConfig,
  RuntimeStatusRequest,
  UpdateMessaging,
} from 'app/utils/runtime-utils';
import { serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CustomizePanelFooter } from './customize-panel-footer';
import { DataProcConfigSelector } from './dataproc-config-selector';
import { DiskSelector } from './disk-selector';
import { GpuConfigSelector } from './gpu-config-selector';
import { PresetSelector } from './preset-selector';
import { PanelContent } from './utils';

export interface CustomizePanelProps {
  allowDataproc: boolean;
  analysisConfig: AnalysisConfig;
  attachedPdExists: boolean;
  creatorFreeCreditsRemaining: number;
  currentRuntime: Runtime;
  environmentChanged: boolean;
  errorMessageContent: JSX.Element[];
  existingAnalysisConfig: AnalysisConfig;
  gcePersistentDisk: Disk;
  onClose: () => void;
  profile: Profile;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  runtimeCannotBeCreatedExplanation?: string;
  runtimeCanBeUpdated: boolean;
  runtimeCannotBeUpdatedExplanation?: string;
  runtimeExists: boolean;
  runtimeStatus: RuntimeStatus;
  setAnalysisConfig: (config: AnalysisConfig) => void;
  setPanelContent: (pc: PanelContent) => void;
  setRuntimeStatusRequest: (rs: RuntimeStatusRequest) => Promise<void>;
  updateMessaging?: UpdateMessaging;
  warningMessageContent: JSX.Element[];
  workspaceData: WorkspaceData;
}
export const CustomizePanel = ({
  allowDataproc,
  analysisConfig,
  attachedPdExists,
  creatorFreeCreditsRemaining,
  currentRuntime,
  environmentChanged,
  errorMessageContent,
  existingAnalysisConfig,
  gcePersistentDisk,
  onClose,
  profile,
  requestAnalysisConfig,
  runtimeCanBeCreated,
  runtimeCannotBeCreatedExplanation,
  runtimeCanBeUpdated,
  runtimeCannotBeUpdatedExplanation,
  runtimeExists,
  runtimeStatus,
  setAnalysisConfig,
  setPanelContent,
  setRuntimeStatusRequest,
  updateMessaging,
  warningMessageContent,
  workspaceData,
}: CustomizePanelProps) => {
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

  const disableControls = runtimeExists && !canUpdateRuntime(runtimeStatus);
  /* If the user has a persistent disk attached (gcePersistentDisk), they cannot change the zone.
      Runtime existence is not relevant, because whenever there is a runtime, there is a persistent disk
      except in the case of Dataproce which cannot change zone anyways.
      This variable is here to make some of the conditions below more readable.*/
  const canChangeZone = !gcePersistentDisk;
  const { gceVmZones } = serverConfigStore.get().config;

  return (
    <div style={{ marginBottom: '10px' }}>
      <div style={styles.controlSection}>
        <EnvironmentInformedActionPanel
          {...{
            creatorFreeCreditsRemaining,
            profile,
            analysisConfig,
            environmentChanged,
          }}
          status={runtimeStatus}
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
          }}
          disabled={disableControls}
        />
        {/* Runtime customization: change detailed machine configuration options. */}
        <h3 style={{ ...styles.sectionHeader, ...styles.bold }}>
          Cloud compute profile
        </h3>
        <div style={{ display: 'flex', gap: '1.5rem' }}>
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
            gap: '1rem',
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
                    runtimeStatus !== RuntimeStatus.RUNNING
                      ? 'Start your Dataproc cluster to access the Spark console'
                      : null
                  }
                >
                  <LinkButton
                    data-test-id='manage-spark-console'
                    disabled={
                      runtimeStatus !== RuntimeStatus.RUNNING ||
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

          {analysisConfig.computeType === ComputeType.Standard &&
            gceVmZones &&
            gceVmZones.length > 1 && (
              <FlexColumn>
                <label style={styles.label} htmlFor='runtime-compute'>
                  Zone
                </label>
                <TooltipTrigger
                  content={`Cannot change the zone when an environment or persistent disk exists. 
                If you would like to change zones, please make sure that you first delete both your environment and your persistent disk`}
                  disabled={canChangeZone}
                >
                  <div>
                    <Dropdown
                      id='runtime-zone'
                      appendTo='self'
                      disabled={disableControls || !canChangeZone}
                      style={{ width: '15rem' }}
                      options={gceVmZones?.sort()}
                      value={analysisConfig.zone}
                      onChange={({ value: zone }) => {
                        setAnalysisConfig({ ...analysisConfig, zone });
                      }}
                    />
                  </div>
                </TooltipTrigger>
              </FlexColumn>
            )}
        </FlexRow>
        {analysisConfig.computeType === ComputeType.Dataproc && (
          <DataProcConfigSelector
            disabled={disableControls}
            runtimeStatus={runtimeStatus}
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
        onChange={(change: Partial<DiskConfig>) => {
          const diskConfig = maybeWithExistingDiskName(
            {
              ...analysisConfig.diskConfig,
              ...change,
            },
            gcePersistentDisk
          );

          setAnalysisConfig({
            ...analysisConfig,
            diskConfig,
            detachedDisk: diskConfig.detachable ? null : gcePersistentDisk,
          });
        }}
        disabled={disableControls}
        computeType={analysisConfig.computeType}
      />
      {runtimeExists && updateMessaging?.warn && (
        <WarningMessage iconSize={30} iconPosition={'center'}>
          <div>{updateMessaging.warn}</div>
        </WarningMessage>
      )}
      {errorMessageContent.length > 0 && (
        <ErrorMessage
          iconSize={16}
          iconPosition={'top'}
          data-test-id={'runtime-error-messages'}
        >
          {errorMessageContent}
        </ErrorMessage>
      )}
      {warningMessageContent.length > 0 && (
        <WarningMessage
          iconSize={16}
          iconPosition={'top'}
          data-test-id={'runtime-warning-messages'}
        >
          {warningMessageContent}
        </WarningMessage>
      )}
      <CustomizePanelFooter
        {...{
          analysisConfig,
          currentRuntime,
          existingAnalysisConfig,
          onClose,
          requestAnalysisConfig,
          runtimeCanBeCreated,
          runtimeCannotBeCreatedExplanation,
          runtimeCanBeUpdated,
          runtimeCannotBeUpdatedExplanation,
          runtimeExists,
          setPanelContent,
          gcePersistentDisk,
        }}
        unattachedPdExists={!!gcePersistentDisk && !attachedPdExists}
      />
    </div>
  );
};
