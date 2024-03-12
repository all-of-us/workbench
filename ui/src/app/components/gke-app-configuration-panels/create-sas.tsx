import * as React from 'react';

import { AppType } from 'generated/fetch';

import { SupportNoteSection } from 'app/components/common-env-conf-panels/support-note-section';
import { supportUrls } from 'app/utils/zendesk';

import { CommonCreateGkeAppProps, CreateGkeApp } from './create-gke-app';

const SASSupportArticles = [
  {
    text: 'How to run SAS in the Researcher Workbench',
    link: supportUrls.sasHowToRun,
  },
  {
    text: 'Exploring All of Us data using SAS',
    link: supportUrls.sasExplore,
  },
];

export const CreateSAS = (props: CommonCreateGkeAppProps) => (
  <CreateGkeApp
    {...props}
    SupportNote={() => SupportNoteSection(AppType.SAS, SASSupportArticles)}
    appType={AppType.SAS}
  />
);
