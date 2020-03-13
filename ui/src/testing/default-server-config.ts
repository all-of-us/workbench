import {ConfigResponse} from 'generated/fetch/api';

const defaultServerConfig: ConfigResponse = {
  gsuiteDomain: 'researchallofus.org',
  projectId: 'all-of-us-rw-prod',
  firecloudURL: 'https://firecloud.org',
  publicApiKeyForErrorReports: 'notasecret',
  enableComplianceTraining: true,
  enableDataUseAgreement: true,
  enableEraCommons: true,
  unsafeAllowSelfBypass: false,
  defaultFreeCreditsDollarLimit: 300,
  enableBillingLockout: true,
  requireInvitationKey: true,
  requireInstitutionalVerification: true,
  enableCBAgeTypeOptions: true,
};

export default defaultServerConfig;
