import * as React from 'react';

import { AppType } from 'generated/fetch';

import { WarningMessage } from 'app/components/messages';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import {
  CROMWELL_INFORMATION_LINK,
  CROMWELL_INTRO_LINK,
  WORKFLOW_AND_WDL_LINK,
} from 'app/utils/aou_external_links';

import { CommonCreateGkeAppProps, CreateGkeApp } from './create-gke-app';

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

const introText =
  'A cloud environment consists of an application configuration, cloud compute, and persistent disk(s). ' +
  'Cromwell is a workflow execution engine. ' +
  'You will need to create a Jupyter terminal environment in order to interact with Cromwell.';

const CostNote = () => (
  <WarningMessage>
    This cost is only for running the Cromwell Engine. There will be additional
    cost for interactions with the workflow.
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
);

const SupportNote = () => (
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
);

const CreateAppText = () => (
  <div style={{ flexGrow: 1 }}>
    <div style={{ fontWeight: 'bold' }}>Next Steps:</div>
    <div>
      You can interact with the workflow by using the Cromshell in Jupyter
      Terminal or Jupyter notebook
    </div>
  </div>
);

export const CreateCromwell = (props: CommonCreateGkeAppProps) => (
  <CreateGkeApp
    {...{
      ...props,
      introText,
      CostNote,
      SupportNote,
      CreateAppText,
    }}
    appType={AppType.CROMWELL}
  />
);
