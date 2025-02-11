import { EnvironmentBase, ZendeskEnv } from 'environments/environment-type';

// The values are shared across the deployed test env as well as the local dev
// environments.
export const testEnvironmentBase: EnvironmentBase = {
  allOfUsApiUrl: 'https://api.test.fake-research-aou.org',
  clientId:
    '602460048110-5uk3vds3igc9qo0luevroc2uc3okgbkt.apps.googleusercontent.com',
  // Captcha Site key for test is same as that of local since both point to the same server keys
  captchaSiteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI',
  // Keep in sync with config_test.json.
  leoApiUrl: 'https://leonardo.dsde-dev.broadinstitute.org',
  publicUiUrl: 'https://aou-db-test.appspot.com',
  gaId: 'G-Q111PQFJH3',
  // note: these are shifted by 1 from other environments due to an extra
  // custom GA dimension "Test" which has the #1 spot in Test
  gaUserAgentDimension: 'dimension2',
  gaLoggedInDimension: 'dimension3',
  gaUserInstitutionCategoryDimension: 'dimension4',
  zendeskEnv: ZendeskEnv.Sandbox,
  shouldShowDisplayTag: true,
  inactivityTimeoutSecondsRt: 24 * 60 * 60, // 24 hours
  inactivityTimeoutSecondsCt: 24 * 60 * 60, // 24 hours
  allowTestAccessTokenOverride: true,
  tanagraLocalAuth: false,
  vwbUiUrl: 'https://workbench-dev.verily.com',
};
