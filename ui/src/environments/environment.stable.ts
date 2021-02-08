import {Environment, ZendeskEnv} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Stable',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api-dot-all-of-us-rw-stable.appspot.com',
  captchaSiteKey: '6LcKXeQUAAAAAEK734FfI8O3BTzCMhewzmI2sBeC',
  clientId: '56507752110-ovdus1lkreopsfhlovejvfgmsosveda6.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://www.databrowser.stable.fake-research-aou.org',
  debug: false,
  gaId: 'UA-112406425-3',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  gaUserInstitutionCategoryDimension: 'dimension3',   // TODO create
  zendeskEnv: ZendeskEnv.Sandbox,
  trainingUrl: 'https://aoudev.nnlm.gov',
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  allowTestAccessTokenOverride: false,
  enableCaptcha: true,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: true,
  enableNewConceptTabs: true,
  enableFooter: true
};
