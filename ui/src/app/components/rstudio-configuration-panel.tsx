import * as React from 'react';

import { AppType, UserAppEnvironment } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { CreateGKEAppButton } from 'app/components/gke-app-configuration-panels/create-gke-app-button';
import { DisabledCloudComputeProfile } from 'app/components/gke-app-configuration-panels/disabled-cloud-compute-profile';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { findMachineByName, Machine } from 'app/utils/machines';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { AnalysisConfig } from 'app/utils/runtime-utils';
import { ProfileStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { defaultRStudioConfig, findApp, UIAppType } from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';

const DEFAULT_MACHINE_TYPE: Machine = findMachineByName(
  defaultRStudioConfig.kubernetesRuntimeConfig.machineType
);

const { cpu, memory } = DEFAULT_MACHINE_TYPE;

const analysisConfig: Partial<AnalysisConfig> = {
  machine: findMachineByName(
    defaultRStudioConfig.kubernetesRuntimeConfig.machineType
  ),
  diskConfig: {
    size: defaultRStudioConfig.persistentDiskRequest.size,
    detachable: true,
    detachableType: defaultRStudioConfig.persistentDiskRequest.diskType,
    existingDiskName: null,
  },
  numNodes: defaultRStudioConfig.kubernetesRuntimeConfig.numNodes,
};

export interface RStudioConfigurationPanelProps {
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  gkeAppsInWorkspace: NonNullable<UserAppEnvironment[]>;
}

export const RStudioConfigurationPanel = ({
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  gkeAppsInWorkspace,
}: RStudioConfigurationPanelProps) => {
  const app = findApp(gkeAppsInWorkspace, UIAppType.RSTUDIO);
  const { profile } = profileState;

  const onDismiss = () => {
    onClose();
    setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
  };

  return (
    <FlexColumn
      id='rstudio-configuration-panel'
      style={{ height: '100%', rowGap: '1rem' }}
    >
      <div>
        Your analysis environment consists of an application and compute
        resources. Your cloud environment is unique to this workspace and not
        shared with other users.
      </div>
      <div style={{ ...styles.controlSection }}>
        <EnvironmentInformedActionPanel
          {...{
            creatorFreeCreditsRemaining,
            profile,
            workspace,
            analysisConfig,
          }}
          status={app?.status}
          onPause={Promise.resolve()}
          onResume={Promise.resolve()}
          appType={AppType.RSTUDIO}
        />
      </div>
      <div style={{ ...styles.controlSection }}>
        <DisabledCloudComputeProfile
          cpu={cpu}
          memory={memory}
          persistentDiskRequestSize={
            defaultRStudioConfig.persistentDiskRequest.size
          }
          appType={AppType.RSTUDIO}
        />
      </div>
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'flex-end',
        }}
      >
        <CreateGKEAppButton
          createAppRequest={defaultRStudioConfig}
          existingApp={app}
          workspaceNamespace={workspace.namespace}
          onDismiss={onDismiss}
        />
      </FlexRow>
    </FlexColumn>
  );
};
