import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

// This file is used for a local UI server pointed at the test API server, i.e. what happens when you
// run yarn dev-up with no arguments
export const environment: Environment = {
  ...testEnvironmentBase,
  displayTag: 'Local->Test',
  debug: true,
  enablePublishedWorkspaces: false
};
