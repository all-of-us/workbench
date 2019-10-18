import {Environment} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Stable',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api-dot-all-of-us-rw-stable.appspot.com',
  clientId: '56507752110-ovdus1lkreopsfhlovejvfgmsosveda6.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://www.databrowser.stable.fake-research-aou.org',
  debug: false,
  gaId: 'UA-112406425-3',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  zendeskHelpCenterUrl: 'http://aousupporthelp.zendesk.com/hc',
  trainingUrl: 'https://aoudev.nnlm.gov',
  shibbolethUrl: 'https://shibboleth.dsde-prod.broadinstitute.org',
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  enableHomepageRestyle: false,
  enablePublishedWorkspaces: false,
  enableAccountPages: false,
  enableProfileCapsFeatures: false,
};
