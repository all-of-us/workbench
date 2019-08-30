import {Environment} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Staging',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api-dot-all-of-us-rw-staging.appspot.com',
  clientId: '657299777109-kvb5qafr70bl01i6bnpgsiq5nt6v1o8u.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://aou-db-staging.appspot.com',
  debug: false,
  gaId: 'UA-112406425-2',
  gaUserAgentDimension: 'dimension1',
  trainingUrl: 'https://aoudev.nnlm.gov',
  zendeskHelpCenterUrl: 'http://aousupporthelp.zendesk.com/hc',
  shibbolethUrl: 'https://shibboleth.dsde-prod.broadinstitute.org',
  inactivityTimeoutInSeconds: 30 * 60,
  inactivityWarningInSeconds: 5 * 60,
  enableJupyterLab: false,
};
