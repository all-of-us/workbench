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
  ...allCompleteNotExpiring,
  {
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(1),
  },
];

const allCompleteOneExpired = (moduleName: AccessModule) => [
  ...allCompleteNotExpiring,
  {
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(-1),
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
      true,
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
      true,
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
