import * as React from 'react';
import { useEffect } from 'react';
import * as fp from 'lodash/fp';

import { AppType, UserAppEnvironment } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningMessage } from 'app/components/messages';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { Spinner } from 'app/components/spinners';
import { appsApi } from 'app/services/swagger-fetch-clients';
import {
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import {
  DEFAULT_MACHINE_NAME,
  findMachineByName,
  Machine,
} from 'app/utils/machines';
import { setSidebarActiveIconStore } from 'app/utils/navigation';

import { defaultCromwellConfig, findApp, UIAppType } from './apps-panel/utils';
import { EnvironmentInformedActionPanel } from './environment-informed-action-panel';

const { useState } = React;

const cromwellSupportArticles = [
  { text: 'How to run Cromwell in All of Us workbench?', link: '#' },
  { text: 'Cromwell documentation', link: '#' },
  { text: 'Workflow and WDL', link: '#' },
  { text: 'Running and Autopause', link: '#' },
  { text: 'Storage options', link: '#' },
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
    const [creationStarted, setCreationStarted] = useState(false);
    const [loading, setLoading] = useState(true);
    const [status, setStatus] = useState(null);

    useEffect(() => {
      appsApi().listAppsInWorkspace(workspace.namespace).then(setUserApps);
    }, []);

    useEffect(() => {
      setStatus(findApp(userApps, UIAppType.CROMWELL)?.status);
    }, [userApps]);

    useEffect(() => {
      if (loading && !!status) {
        setLoading(false);
      }
    }, [loading, status]);

    useEffect(() => {
      if (creationStarted) {
        appsApi().createApp(workspace.namespace, defaultCromwellConfig);
        onClose();
        setTimeout(() => setSidebarActiveIconStore.next('apps'), 3000);
      }
    }, [creationStarted]);

    const { profile } = profileState;

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
            status={status}
            onPause={Promise.resolve()}
            onResume={Promise.resolve()}
            appType={AppType.CROMWELL}
          />
          <WarningMessage>
            This cost is only for running the Cromwell Engine, there will be
            additional cost for interactions with the workflow.
            <a href='#' style={{ marginLeft: '0.25rem' }}>
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
          <div style={{ fontWeight: 'bold' }}>Cromwell version: 76</div>
          <FlexRow style={{ alignItems: 'center' }}>
            <div style={{ fontWeight: 'bold', marginRight: '0.5rem' }}>
              Cloud compute profile
            </div>
            <div
              style={{
                marginRight: '0.5rem',
                backgroundColor: '#ededed',
                color: '#7b828e',
                padding: '1rem 2rem 1rem 0.5rem',
                borderRadius: '0.5rem',
              }}
            >
              {`${cpu} CPUS, ${memory}GB RAM, ${defaultCromwellConfig.persistentDiskRequest.size}GB disk`}
            </div>
            <a href='#' style={{ marginLeft: '0.25rem' }}>
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
          </FlexRow>
        </div>
        <div style={{ ...styles.controlSection, marginTop: '1rem' }}>
          <div style={{ fontWeight: 'bold' }}>Cromwell support articles</div>
          {cromwellSupportArticles.map((article, index) => (
            <a href={article.link} style={{ display: 'block' }}>
              {index + 1}. {article.text}
            </a>
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
            onClick={() => {
              setCreationStarted(true);
            }}
            disabled={loading || !!status || creationStarted}
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
  const [analysisConfig, setAnalysisConfig] = useState({});

  useEffect(
    () =>
      setAnalysisConfig({
        machine: findMachineByName(
          defaultCromwellConfig.kubernetesRuntimeConfig.machineType
        ),
        diskConfig: {
          size: defaultCromwellConfig.persistentDiskRequest.size,
          detachable: true,
          detachableType: defaultCromwellConfig.persistentDiskRequest.diskType,
        },
        numNodes: defaultCromwellConfig.kubernetesRuntimeConfig.numNodes,
      }),
    []
  );

  const analysisConfigLoaded = Object.keys(analysisConfig).length > 0;
  if (!analysisConfigLoaded) {
    return (
      <Spinner
        id='cromwell-configuration-panel-spinner'
        aria-label='spinner showing that cromwell configuration panel is loading'
        style={{ width: '100%', marginTop: '7.5rem' }}
      />
    );
  }

  return (
    <FlexColumn id='cromwell-configuration-panel' style={{ height: '100%' }}>
      <div>
        A cloud environment consists of an application configuration, cloud
        compute and persistent disk(s). This is the server version of Cromwell
        only. You will need to create a Jupyter terminal environment in order to
        interact with the workflow.
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
