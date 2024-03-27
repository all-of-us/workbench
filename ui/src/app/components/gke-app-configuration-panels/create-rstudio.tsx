import * as React from 'react';

import { AppType } from 'generated/fetch';

import { SupportNoteSection } from 'app/components/common-env-conf-panels/support-note-section';
import { InfoMessage } from 'app/components/messages';
import { supportUrls } from 'app/utils/zendesk';

import { CommonCreateGkeAppProps, CreateGkeApp } from './create-gke-app';

const rStudioSupportArticles = [
  {
    text: 'How to run RStudio in All of Us Researcher Workbench',
    link: supportUrls.rStudioHowToRun,
  },
  {
    text: 'How to use Dataset Builder with RStudio',
    link: supportUrls.rStudioHowToDataset,
  },
];

const SupportNote = () => (
  <>
    <InfoMessage>
      <h4 style={{ marginTop: 0, fontSize: '1rem' }}>
        How to create RStudio artifacts:
      </h4>
      <p style={{ marginTop: '0.5rem' }}>
        You can create R and RMD files within RStudio’s menu bar. Saved files
        will appear in the analysis tab and be stored in the workspace bucket.
        Access your files in RStudio through the Output pane.
      </p>
    </InfoMessage>
    <div>{SupportNoteSection('RStudio', rStudioSupportArticles)}</div>
  </>
);

export const CreateRStudio = (props: CommonCreateGkeAppProps) => (
  <CreateGkeApp
    {...{
      ...props,
      SupportNote,
    }}
    appType={AppType.RSTUDIO}
  />
);
