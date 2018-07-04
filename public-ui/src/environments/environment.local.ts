import {testEnvironmentBase} from 'environments/test-env-base';

export const environment = {
  displayTag: 'Local->Local',
  clientId: testEnvironmentBase.clientId,
  publicApiUrl: 'http://localhost:8083',
  debug: true,
  testing: false
};
