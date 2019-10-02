import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

// This file is used in the deployed test environment
export const environment: Environment = {
  ...testEnvironmentBase,
  displayTag: 'Test',
  debug: false,
  enablePublishedWorkspaces: true,
  enableAccountPages: true
};
