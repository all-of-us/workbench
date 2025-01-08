import { Environment, ZendeskEnv } from 'environments/environment-type';

export const environment: Environment = {
  displayTag: '',
  shouldShowDisplayTag: false,
  allOfUsApiUrl: 'https://api.workbench.researchallofus.org',
  captchaSiteKey: '6LcsXeQUAAAAAIqvC_rqpUExWsoK4kE9siunPeCG',
  clientId:
    '684273740878-d7i68in5d9hqr6n9mfvrdh53snekp79f.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://databrowser.researchallofus.org',
  debug: false,
  gaId: 'UA-112406425-4',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  gaUserInstitutionCategoryDimension: 'dimension3',
  zendeskEnv: ZendeskEnv.Prod,
  inactivityTimeoutSecondsRt: 6 * 60 * 60, // 6 hours
  inactivityTimeoutSecondsCt: 12 * 60 * 60, // 12 hours
  allowTestAccessTokenOverride: false,
  tanagraLocalAuth: false,
  vwbUiUrl: 'https://workbench.verily.com',
};
