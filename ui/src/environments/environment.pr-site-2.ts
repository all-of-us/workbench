import { Environment } from 'environments/environment-type';
import { testEnvironmentBase } from 'environments/test-env-base';

// This file is used in the deployed test environment
export const environment: Environment = {
  ...testEnvironmentBase,
  allOfUsApiUrl:
    'https://pr-2-dot-api-dot-all-of-us-workbench-test.appspot.com',
  displayTag: 'PR Site 2',
  debug: false,
};
