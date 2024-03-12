import * as React from 'react';

import { AppType } from 'generated/fetch';

import { SupportNoteSection } from 'app/components/common-env-conf-panels/support-note-section';
import { SAS_EXPLORING, SAS_INTRO_LINK } from 'app/utils/aou_external_links';

import { CommonCreateGkeAppProps, CreateGkeApp } from './create-gke-app';

const SASSupportArticles = [
  {
    text: 'How to run SAS in the Researcher Workbench',
    link: SAS_INTRO_LINK,
  },
  {
    text: 'Exploring All of Us data using SAS',
    link: SAS_EXPLORING,
  },
];

export const CreateSAS = (props: CommonCreateGkeAppProps) => (
  <CreateGkeApp
    {...props}
    SupportNote={() => SupportNoteSection(AppType.SAS, SASSupportArticles)}
    appType={AppType.SAS}
  />
);
