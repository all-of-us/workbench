import {testEnvironmentBase} from './test-env-base';

export const environment = {
  ...testEnvironmentBase,
  displayTag: 'Local->Test',
  debug: true,
  enableTemporal: false
};
