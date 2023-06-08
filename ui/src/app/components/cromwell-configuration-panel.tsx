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
import { WarningMessage } from 'app/components/messages';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import {
  CROMWELL_INFORMATION_LINK,
  CROMWELL_INTRO_LINK,
  WORKFLOW_AND_WDL_LINK,
} from 'app/utils/aou_external_links';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { ProfileStore } from 'app/utils/stores';
import { unattachedDiskExists } from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import {
  createAppRequestToAnalysisConfig,
  defaultCromwellConfig,
} from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';

const cromwellSupportArticles = [
  {
    text: 'How to run Cromwell in All of Us workbench?',
    link: CROMWELL_INFORMATION_LINK,
  },
  {
    text: 'Cromwell documentation',
    link: CROMWELL_INTRO_LINK,
  },
  {
    text: 'Workflow and WDL',
    link: WORKFLOW_AND_WDL_LINK,
  },
];

export interface CromwellConfigurationPanelProps {
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  app: UserAppEnvironment | undefined;
  disk: Disk | undefined;
  onClickDeleteUnattachedPersistentDisk: () => void;
}

export const CromwellConfigurationPanel = ({
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  app,
  disk,
  onClickDeleteUnattachedPersistentDisk,
}: CromwellConfigurationPanelProps) => {
  const { profile } = profileState;

  const onDismiss = () => {
    onClose();
    setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
  };

  const cromwellConfig: CreateAppRequest = {
    ...defaultCromwellConfig,
    persistentDiskRequest:
      disk !== undefined ? disk : defaultCromwellConfig.persistentDiskRequest,
  };
  const analysisConfig = createAppRequestToAnalysisConfig(cromwellConfig);

  return (
    <FlexColumn
      id='cromwell-configuration-panel'
      style={{ height: '100%', rowGap: '1rem' }}
    >
      <div>
        A cloud environment consists of an application configuration, cloud
        compute and persistent disk(s). Cromwell is a workflow execution engine.
        You will need to create a Jupyter terminal environment in order to
        interact with Cromwell.
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
          appType={AppType.CROMWELL}
        />
        <WarningMessage>
          This cost is only for running the Cromwell Engine, there will be
          additional cost for interactions with the workflow.
          <a
            style={{ marginLeft: '0.25rem' }}
            href={CROMWELL_INFORMATION_LINK}
            target={'_blank'}
          >
            Learn more{' '}
          </a>
          <i
            className='pi pi-external-link'
            style={{
              marginLeft: '0.25rem',
              fontSize: '0.75rem',
              color: '#6fb4ff',
              cursor: 'pointer',
            }}
          />
        </WarningMessage>
      </div>
      <div style={{ ...styles.controlSection }}>
        <DisabledCloudComputeProfile
          cpu={analysisConfig.machine.cpu}
          memory={analysisConfig.machine.memory}
          persistentDiskRequestSize={cromwellConfig.persistentDiskRequest.size}
          appType={AppType.CROMWELL}
        />
      </div>
      <div style={{ ...styles.controlSection }}>
        <div style={{ fontWeight: 'bold' }}>Cromwell support articles</div>
        {cromwellSupportArticles.map((article, index) => (
          <div key={index} style={{ display: 'block' }}>
            <a href={article.link} target='_blank'>
              {index + 1}. {article.text}
            </a>
          </div>
        ))}
      </div>
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
        <div style={{ flexGrow: 1 }}>
          <div style={{ fontWeight: 'bold' }}>Next Steps:</div>
          <div>
            You can interact with the workflow by using the Cromshell in Jupyter
            Terminal or Jupyter notebook
          </div>
        </div>
        <CreateGKEAppButton
          createAppRequest={cromwellConfig}
          existingApp={app}
          workspaceNamespace={workspace.namespace}
          onDismiss={onDismiss}
        />
      </FlexRow>
    </FlexColumn>
  );
};
