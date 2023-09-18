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
    moduleName: AccessModule.PROFILE_CONFIRMATION,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(365),
  },
  {
    moduleName: AccessModule.PUBLICATION_CONFIRMATION,
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

const allBypassed: AccessModuleStatus[] = Object.keys(AccessModule).map(
  (key) => {
    return {
      moduleName: AccessModule[key],
      bypassEpochMillis: Date.now(),
    };
  }
);

// 2FA is missing (initial, not renewable)
const allCompleteMissingOneInitial: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) => moduleName !== AccessModule.TWO_FACTOR_AUTH
  );

// RW-8203
// artificial state for test users - Publications is missing (renewable, not initial registration)
const allCompleteMissingOneRenewable: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) => moduleName !== AccessModule.PUBLICATION_CONFIRMATION
  );

const allCompleteMissingOneEach: AccessModuleStatus[] =
  allCompleteNotExpiring.filter(
    ({ moduleName }) =>
      !(
        [
          AccessModule.TWO_FACTOR_AUTH,
          AccessModule.PUBLICATION_CONFIRMATION,
        ] as Array<AccessModule>
      ).includes(moduleName)
  );

// PUBLICATIONCONFIRMATION is expired
const allCompleteOneExpired: AccessModuleStatus[] =
  allCompleteMissingOneRenewable.concat({
    moduleName: AccessModule.PUBLICATION_CONFIRMATION,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(-1),
  });

const allCompleteCtTrainingExpired: AccessModuleStatus[] =
  allCompleteNotExpiring
    .filter(
      ({ moduleName }) => moduleName !== AccessModule.CT_COMPLIANCE_TRAINING
    )
    .concat({
      moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
      completionEpochMillis: Date.now(),
      expirationEpochMillis: nowPlusDays(-1),
    });

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
      'all bypassed',
      undefined,
      {
        ...ProfileStubVariables.PROFILE_STUB,
        accessTierShortNames: [AccessTierShortNames.Registered],
        accessModules: {
          modules: allBypassed,
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
