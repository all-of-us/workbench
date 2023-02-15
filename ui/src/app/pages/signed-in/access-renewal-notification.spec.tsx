import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { AccessModule, AccessModuleStatus } from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import { nowPlusDays } from 'app/utils/dates';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import {
  AccessRenewalNotificationMaybe,
  AccessRenewalNotificationProps,
} from './access-renewal-notification';

const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

const allModules = [
  AccessModule.DATAUSERCODEOFCONDUCT,
  AccessModule.COMPLIANCETRAINING,
  AccessModule.CTCOMPLIANCETRAINING,
  AccessModule.ERACOMMONS,
  AccessModule.TWOFACTORAUTH,
  AccessModule.RASLINKLOGINGOV,
  AccessModule.PROFILECONFIRMATION,
  AccessModule.PUBLICATIONCONFIRMATION,
];

const allCompleteNotExpiring: AccessModuleStatus[] = allModules.map(
  (moduleName) => ({
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(365),
  })
);

const allCompleteOneExpiring = (moduleName: AccessModule) => [
  ...allCompleteNotExpiring.filter(
    (status) => status.moduleName !== moduleName
  ),
  {
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(1),
  },
];

const allCompleteOneExpired = (moduleName: AccessModule) => [
  ...allCompleteNotExpiring.filter(
    (status) => status.moduleName !== moduleName
  ),
  {
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(-1),
  },
];

const rtExpiresFirst = [
  ...allCompleteNotExpiring.filter(
    (status) =>
      ![
        AccessModule.COMPLIANCETRAINING,
        AccessModule.CTCOMPLIANCETRAINING,
      ].includes(status.moduleName)
  ),
  {
    moduleName: AccessModule.COMPLIANCETRAINING, // RT
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(5),
  },
  {
    moduleName: AccessModule.CTCOMPLIANCETRAINING,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(10),
  },
];

const ctExpiresFirst = [
  ...allCompleteNotExpiring.filter(
    (status) =>
      ![
        AccessModule.COMPLIANCETRAINING,
        AccessModule.CTCOMPLIANCETRAINING,
      ].includes(status.moduleName)
  ),
  {
    moduleName: AccessModule.COMPLIANCETRAINING, // RT
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(10),
  },
  {
    moduleName: AccessModule.CTCOMPLIANCETRAINING,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(5),
  },
];

const updateModules = (modules: AccessModuleStatus[]) => {
  profileStore.set({
    profile: {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: { modules },
    },
    load,
    reload,
    updateCache,
  });
};

describe('Access Renewal Notification', () => {
  const component = (props: AccessRenewalNotificationProps) => {
    return mount(
      <MemoryRouter>
        <AccessRenewalNotificationMaybe {...props} />
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });
  });

  test.each([
    [
      false,
      'for RT when there are no modules',
      AccessTierShortNames.Registered,
      [],
    ],
    [
      false,
      'for CT when there are no modules',
      AccessTierShortNames.Controlled,
      [],
    ],
    [
      false,
      'for RT when there are no expiring modules',
      AccessTierShortNames.Registered,
      allCompleteNotExpiring,
    ],
    [
      false,
      'for CT when there are no expiring modules',
      AccessTierShortNames.Controlled,
      allCompleteNotExpiring,
    ],
    [
      true,
      'for RT when there is one expiring module',
      AccessTierShortNames.Registered,
      allCompleteOneExpiring(AccessModule.DATAUSERCODEOFCONDUCT),
    ],
    [
      // Showing the RT banner appears in this case, so CT is not needed
      false,
      'for CT when there is one expiring module',
      AccessTierShortNames.Controlled,
      allCompleteOneExpiring(AccessModule.DATAUSERCODEOFCONDUCT),
    ],
    [
      true,
      'for RT when there is one expired module',
      AccessTierShortNames.Registered,
      allCompleteOneExpired(AccessModule.DATAUSERCODEOFCONDUCT),
    ],
    [
      // Showing the RT banner appears in this case, so CT is not needed
      false,
      'for CT when there is one expired module',
      AccessTierShortNames.Controlled,
      allCompleteOneExpired(AccessModule.DATAUSERCODEOFCONDUCT),
    ],
    [
      false,
      'for RT when only CT training is expiring',
      AccessTierShortNames.Registered,
      allCompleteOneExpiring(AccessModule.CTCOMPLIANCETRAINING),
    ],
    [
      false,
      'for RT when only CT training is expired',
      AccessTierShortNames.Registered,
      allCompleteOneExpired(AccessModule.CTCOMPLIANCETRAINING),
    ],
    [
      true,
      'for CT when only CT training is expiring',
      AccessTierShortNames.Controlled,
      allCompleteOneExpiring(AccessModule.CTCOMPLIANCETRAINING),
    ],
    [
      true,
      'for CT when only CT training is expired',
      AccessTierShortNames.Controlled,
      allCompleteOneExpired(AccessModule.CTCOMPLIANCETRAINING),
    ],
    [
      true,
      'for RT when RT is expiring sooner',
      AccessTierShortNames.Registered,
      rtExpiresFirst,
    ],
    [
      // Showing the RT banner appears in this case, so CT is not needed
      false,
      'for CT when RT is expiring sooner',
      AccessTierShortNames.Controlled,
      rtExpiresFirst,
    ],
    [
      // Still need to show RT, because it's more important
      true,
      'for RT when CT is expiring sooner',
      AccessTierShortNames.Registered,
      ctExpiresFirst,
    ],
    [
      true,
      'for CT when CT is expiring sooner',
      AccessTierShortNames.Controlled,
      ctExpiresFirst,
    ],
  ])(
    'display=%s %s',
    async (expected, condition, accessTier, moduleStatuses) => {
      updateModules(moduleStatuses);

      const wrapper = component({
        accessTier,
      });
      expect(wrapper.exists()).toBeTruthy();

      const banner = wrapper.find({
        'data-test-id': 'access-renewal-notification',
      });
      expect(banner.exists()).toEqual(expected);
    }
  );
});
