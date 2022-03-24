import { Environment, ZendeskEnv } from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'preprod',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api.preprod-workbench.researchallofus.org',
  captchaSiteKey: '6LceVfYUAAAAAFInPvKl_bGoiyGyA3Y3dVp4o8Ly',
  clientId:
    '589109405884-bmoj9ra8849rqeepuamk8jpu102iq363.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://databrowser.researchallofus.org',
  debug: false,
  gaId: 'UA-112406425-7',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  gaUserInstitutionCategoryDimension: 'dimension3',
  zendeskEnv: ZendeskEnv.Preprod,
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  allowTestAccessTokenOverride: false,
  enableCaptcha: true,
  enablePublishedWorkspaces: false,
  enableFooter: true,
  enableAsyncWorkspaceOperations: true,
  mergedAccessRenewal: false,
  enableTOSRedirectForLoggedInUser: false,
};
