import {
  AccessModule,
  AccessModuleConfig,
  ConfigResponse,
} from 'generated/fetch/api';

const defaultAccessModuleConfig: AccessModuleConfig[] = [
  {
    name: AccessModule.TWOFACTORAUTH,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.RASLINKLOGINGOV,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.ERACOMMONS,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: false,
  },
  {
    name: AccessModule.COMPLIANCETRAINING,
    bypassable: true,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.CTCOMPLIANCETRAINING,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.DATAUSERCODEOFCONDUCT,
    bypassable: true,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.PROFILECONFIRMATION,
    bypassable: false,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    name: AccessModule.PUBLICATIONCONFIRMATION,
    bypassable: false,
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
  enableRasLoginGovLinking: true,
  enableGpu: true,
  enablePersistentDisk: true,
  accessRenewalLookback: 330,
  complianceTrainingRenewalLookback: 30,
  freeTierBillingAccountId: 'freetier',
  accessModules: defaultAccessModuleConfig,
  currentDuccVersions: [3, 4],
  enableControlledTierTrainingRenewal: true,
  enableRStudioGKEApp: true,
};

export default defaultServerConfig;
