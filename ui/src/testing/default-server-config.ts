import {
  AccessModule,
  AccessModuleConfig,
  ConfigResponse,
} from 'generated/fetch/api';

const defaultAccessModuleConfig: AccessModuleConfig[] = [
  {
    name: AccessModule.TWOFACTORAUTH,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.RASLINKLOGINGOV,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.ERACOMMONS,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: false,
  },
  {
    name: AccessModule.COMPLIANCETRAINING,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.CTCOMPLIANCETRAINING,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.DATAUSERCODEOFCONDUCT,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.PROFILECONFIRMATION,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.PUBLICATIONCONFIRMATION,
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
  tanagraBaseUrl: 'https://aou-tanagra.dev.pmi-ops.org',
};

export default defaultServerConfig;
