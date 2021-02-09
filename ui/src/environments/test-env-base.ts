import {EnvironmentBase, ZendeskEnv} from 'environments/environment-type';

// The values are shared across the deployed test env as well as the local dev
// environments.
export const testEnvironmentBase: EnvironmentBase = {
  allOfUsApiUrl: 'https://api-dot-all-of-us-workbench-test.appspot.com',
  clientId: '602460048110-5uk3vds3igc9qo0luevroc2uc3okgbkt.apps.googleusercontent.com',
  // Captcha Site key for test is same as that of local since both point to the same server keys
  captchaSiteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI',
  // Keep in sync with config_test.json.
  leoApiUrl: 'https://leonardo.dsde-dev.broadinstitute.org',
  publicUiUrl: 'https://aou-db-test.appspot.com',
  gaId: 'UA-112406425-1',
  // note: these are shifted by 1 from other environments due to an extra
  // custom GA dimension "Test" which has the #1 spot in Test
  gaUserAgentDimension: 'dimension2',
  gaLoggedInDimension: 'dimension3',
  gaUserInstitutionCategoryDimension: 'dimension4',
  trainingUrl: 'https://aoudev.nnlm.gov',
  zendeskEnv: ZendeskEnv.Sandbox,
  shouldShowDisplayTag: true,
  inactivityTimeoutSeconds: 99999999999,
  inactivityWarningBeforeSeconds: 5 * 60,
  allowTestAccessTokenOverride: true,
  enableCaptcha: true,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: true,
  enableNewConceptTabs: true,
  enableFooter: true
};
