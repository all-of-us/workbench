import * as React from 'react';

import { AppType, UserAppEnvironment } from 'generated/fetch';

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
import { findMachineByName, Machine } from 'app/utils/machines';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { AnalysisConfig } from 'app/utils/runtime-utils';
import { ProfileStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { defaultCromwellConfig, findApp, UIAppType } from './apps-panel/utils';
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

const DEFAULT_MACHINE_TYPE: Machine = findMachineByName(
  defaultCromwellConfig.kubernetesRuntimeConfig.machineType
);

const { cpu, memory } = DEFAULT_MACHINE_TYPE;

const analysisConfig: Partial<AnalysisConfig> = {
  machine: DEFAULT_MACHINE_TYPE,
  diskConfig: {
    size: defaultCromwellConfig.persistentDiskRequest.size,
    detachable: true,
    detachableType: defaultCromwellConfig.persistentDiskRequest.diskType,
    existingDiskName: null,
  },
  numNodes: defaultCromwellConfig.kubernetesRuntimeConfig.numNodes,
};

export interface CromwellConfigurationPanelProps {
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  gkeAppsInWorkspace: NonNullable<UserAppEnvironment[]>;
}

export const CromwellConfigurationPanel = ({
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  gkeAppsInWorkspace,
}: CromwellConfigurationPanelProps) => {
  const app = findApp(gkeAppsInWorkspace, UIAppType.CROMWELL);
  const { profile } = profileState;

  const onDismiss = () => {
    onClose();
    setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
  };

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
          cpu={cpu}
          memory={memory}
          persistentDiskRequestSize={
            defaultCromwellConfig.persistentDiskRequest.size
          }
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
        }}
      >
        <div style={{ margin: '0rem 1rem 1rem 0rem ' }}>
          <div style={{ fontWeight: 'bold' }}>Next Steps:</div>
          <div>
            You can interact with the workflow by using the Cromshell in Jupyter
            Terminal or Jupyter notebook
          </div>
        </div>
        <CreateGKEAppButton
          createAppRequest={defaultCromwellConfig}
          existingApp={app}
          workspaceNamespace={workspace.namespace}
          onDismiss={onDismiss}
        />
      </FlexRow>
    </FlexColumn>
  );
};
