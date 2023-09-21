import * as React from 'react';

import { AppType } from 'generated/fetch';

import { CommonCreateGkeAppProps, CreateGkeApp } from './create-gke-app';

export const CreateSAS = (props: CommonCreateGkeAppProps) => (
  <CreateGkeApp {...props} appType={AppType.SAS} />
);
