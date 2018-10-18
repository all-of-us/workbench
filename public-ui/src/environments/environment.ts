import {testEnvironmentBase} from 'environments/test-env-base';

export const environment = {
  ...testEnvironmentBase,
  displayTag: 'Local->Test',
  workbenchUrl: 'http://localhost:4200',
  debug: true,
};
