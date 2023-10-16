import * as React from 'react';

import { AppType } from 'generated/fetch';

import { CommonCreateGkeAppProps } from './create-gke-app';
import { CreateAwsApp } from './create-aws-app';
import { Button } from 'primereact/button';

export const CreateSagemaker = (props: CommonCreateGkeAppProps) => (
  <CreateAwsApp {...props} appType={AppType.SAGEMAKER} />
);