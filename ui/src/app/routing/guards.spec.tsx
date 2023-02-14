import { AccessModule, AccessModuleStatus, Profile } from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  ACCESS_RENEWAL_PATH,
  DATA_ACCESS_REQUIREMENTS_PATH,
} from 'app/utils/access-utils';
import { nowPlusDays } from 'app/utils/dates';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { shouldRedirectToMaybe } from './guards';

// a newly-created user will have Profile and Publications newly completed, and no others
const newUserModuleState: AccessModuleStatus[] = [
  {
    moduleName: AccessModule.PROFILECONFIRMATION,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(365),
  },
  {
    moduleName: AccessModule.PUBLICATIONCONFIRMATION,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(365),
  },
];

const allAccessModuleNames = [
  AccessModule.DATAUSERCODEOFCONDUCT,
  AccessModule.COMPLIANCETRAINING,
  AccessModule.CTCOMPLIANCETRAINING,
  AccessModule.ERACOMMONS,
  AccessModule.TWOFACTORAUTH,
  AccessModule.RASLINKLOGINGOV,
  AccessModule.PROFILECONFIRMATION,
  AccessModule.PUBLICATIONCONFIRMATION,
];

const allCompleteNotExpiring: AccessModuleStatus[] = allAccessModuleNames.map(
  (moduleName) => ({
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(365),
  })
);

const allCompleteExpiringSoon: AccessModuleStatus[] = allAccessModuleNames.map(
  (moduleName) => ({
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(1),
  })
);

const allBypassedPPComplete: AccessModuleStatus[] = allAccessModuleNames.map(
  (moduleName) => {
    return [
      AccessModule.PROFILECONFIRMATION,
      AccessModule.PUBLICATIONCONFIRMATION,
    ].includes(moduleName)
      ? {
          moduleName,
          completionEpochMillis: Date.now(),
        }
      : {
          moduleName,
          bypassEpochMillis: Date.now(),
        };
  }
);

// 2FA is missing (initial, not renewable)
const allCompleteMissingOneInitial: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) => moduleName !== AccessModule.TWOFACTORAUTH
  );

// RW-8203
// artificial state for test users - Publications is missing (renewable, not initial registration)
const allCompleteMissingOneRenewable: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) => moduleName !== AccessModule.PUBLICATIONCONFIRMATION
  );

const allCompleteMissingOneEach: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) =>
      ![
        AccessModule.TWOFACTORAUTH,
        AccessModule.PUBLICATIONCONFIRMATION,
      ].includes(moduleName)
  );

// PUBLICATIONCONFIRMATION is expired
const allCompleteOneExpired: AccessModuleStatus[] =
  allCompleteMissingOneRenewable.concat({
    moduleName: AccessModule.PUBLICATIONCONFIRMATION,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(-1),
  });

const allCompleteCtTrainingExpired: AccessModuleStatus[] =
  allCompleteNotExpiring
    .filter(
      ({ moduleName }) => moduleName !== AccessModule.CTCOMPLIANCETRAINING
    )
    .concat({
      moduleName: AccessModule.CTCOMPLIANCETRAINING,
      completionEpochMillis: Date.now(),
      expirationEpochMillis: nowPlusDays(-1),
    });

// RW-8203
// artificial state for test users - all complete but Profile/Publications are missing
const allCompleteMissingPP: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) =>
      ![
        AccessModule.PROFILECONFIRMATION,
        AccessModule.PUBLICATIONCONFIRMATION,
      ].includes(moduleName)
  );

// RW-8203
// artificial state for test users - all bypassed but Profile/Publications are missing
const allBypassedMissingPP: AccessModuleStatus[] = allBypassedPPComplete.filter(
  ({ moduleName }) =>
    ![
      AccessModule.PROFILECONFIRMATION,
      AccessModule.PUBLICATIONCONFIRMATION,
    ].includes(moduleName)
);

describe('redirectTo', () => {
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
  });

  test.each([
    [
      'all complete and not expiring',
      undefined,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          modules: allCompleteNotExpiring,
        },
      },
    ],
    [
      'all complete but expiring soon',
      undefined,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          modules: allCompleteExpiringSoon,
        },
      },
    ],
    [
      'all bypassed except Profile and Publications are missing',
      // Access Renewal is the only page which allows progress on these modules
      ACCESS_RENEWAL_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: allBypassedMissingPP,
        },
      },
    ],
    [
      'all bypassed except Profile and Publications are complete',
      undefined,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          modules: allBypassedPPComplete,
        },
      },
    ],
    [
      'initially created state',
      DATA_ACCESS_REQUIREMENTS_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: newUserModuleState,
        },
      },
    ],
    [
      'all complete but one module is expired',
      ACCESS_RENEWAL_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: allCompleteOneExpired,
        },
      },
    ],
    [
      'all complete but Controlled Tier training is expired',
      undefined,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          modules: allCompleteCtTrainingExpired,
        },
      },
    ],
    [
      'all complete but missing one (initial, not expirable/renewable)',
      DATA_ACCESS_REQUIREMENTS_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: allCompleteMissingOneInitial,
        },
      },
    ],
    // RW-8203
    [
      'all complete but missing one (renewable, not initial)',
      // Access Renewal is the only page which allows progress on PUBLICATIONCONFIRMATION
      ACCESS_RENEWAL_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: allCompleteMissingOneRenewable,
        },
      },
    ],
    // RW-8203
    [
      'all complete but missing one each (1 renewable, 1 initial)',
      // Access Renewal is the only page which allows progress on PUBLICATIONCONFIRMATION
      ACCESS_RENEWAL_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: allCompleteMissingOneEach,
        },
      },
    ],
    // RW-8203
    [
      'all complete but Profile + Publication Confirmations are missing',
      // Access Renewal is the only page which allows progress on these modules
      ACCESS_RENEWAL_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: allCompleteMissingPP,
        },
      },
    ],
    // RW-8203
    [
      'all incomplete',
      // Such a user would have to visit Access Renewal first, allowing progress on Profile + Publication.
      // After the user does this, they are in the "initially created" state with respect to modules
      // so they would be redirected to Data Access Requirements
      ACCESS_RENEWAL_PATH,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          modules: [],
        },
      },
    ],
  ])('%s', (desc, expected: string, profile: Profile) => {
    expect(shouldRedirectToMaybe(profile)).toEqual(expected);
  });
});
