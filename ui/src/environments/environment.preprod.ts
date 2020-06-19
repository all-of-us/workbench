import {Environment} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: 'preprod',
  shouldShowDisplayTag: true,
  allOfUsApiUrl: 'https://api.preprod-workbench.researchallofus.org',
  captchaSiteKey: '6LceVfYUAAAAAFInPvKl_bGoiyGyA3Y3dVp4o8Ly',
  clientId: '589109405884-bmoj9ra8849rqeepuamk8jpu102iq363.apps.googleusercontent.com',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicUiUrl: 'https://databrowser.researchallofus.org',
  debug: false,
  gaId: 'UA-112406425-7',
  gaUserAgentDimension: 'dimension1',
  gaLoggedInDimension: 'dimension2',
  zendeskHelpCenterUrl: 'http://aousupporthelp.zendesk.com/hc',
  createBillingAccountHelpUrl: 'https://aousupporthelp.zendesk.com/hc/en-us/articles/360039539411-How-to-Create-a-Billing-Account',
  zendeskWidgetKey: '5a7d70b9-37f9-443b-8d0e-c3bd3c2a55e3',
  shibbolethUrl: 'https://shibboleth.dsde-prod.broadinstitute.org',
  trainingUrl: 'https://aou.nnlm.gov',
  inactivityTimeoutSeconds: 30 * 60,
  inactivityWarningBeforeSeconds: 5 * 60,
  enableCaptcha: true,
  enablePublishedWorkspaces: false,
  enableProfileCapsFeatures: true,
  enableNewConceptTabs: false,
  enableFooter: true
};
