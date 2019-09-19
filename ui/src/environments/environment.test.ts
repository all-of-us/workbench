import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

export const environment: Environment = {
  ...testEnvironmentBase,
  displayTag: 'Test',
  debug: false,
  enablePublishedWorkspaces: true,
  enableAccountPages: false
};
