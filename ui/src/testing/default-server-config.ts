import {
  AccessModuleConfig,
  AccessModuleName,
  ConfigResponse,
} from 'generated/fetch/api';

import { AccessTierShortNames } from 'app/utils/access-tiers';

const defaultAccessModuleConfig: AccessModuleConfig[] = [
  {
    moduleNameTemp: AccessModuleName.TWOFACTORAUTH,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    moduleNameTemp: AccessModuleName.RASLOGINGOV,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    moduleNameTemp: AccessModuleName.ERACOMMONS,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: false,
  },
  {
    moduleNameTemp: AccessModuleName.RTCOMPLIANCETRAINING,
    bypassable: true,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    moduleNameTemp: AccessModuleName.CTCOMPLIANCETRAINING,
    bypassable: true,
    expirable: false,
    requiredForRTAccess: false,
    requiredForCTAccess: true,
  },
  {
    moduleNameTemp: AccessModuleName.DATAUSERCODEOFCONDUCT,
    bypassable: true,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    moduleNameTemp: AccessModuleName.PROFILECONFIRMATION,
    bypassable: false,
    expirable: true,
    requiredForRTAccess: true,
    requiredForCTAccess: true,
  },
  {
    moduleNameTemp: AccessModuleName.PUBLICATIONCONFIRMATION,
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
  freeTierBillingAccountId: 'freetier',
  accessModules: defaultAccessModuleConfig,
  accessTiersVisibleToUsers: [
    AccessTierShortNames.Registered,
    AccessTierShortNames.Controlled,
  ],
};

export default defaultServerConfig;
