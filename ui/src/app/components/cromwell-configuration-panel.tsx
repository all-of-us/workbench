import * as React from 'react';
import { useEffect } from 'react';
import * as fp from 'lodash/fp';

import { AppType } from 'generated/fetch';

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
import { findMachineByName } from 'app/utils/machines';
import { runtimeStore, useStore } from 'app/utils/stores';

import { defaultCromwellConfig } from './apps-panel/utils';
import { CostPredictor } from './cost-predictor';

const { useState } = React;

const cromwellSupportArticles = [
  { text: 'How to run Cromwell in All of Us workbench?', link: '#' },
  { text: 'Cromwell documentation', link: '#' },
  { text: 'Workflow and WDL', link: '#' },
  { text: 'Running and Autopause', link: '#' },
  { text: 'Storage options', link: '#' },
];

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
    const { profile } = profileState;

    return (
      <FlexColumn style={{ height: '100%' }}>
        <div
          data-test-id='runtime-create-panel'
          style={{ ...styles.controlSection, marginTop: '1rem' }}
        >
          <CostPredictor
            {...{
              creatorFreeCreditsRemaining,
              profile,
              workspace,
              analysisConfig,
            }}
            status={null}
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
              4 CPUS, 15GB RAM, 120GB disk
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
            onClick={() => {
              appsApi().createApp(workspace.namespace, defaultCromwellConfig);
              onClose();
            }}
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
  const { runtimeLoaded } = useStore(runtimeStore);

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
  if (!runtimeLoaded || !analysisConfigLoaded) {
    return <Spinner style={{ width: '100%', marginTop: '7.5rem' }} />;
  }

  return (
    <FlexColumn style={{ height: '100%' }}>
      <div>
        A cloud environment consists of application configuration, cloud compute
        and persistent disk(s). This is the server version of Cromwell only. You
        will need to create a Jupyter terminal environment in order to interact
        with the workflow.
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
