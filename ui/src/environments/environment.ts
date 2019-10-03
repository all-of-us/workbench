import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

// This is just a stub for actual environments to override
export const environment: Environment = {
  ...testEnvironmentBase,
  displayTag: 'Stub',
  debug: false
};
