import * as React from 'react';

import {
  AppType,
  CreateAppRequest,
  Disk,
  UserAppEnvironment,
} from 'generated/fetch';

import { DeletePersistentDiskButton } from 'app/components/delete-persistent-disk-button';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { CreateGKEAppButton } from 'app/components/gke-app-configuration-panels/create-gke-app-button';
import { DisabledCloudComputeProfile } from 'app/components/gke-app-configuration-panels/disabled-cloud-compute-profile';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { ProfileStore } from 'app/utils/stores';
import { unattachedDiskExists } from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import {
  createAppRequestToAnalysisConfig,
  defaultRStudioConfig,
} from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';

export interface RStudioConfigurationPanelProps {
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  app: UserAppEnvironment | undefined;
  disk: Disk | undefined;
  onClickDeleteUnattachedPersistentDisk: () => void;
}

export const RStudioConfigurationPanel = ({
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  app,
  disk,
  onClickDeleteUnattachedPersistentDisk,
}: RStudioConfigurationPanelProps) => {
  const { profile } = profileState;

  const onDismiss = () => {
    onClose();
    setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
  };

  const rstudioConfig: CreateAppRequest = {
    ...defaultRStudioConfig,
    persistentDiskRequest:
      disk !== undefined ? disk : defaultRStudioConfig.persistentDiskRequest,
  };
  const analysisConfig = createAppRequestToAnalysisConfig(rstudioConfig);

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
          cpu={analysisConfig.machine.cpu}
          memory={analysisConfig.machine.memory}
          persistentDiskRequestSize={rstudioConfig.persistentDiskRequest.size}
          appType={AppType.RSTUDIO}
        />
      </div>
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'flex-end',
        }}
      >
        {unattachedDiskExists(app, disk) && (
          <DeletePersistentDiskButton
            onClick={onClickDeleteUnattachedPersistentDisk}
            style={{ flexGrow: 1 }}
          />
        )}
        <CreateGKEAppButton
          createAppRequest={rstudioConfig}
          existingApp={app}
          workspaceNamespace={workspace.namespace}
          onDismiss={onDismiss}
        />
      </FlexRow>
    </FlexColumn>
  );
};
