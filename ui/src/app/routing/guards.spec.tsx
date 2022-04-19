import { AccessModule, AccessModuleStatus, Profile } from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import { nowPlusDays } from 'app/utils/dates';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { AccessModulesRedirection, redirectTo } from './guards';

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

const allCompleteNotExpiring: AccessModuleStatus[] = Object.keys(
  AccessModule
).map((key) => ({
  moduleName: AccessModule[key],
  completionEpochMillis: Date.now(),
  expirationEpochMillis: nowPlusDays(365),
}));

const allCompleteExpiringSoon: AccessModuleStatus[] = Object.keys(
  AccessModule
).map((key) => ({
  moduleName: AccessModule[key],
  completionEpochMillis: Date.now(),
  expirationEpochMillis: nowPlusDays(1),
}));

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

// PUBLICATIONCONFIRMATION is expired
const allCompleteOneExpired: AccessModuleStatus[] =
  allCompleteMissingOneRenewable.concat({
    moduleName: AccessModule.PUBLICATIONCONFIRMATION,
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

describe('redirectTo', () => {
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
  });

  test.each([
    [
      'all complete and not expiring',
      AccessModulesRedirection.NO_REDIRECT,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          anyModuleHasExpired: false,
          modules: allCompleteNotExpiring,
        },
      },
    ],
    [
      'all complete but expiring soon',
      AccessModulesRedirection.NO_REDIRECT,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          anyModuleHasExpired: false,
          modules: allCompleteExpiringSoon,
        },
      },
    ],
    [
      'initially created state',
      AccessModulesRedirection.DATA_ACCESS_REQUIREMENTS,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          anyModuleHasExpired: false,
          modules: newUserModuleState,
        },
      },
    ],
    [
      'all complete but one module is expired',
      AccessModulesRedirection.ACCESS_RENEWAL,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          anyModuleHasExpired: true,
          modules: allCompleteOneExpired,
        },
      },
    ],
    [
      'all complete but missing one (initial, not expirable/renewable)',
      AccessModulesRedirection.DATA_ACCESS_REQUIREMENTS,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          anyModuleHasExpired: false,
          modules: allCompleteMissingOneInitial,
        },
      },
    ],
    // RW-8203 this fails
    [
      'all complete but missing one (renewable, not initial)',
      AccessModulesRedirection.ACCESS_RENEWAL,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          anyModuleHasExpired: false,
          modules: allCompleteMissingOneRenewable,
        },
      },
    ],
    // RW-8203 this fails
    [
      'all complete but Profile + Publication Confirmations are missing',
      // Access Renewal is the only page which allows progress on these modules
      AccessModulesRedirection.ACCESS_RENEWAL,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          anyModuleHasExpired: false,
          modules: allCompleteMissingPP,
        },
      },
    ],
    // RW-8203 this fails
    [
      'all incomplete',
      // such a user would have to visit Access Renewal first, allowing progress on Profile + Publication
      // after the user does this, they are in the "initially created" state with respect to modules
      // so they would be redirected to Data Access Requirements
      AccessModulesRedirection.ACCESS_RENEWAL,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [],
        accessModules: {
          anyModuleHasExpired: false,
          modules: [],
        },
      },
    ],
  ])('%s', (desc, expected: AccessModulesRedirection, profile: Profile) => {
    expect(redirectTo(profile)).toEqual(expected);
  });
});
