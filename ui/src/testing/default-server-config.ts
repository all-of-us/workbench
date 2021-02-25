import {ConfigResponse} from 'generated/fetch/api';

const defaultServerConfig: ConfigResponse = {
  gsuiteDomain: 'researchallofus.org',
  projectId: 'all-of-us-rw-prod',
  firecloudURL: 'https://firecloud.org',
  publicApiKeyForErrorReports: 'notasecret',
  shibbolethUiBaseUrl: 'https://broad-shibboleth-prod.appspot.com/',
  enableComplianceTraining: true,
  enableDataUseAgreement: true,
  enableEraCommons: true,
  unsafeAllowSelfBypass: false,
  defaultFreeCreditsDollarLimit: 300,
  enableBillingUpgrade: true,
};

export default defaultServerConfig;
