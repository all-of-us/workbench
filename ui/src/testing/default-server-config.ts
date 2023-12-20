import {
  AccessModule,
  AccessModuleConfig,
  ConfigResponse,
} from 'generated/fetch';

const defaultAccessModuleConfig: AccessModuleConfig[] = [
  {
    name: AccessModule.TWO_FACTOR_AUTH,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.IDENTITY,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.ERA_COMMONS,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: false,
  },
  {
    name: AccessModule.COMPLIANCE_TRAINING,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.CT_COMPLIANCE_TRAINING,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.DATA_USER_CODE_OF_CONDUCT,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.PROFILE_CONFIRMATION,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.PUBLICATION_CONFIRMATION,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
];

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
  enableRasIdMeLinking: false,
  enableRasLoginGovLinking: true,
  accessRenewalLookback: 330,
  complianceTrainingRenewalLookback: 30,
  freeTierBillingAccountId: 'freetier',
  accessModules: defaultAccessModuleConfig,
  currentDuccVersions: [3, 4],
  enableRStudioGKEApp: true,
  enableSasGKEApp: true,
  tanagraBaseUrl: 'https://test.fake-research-aou.org',
  enableGKEAppPausing: false,
};

export default defaultServerConfig;
