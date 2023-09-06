import * as React from 'react';

import {
  AppType,
  CreateAppRequest,
  Disk,
  PersistentDiskRequest,
  UserAppEnvironment,
} from 'generated/fetch';

import {
  createAppRequestToAnalysisConfig,
  defaultCromwellConfig,
  defaultRStudioConfig,
} from 'app/components/apps-panel/utils';
import { DeletePersistentDiskButton } from 'app/components/delete-persistent-disk-button';
import { EnvironmentInformedActionPanel } from 'app/components/environment-informed-action-panel';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { switchCase } from 'app/utils';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { ProfileStore } from 'app/utils/stores';
import {
  appTypeToString,
  unattachedDiskExists,
} from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CreateGKEAppButton } from './create-gke-app-button';
import { DisabledCloudComputeProfile } from './disabled-cloud-compute-profile';

export interface CreateGKEAppPanelProps {
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  app: UserAppEnvironment | undefined;
  disk: Disk | undefined;
  onClickDeleteUnattachedPersistentDisk: () => void;
}

export type CreateGKEAppPanelPropsWithAppType = {
  appType: AppType;
} & CreateGKEAppPanelProps;

type Props = {
  introTextOverride?: string;
  CostNote?: React.FunctionComponent;
  SupportNote?: React.FunctionComponent;
  CreateAppText?: React.FunctionComponent;
} & CreateGKEAppPanelPropsWithAppType;

export const CreateGKEAppPanel = ({
  appType,
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  app,
  disk,
  onClickDeleteUnattachedPersistentDisk,
  introTextOverride,
  CostNote = () => null,
  SupportNote = () => null,
  CreateAppText = () => null,
}: Props) => {
  const { profile } = profileState;

  const onDismiss = () => {
    onClose();
    setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
  };

  const defaultIntroText =
    'Your analysis environment consists of an application and compute resources. ' +
    'Your cloud environment is unique to this workspace and not shared with other users.';

  const defaultConfig = switchCase(
    appType,
    [AppType.CROMWELL, () => defaultCromwellConfig],
    [AppType.RSTUDIO, () => defaultRStudioConfig]
  );

  const persistentDiskRequest: PersistentDiskRequest =
    disk ?? defaultConfig.persistentDiskRequest;
  const createAppRequest: CreateAppRequest = {
    ...defaultConfig,
    persistentDiskRequest,
  };
  const analysisConfig = createAppRequestToAnalysisConfig(createAppRequest);
  const { machine } = analysisConfig;

  return (
    <FlexColumn
      id={`${appTypeToString[appType]}-configuration-panel`}
      style={{ height: '100%', rowGap: '1rem' }}
    >
      <div>{introTextOverride ?? defaultIntroText}</div>
      <div style={{ ...styles.controlSection }}>
        <EnvironmentInformedActionPanel
          {...{
            appType,
            creatorFreeCreditsRemaining,
            profile,
            workspace,
            analysisConfig,
          }}
          status={app?.status}
          onPause={Promise.resolve()}
          onResume={Promise.resolve()}
        />
        <CostNote />
      </div>
      <div style={{ ...styles.controlSection }}>
        <DisabledCloudComputeProfile
          {...{ appType, machine, persistentDiskRequest }}
        />
      </div>
      <SupportNote />
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'flex-end',
          gap: '2rem',
        }}
      >
        {unattachedDiskExists(app, disk) && (
          <DeletePersistentDiskButton
            onClick={onClickDeleteUnattachedPersistentDisk}
            style={{ flexShrink: 0 }}
          />
        )}
        <CreateAppText />
        <CreateGKEAppButton
          {...{ createAppRequest, onDismiss }}
          existingApp={app}
          workspaceNamespace={workspace.namespace}
        />
      </FlexRow>
    </FlexColumn>
  );
};
