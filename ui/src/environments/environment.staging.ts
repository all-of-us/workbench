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
  gaLoggedInDimension: 'dimension2',
  trainingUrl: 'https://aoudev.nnlm.gov',
  zendeskHelpCenterUrl: 'https://aousupporthelp1580753096.zendesk.com/hc',
  createBillingAccountHelpUrl: 'https://aousupporthelp1580753096.zendesk.com/hc/en-us/articles/360039550031-Instructions-to-Create-a-Billing-Account',
  zendeskWidgetKey: 'df0a2e39-f8a8-482b-baf5-af82e14d38f9',
  shibbolethUrl: 'https://shibboleth.dsde-prod.broadinstitute.org',
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: false,
  enableNewConceptTabs: false
};
