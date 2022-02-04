import { ConfigResponse } from 'generated/fetch/api';

const defaultServerConfig: ConfigResponse = {
  gsuiteDomain: 'researchallofus.org',
  projectId: 'all-of-us-rw-prod',
  firecloudURL: 'https://firecloud.org',
  publicApiKeyForErrorReports: 'notasecret',
  shibbolethUiBaseUrl: 'https://broad-shibboleth-prod.appspot.com/',
  enableComplianceTraining: true,
  enableEraCommons: true,
  unsafeAllowSelfBypass: false,
  defaultFreeCreditsDollarLimit: 300,
  rasHost: 'https://stsstg.nih.gov/',
  rasClientId: '903cfaeb-57d9-4ef6-5659-04377794ed65',
  enableRasLoginGovLinking: true,
  enableGpu: true,
  enablePersistentDisk: true,
  accessRenewalLookback: 330,
  freeTierBillingAccountId: 'freetier',
};

export default defaultServerConfig;
