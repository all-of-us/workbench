import { AppType } from 'generated/fetch';

import { CreateAwsApp } from './create-aws-app';
import { CommonCreateGkeAppProps } from './create-gke-app';

export const CreateSagemaker = (props: CommonCreateGkeAppProps) => (
  <CreateAwsApp {...props} appType={AppType.SAGEMAKER} />
);
