import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

// This file is used for a local UI server pointed at a local API server
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
  gaLoggedInDimension: 'dimension2',
  zendeskHelpCenterUrl: 'https://aousupporthelp1579899699.zendesk.com/hc',
  zendeskWidgetKey: '6593967d-4352-4915-9e0e-b45226c1e518',
  shibbolethUrl: 'http://mock-nih.dev.test.firecloud.org',
  inactivityTimeoutSeconds: 99999999999,
  inactivityWarningBeforeSeconds: 5 * 60,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: true,
  enableNewConceptTabs: true
};
