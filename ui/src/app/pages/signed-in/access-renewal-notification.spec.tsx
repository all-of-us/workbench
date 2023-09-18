import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { AccessModule, AccessModuleStatus } from 'generated/fetch';

import { AlarmExclamation } from 'app/components/icons';
import colors from 'app/styles/colors';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { nowPlusDays } from 'app/utils/dates';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import {
  AccessRenewalNotificationMaybe,
  AccessRenewalNotificationProps,
} from './access-renewal-notification';

const allModules = [
  AccessModule.DATA_USER_CODE_OF_CONDUCT,
  AccessModule.COMPLIANCE_TRAINING,
  AccessModule.CT_COMPLIANCE_TRAINING,
  AccessModule.ERA_COMMONS,
  AccessModule.TWO_FACTOR_AUTH,
  AccessModule.RAS_LINK_LOGIN_GOV,
  AccessModule.PROFILE_CONFIRMATION,
  AccessModule.PUBLICATION_CONFIRMATION,
];

const EMPTY_STRING = '';

const allCompleteNotExpiring: AccessModuleStatus[] = allModules.map(
  (moduleName) => ({
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(365),
  })
);

const allCompleteOneExpiringInMoreThanTwoWeeks = (moduleName: AccessModule) => [
  ...allCompleteNotExpiring.filter(
    (status) => status.moduleName !== moduleName
  ),
  {
    moduleName,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(15),
  },
];

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
        AccessModule.COMPLIANCE_TRAINING,
        AccessModule.CT_COMPLIANCE_TRAINING,
      ].includes(status.moduleName)
  ),
  {
    moduleName: AccessModule.COMPLIANCE_TRAINING, // RT
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(5),
  },
  {
    moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(10),
  },
];

const ctExpiresFirst = [
  ...allCompleteNotExpiring.filter(
    (status) =>
      ![
        AccessModule.COMPLIANCE_TRAINING,
        AccessModule.CT_COMPLIANCE_TRAINING,
      ].includes(status.moduleName)
  ),
  {
    moduleName: AccessModule.COMPLIANCE_TRAINING, // RT
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(10),
  },
  {
    moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
    completionEpochMillis: Date.now(),
    expirationEpochMillis: nowPlusDays(5),
  },
];

const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

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
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      false,
      'for CT when there are no modules',
      AccessTierShortNames.Controlled,
      [],
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      false,
      'for RT when there are no expiring modules',
      AccessTierShortNames.Registered,
      allCompleteNotExpiring,
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      false,
      'for CT when there are no expiring modules',
      AccessTierShortNames.Controlled,
      allCompleteNotExpiring,
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      true,
      'for RT when there is one expiring module',
      AccessTierShortNames.Registered,
      allCompleteOneExpiring(AccessModule.DATA_USER_CODE_OF_CONDUCT),
      colors.notificationBanner.expiring_within_one_week,
      colors.danger,
    ],
    [
      true,
      'for RT when there is one module expiring in two weeks',
      AccessTierShortNames.Registered,
      allCompleteOneExpiringInMoreThanTwoWeeks(
        AccessModule.DATA_USER_CODE_OF_CONDUCT
      ),
      colors.notificationBanner.expiring_after_two_weeks,
      colors.primary,
    ],
    [
      // Showing the RT banner appears in this case, so CT is not needed
      false,
      'for CT when there is one expiring module',
      AccessTierShortNames.Controlled,
      allCompleteOneExpiring(AccessModule.DATA_USER_CODE_OF_CONDUCT),
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      true,
      'for RT when there is one expired module',
      AccessTierShortNames.Registered,
      allCompleteOneExpired(AccessModule.DATA_USER_CODE_OF_CONDUCT),
      colors.notificationBanner.expiring_within_one_week,
      colors.danger,
    ],
    [
      // Showing the RT banner appears in this case, so CT is not needed
      false,
      'for CT when there is one expired module',
      AccessTierShortNames.Controlled,
      allCompleteOneExpired(AccessModule.DATA_USER_CODE_OF_CONDUCT),
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      false,
      'for RT when only CT training is expiring',
      AccessTierShortNames.Registered,
      allCompleteOneExpiring(AccessModule.CT_COMPLIANCE_TRAINING),
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      false,
      'for RT when only CT training is expired',
      AccessTierShortNames.Registered,
      allCompleteOneExpired(AccessModule.CT_COMPLIANCE_TRAINING),
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      true,
      'for CT when only CT training is expiring',
      AccessTierShortNames.Controlled,
      allCompleteOneExpiring(AccessModule.CT_COMPLIANCE_TRAINING),
      colors.notificationBanner.expiring_within_one_week,
      colors.danger,
    ],
    [
      true,
      'for CT when only CT training is expiring in two weeks',
      AccessTierShortNames.Controlled,
      allCompleteOneExpiringInMoreThanTwoWeeks(
        AccessModule.CT_COMPLIANCE_TRAINING
      ),
      colors.notificationBanner.expiring_after_two_weeks,
      colors.primary,
    ],
    [
      true,
      'for CT when only CT training is expired',
      AccessTierShortNames.Controlled,
      allCompleteOneExpired(AccessModule.CT_COMPLIANCE_TRAINING),
      colors.notificationBanner.expiring_within_one_week,
      colors.danger,
    ],
    [
      true,
      'for RT when RT is expiring sooner',
      AccessTierShortNames.Registered,
      rtExpiresFirst,
      colors.notificationBanner.expiring_within_one_week,
      colors.danger,
    ],
    [
      // Showing the RT banner appears in this case, so CT is not needed
      false,
      'for CT when RT is expiring sooner',
      AccessTierShortNames.Controlled,
      rtExpiresFirst,
      EMPTY_STRING,
      EMPTY_STRING,
    ],
    [
      // Still need to show RT, because it's more important
      true,
      'for RT when CT is expiring sooner',
      AccessTierShortNames.Registered,
      ctExpiresFirst,
      colors.notificationBanner.expiring_within_two_weeks,
      colors.warning,
    ],
    [
      true,
      'for CT when CT is expiring sooner',
      AccessTierShortNames.Controlled,
      ctExpiresFirst,
      colors.notificationBanner.expiring_within_one_week,
      colors.danger,
    ],
  ])(
    'display=%s %s',
    async (
      expected,
      condition,
      accessTier,
      moduleStatuses,
      bannerColor,
      iconColor
    ) => {
      updateModules(moduleStatuses);

      const wrapper = component({ accessTier });
      expect(wrapper.exists()).toBeTruthy();

      const banner = wrapper.find({
        'data-test-id': 'access-renewal-notification',
      });
      expect(banner.exists()).toEqual(expected);
      if (expected) {
        expect(banner.first().props().style.backgroundColor).toBe(bannerColor);
        expect(wrapper.find(AlarmExclamation).prop('style').color).toBe(
          iconColor
        );
      }
    }
  );
});
