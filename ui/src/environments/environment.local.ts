import {Environment} from 'environments/environment-type';
import {testEnvironmentBase} from 'environments/test-env-base';

// This file is used for a local UI server pointed at a local API server
export const environment: Environment = {
  displayTag: 'Local->Local',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'http://localhost:8081',
  captchaSiteKey: '6Lc9VdwUAAAAAJy8N_smAmq1FEC3i7WaLBdXIhnA',
  clientId: testEnvironmentBase.clientId,
  trainingUrl: 'https://aoudev.nnlm.gov',
  leoApiUrl: 'https://leonardo.dsde-dev.broadinstitute.org',
  publicUiUrl: 'http://localhost:4201',
  debug: true,
  gaId: 'UA-112406425-5',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  zendeskHelpCenterUrl: 'https://aousupporthelp1580753096.zendesk.com/hc',
  createBillingAccountHelpUrl: 'https://aousupporthelp1580753096.zendesk.com/hc/en-us/articles/360039550031-Instructions-to-Create-a-Billing-Account',
  zendeskWidgetKey: 'df0a2e39-f8a8-482b-baf5-af82e14d38f9',
  shibbolethUrl: 'http://mock-nih.dev.test.firecloud.org',
  inactivityTimeoutSeconds: 99999999999,
  inactivityWarningBeforeSeconds: 5 * 60,
  enableCaptcha: true,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: true,
  enableNewConceptTabs: true
};
