import {testEnvironmentBase} from 'environments/test-env-base';

export const environment = {
  ...testEnvironmentBase,
  displayTag: 'Local->Test',
  debug: true,
  testing: false
};
