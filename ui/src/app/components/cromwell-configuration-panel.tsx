import * as React from 'react';
import { useEffect } from 'react';
import * as fp from 'lodash/fp';

import { AppType, UserAppEnvironment } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningMessage } from 'app/components/messages';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { appsApi } from 'app/services/swagger-fetch-clients';
import {
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import {
  CROMWELL_INFORMATION_LINK,
  CROMWELL_INTRO_LINK,
  WORKFLOW_AND_WDL_LINK,
} from 'app/utils/aou_external_links';
import { ApiErrorResponse, fetchWithErrorModal } from 'app/utils/errors';
import {
  DEFAULT_MACHINE_NAME,
  findMachineByName,
  Machine,
} from 'app/utils/machines';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { createUserApp } from 'app/utils/user-apps-utils';

import {
  canCreateApp,
  defaultCromwellConfig,
  findApp,
  UIAppType,
} from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';
import { TooltipTrigger } from './popups';

const { useState } = React;

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
const DEFAULT_MACHINE_TYPE: Machine = findMachineByName(DEFAULT_MACHINE_NAME);

const { cpu, memory } = DEFAULT_MACHINE_TYPE;

const PanelMain = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withUserProfile()
)(
  ({
    analysisConfig,
    workspace,
    profileState,
    creatorFreeCreditsRemaining,
    onClose,
  }) => {
    // all apps besides Jupyter
    const [userApps, setUserApps] = useState<UserAppEnvironment[]>();
    const [creatingCromwellApp, setCreatingCromwellApp] = useState(false);

    const app = findApp(userApps, UIAppType.CROMWELL);
    const loadingApps = userApps === undefined;

    useEffect(() => {
      appsApi().listAppsInWorkspace(workspace.namespace).then(setUserApps);
    }, []);

    const onDismiss = () => {
      onClose();
      setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
    };

    const onCreate = () => {
      if (!creatingCromwellApp) {
        setCreatingCromwellApp(true);
        fetchWithErrorModal(
          () => createUserApp(workspace.namespace, defaultCromwellConfig),
          {
            customErrorResponseFormatter: (error: ApiErrorResponse) =>
              error?.originalResponse?.status === 409 && {
                title: 'Error Creating Cromwell Environment',
                message:
                  'Please wait a few minutes and try to create your Cromwell Environment again.',
                onDismiss,
              },
          }
        ).then(() => onDismiss());
      }
    };

    const { profile } = profileState;

    const createEnabled =
      !loadingApps && !creatingCromwellApp && canCreateApp(app);

    return (
      <FlexColumn style={{ height: '100%' }}>
        <div
          data-test-id='cromwell-create-panel'
          style={{ ...styles.controlSection, marginTop: '1rem' }}
        >
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
        <div style={{ ...styles.controlSection, marginTop: '1rem' }}>
          <FlexRow style={{ alignItems: 'center' }}>
            <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
              Cloud compute profile
            </div>
            <TooltipTrigger
              content='The cloud compute profile for Cromwell beta is non-configurable.'
              side={'right'}
            >
              <div style={styles.disabledCloudProfile}>
                {`${cpu} CPUS, ${memory}GB RAM, ${defaultCromwellConfig.persistentDiskRequest.size}GB disk`}
              </div>
            </TooltipTrigger>
          </FlexRow>
        </div>
        <div style={{ ...styles.controlSection, marginTop: '1rem' }}>
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
            marginTop: '1rem',
            alignItems: 'center',
          }}
        >
          <div style={{ margin: '0rem 1rem 1rem 0rem ' }}>
            <div style={{ fontWeight: 'bold' }}>Next Steps:</div>
            <div>
              You can interact with the workflow by using the Cromshell in
              Jupyter Terminal or Jupyter notebook
            </div>
          </div>
          <Button
            id='cromwell-cloud-environment-create-button'
            aria-label='cromwell cloud environment create button'
            onClick={onCreate}
            disabled={!createEnabled}
          >
            Start
          </Button>
        </FlexRow>
      </FlexColumn>
    );
  }
);

export const CromwellConfigurationPanel = ({
  onClose = () => {},
  initialPanelContent = null,
  creatorFreeCreditsRemaining = null,
}) => {
  const analysisConfig = {
    machine: findMachineByName(
      defaultCromwellConfig.kubernetesRuntimeConfig.machineType
    ),
    diskConfig: {
      size: defaultCromwellConfig.persistentDiskRequest.size,
      detachable: true,
      detachableType: defaultCromwellConfig.persistentDiskRequest.diskType,
    },
    numNodes: defaultCromwellConfig.kubernetesRuntimeConfig.numNodes,
  };

  return (
    <FlexColumn id='cromwell-configuration-panel' style={{ height: '100%' }}>
      <div>
        A cloud environment consists of an application configuration, cloud
        compute and persistent disk(s). Cromwell is a workflow execution engine.
        You will need to create a Jupyter terminal environment in order to
        interact with Cromwell.
      </div>
      <PanelMain
        {...{
          analysisConfig,
          onClose,
          initialPanelContent,
          creatorFreeCreditsRemaining,
        }}
      />
    </FlexColumn>
  );
};
