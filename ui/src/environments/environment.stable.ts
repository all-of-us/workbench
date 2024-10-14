import { Environment, ZendeskEnv } from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'Stable',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api.stable.fake-research-aou.org',
  captchaSiteKey: '6LcKXeQUAAAAAEK734FfI8O3BTzCMhewzmI2sBeC',
  clientId:
    '56507752110-ovdus1lkreopsfhlovejvfgmsosveda6.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://www.databrowser.stable.fake-research-aou.org',
  debug: false,
  gaId: 'G-KBF2CXWBNL',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  gaUserInstitutionCategoryDimension: 'dimension3',
  zendeskEnv: ZendeskEnv.Sandbox,
  inactivityTimeoutSecondsRt: 24 * 60 * 60, // 24 hours
  inactivityTimeoutSecondsCt: 24 * 60 * 60, // 24 hours
  allowTestAccessTokenOverride: false,
  tanagraLocalAuth: false,
  showDataAppsVersionSelect: false,
};
