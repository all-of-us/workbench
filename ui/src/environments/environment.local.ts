import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

export const environment: Environment = {
  displayTag: 'Local->Local',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'http://localhost:8081',
  clientId: testEnvironmentBase.clientId,
  trainingUrl: 'https://aoudev.nnlm.gov',
  leoApiUrl: 'https://leonardo.dsde-dev.broadinstitute.org',
  publicUiUrl: 'http://localhost:4201',
  debug: true,
  gaId: 'UA-112406425-5',
  gaUserAgentDimension: 'dimension1',
  zendeskHelpCenterUrl: 'http://aousupporthelp.zendesk.com/hc',
  shibbolethUrl: 'http://mock-nih.dev.test.firecloud.org',
  inactivityTimeoutSeconds: 99999999999,
  inactivityWarningBeforeSeconds: 5 * 60,
  enableJupyterLab: true,
};
