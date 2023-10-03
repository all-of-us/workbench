import * as React from 'react';

import { AppType } from 'generated/fetch';

import { InfoMessage } from 'app/components/messages';

import { CommonCreateGkeAppProps, CreateGkeApp } from './create-gke-app';

const SupportNote = () => (
  <InfoMessage>
    <h4 style={{ marginTop: 0, fontSize: '1rem' }}>
      How to create Sagemaker artifacts:
    </h4>
  </InfoMessage>
);

export const CreateSagemaker = (props: CommonCreateGkeAppProps) => (
  <CreateGkeApp
    {...{
      ...props,
      SupportNote,
    }}
    appType={AppType.SAGEMAKER}
  />
);
