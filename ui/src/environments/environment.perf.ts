import {Environment} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Perf',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api-dot-all-of-us-rw-perf.appspot.com',
  clientId: '63939010390-aj0r8hro7r8lkt7a45gissu3m73ietl2.apps.googleusercontent.com',
  leoApiUrl: 'https://leonardo.dsde-perf.broadinstitute.org',
  // There is no perf environment for the data browser
  publicUiUrl: 'https://aou-db-staging.appspot.com',
  debug: false,
  gaId: 'UA-112406425-2',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  trainingUrl: 'https://aoudev.nnlm.gov',
  zendeskHelpCenterUrl: 'http://aousupporthelp.zendesk.com/hc',
  shibbolethUrl: 'https://shibboleth.dsde-perf.broadinstitute.org',
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  enableHomepageRestyle: true,
  enablePublishedWorkspaces: false,
  enableAccountPages: false,
  enableProfileCapsFeatures: false,
};
