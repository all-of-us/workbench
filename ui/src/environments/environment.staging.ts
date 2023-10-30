import { Environment, ZendeskEnv } from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Staging',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api.staging.fake-research-aou.org',
  captchaSiteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI',
  clientId:
    '657299777109-kvb5qafr70bl01i6bnpgsiq5nt6v1o8u.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://aou-db-staging.appspot.com',
  debug: false,
  gaId: 'G-BYPVFY24Q2',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  gaUserInstitutionCategoryDimension: 'dimension3',
  zendeskEnv: ZendeskEnv.Sandbox,
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  allowTestAccessTokenOverride: true,
  showNewAnalysisTab: true,
  showCBFunnelPlot: true,
};
