import * as React from 'react';
import { useEffect } from 'react';

import { AppType, UserAppEnvironment } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { ApiErrorResponse, fetchWithErrorModal } from 'app/utils/errors';
import { findMachineByName, Machine } from 'app/utils/machines';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { AnalysisConfig } from 'app/utils/runtime-utils';
import { ProfileStore } from 'app/utils/stores';
import { createUserApp } from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import {
  canCreateApp,
  defaultRStudioConfig,
  findApp,
  UIAppType,
} from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';
import { TooltipTrigger } from './popups';

const { useState } = React;

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
}

export const RStudioConfigurationPanel = ({
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
}: RStudioConfigurationPanelProps) => {
  const [gkeAppsInWorkspace, setGkeAppsInWorkspace] =
    useState<UserAppEnvironment[]>();
  const [creatingRStudioApp, setCreatingRStudioApp] = useState(false);

  const app = findApp(gkeAppsInWorkspace, UIAppType.RSTUDIO);
  const loadingApps = gkeAppsInWorkspace === undefined;

  const { profile } = profileState;

  useEffect(() => {
    appsApi()
      .listAppsInWorkspace(workspace.namespace)
      .then(setGkeAppsInWorkspace);
  }, []);

  const createEnabled =
    !loadingApps && !creatingRStudioApp && canCreateApp(app);

  const onDismiss = () => {
    onClose();
    setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
  };

  const onCreate = () => {
    setCreatingRStudioApp(true);
    fetchWithErrorModal(
      () => createUserApp(workspace.namespace, defaultRStudioConfig),
      {
        customErrorResponseFormatter: (error: ApiErrorResponse) =>
          error?.originalResponse?.status === 409 && {
            title: 'Error Creating RStudio Environment',
            message:
              'Please wait a few minutes and try to create your RStudio Environment again.',
            onDismiss,
          },
      }
    ).then(() => onDismiss());
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
        <FlexRow style={{ alignItems: 'center' }}>
          <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
            Cloud compute profile
          </div>
          <TooltipTrigger
            content='The cloud compute profile for RStudio beta is non-configurable.'
            side={'right'}
          >
            <div style={styles.disabledCloudProfile}>
              {`${cpu} CPUS, ${memory}GB RAM, ${defaultRStudioConfig.persistentDiskRequest.size}GB disk`}
            </div>
          </TooltipTrigger>
        </FlexRow>
      </div>
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'flex-end',
        }}
      >
        <Button
          id='rstudio-cloud-environment-create-button'
          aria-label='rstudio cloud environment create button'
          onClick={onCreate}
          disabled={!createEnabled}
        >
          Start
        </Button>
      </FlexRow>
    </FlexColumn>
  );
};
