import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  AccessModule,
  AccessModuleStatus,
  ErrorCode,
  ProfileApi,
} from 'generated/fetch';
import { Profile } from 'generated/fetch';

import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import {
  AccessRenewalStatus,
  buildRasRedirectUrl,
  computeRenewalDisplayDates,
  getTwoFactorSetupUrl,
  maybeDaysRemaining,
  NOTIFICATION_THRESHOLD_DAYS,
  RAS_CALLBACK_PATH,
  useIsUserDisabled,
} from 'app/utils/access-utils';
import {
  displayDateWithoutHours,
  nowPlusDays,
  plusDays,
} from 'app/utils/dates';
import { authStore, profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOnTimersAndUpdate } from 'testing/react-test-helpers';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';

const ONE_MINUTE_IN_MILLIS = 1000 * 60;

describe('maybeDaysRemaining', () => {
  it('returns undefined when the profile has no accessModules', () => {
    const noModules: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {},
    };
    expect(maybeDaysRemaining(noModules)).toBeUndefined();
  });

  it('returns undefined when the profile has no expirable accessModules', () => {
    const noExpirableModules: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.RASLINKLOGINGOV,
            expirationEpochMillis: undefined,
          },
        ],
      },
    };

    expect(maybeDaysRemaining(noExpirableModules)).toBeUndefined();
  });

  it('returns undefined when the profile has no expirable accessModules with expirations', () => {
    const noExpirations: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCETRAINING,
            expirationEpochMillis: undefined,
          },
        ],
      },
    };

    expect(maybeDaysRemaining(noExpirations)).toBeUndefined();
  });

  it('returns undefined when the accessModules have expirations past the window', () => {
    const laterExpiration: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCETRAINING,
            expirationEpochMillis:
              nowPlusDays(NOTIFICATION_THRESHOLD_DAYS + 1) +
              ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    expect(maybeDaysRemaining(laterExpiration)).toBeUndefined();
  });

  it('returns the soonest of all expirations within the window', () => {
    const soonDays = 5;
    const aBitLater = 10;

    const expirationsInWindow: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCETRAINING,
            expirationEpochMillis: nowPlusDays(soonDays) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
            expirationEpochMillis:
              nowPlusDays(aBitLater) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    expect(maybeDaysRemaining(expirationsInWindow)).toEqual(soonDays);
  });

  // regression test for RW-7108
  it('returns 30 days when the max expiration is between 30 and 31 days', () => {
    const thirtyDaysPlusExpiration: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCETRAINING,
            expirationEpochMillis: nowPlusDays(30) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
            expirationEpochMillis: nowPlusDays(31) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    expect(maybeDaysRemaining(thirtyDaysPlusExpiration)).toEqual(30);
  });
});

describe('computeRenewalDisplayDates', () => {
  const EXPIRATION_DAYS = 123; // arbitrary for testing; actual prod value is 365
  const LOOKBACK_PERIOD = 99; // arbitrary for testing; actual prod value is 330

  beforeEach(() => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        accessRenewalLookback: LOOKBACK_PERIOD,
      },
    });
  });

  it('returns Unavailable/Incomplete when the module is incomplete', () => {
    expect(computeRenewalDisplayDates({})).toStrictEqual({
      lastConfirmedDate: 'Unavailable (not completed)',
      nextReviewDate: 'Unavailable (not completed)',
      moduleStatus: AccessRenewalStatus.INCOMPLETE,
    });
  });

  it('returns Unavailable/Incomplete when the module is undefined', () => {
    expect(computeRenewalDisplayDates(undefined)).toStrictEqual({
      lastConfirmedDate: 'Unavailable (not completed)',
      nextReviewDate: 'Unavailable (not completed)',
      moduleStatus: AccessRenewalStatus.INCOMPLETE,
    });
  });

  it('returns Unavailable/Incomplete when the module is null', () => {
    expect(computeRenewalDisplayDates(null)).toStrictEqual({
      lastConfirmedDate: 'Unavailable (not completed)',
      nextReviewDate: 'Unavailable (not completed)',
      moduleStatus: AccessRenewalStatus.INCOMPLETE,
    });
  });

  it('returns Unavailable/Incomplete when the module is incomplete, regardless of expiration date', () => {
    const expirationDate = nowPlusDays(25);
    const status: AccessModuleStatus = {
      expirationEpochMillis: expirationDate,
    };

    expect(computeRenewalDisplayDates(status)).toStrictEqual({
      lastConfirmedDate: 'Unavailable (not completed)',
      nextReviewDate: 'Unavailable (not completed)',
      moduleStatus: AccessRenewalStatus.INCOMPLETE,
    });
  });

  it('returns Unavailable/Bypassed when the module is bypassed, regardless of bypass date', () => {
    const bypassDate = nowPlusDays(-10000);
    const status: AccessModuleStatus = {
      bypassEpochMillis: bypassDate,
    };

    expect(computeRenewalDisplayDates(status)).toStrictEqual({
      lastConfirmedDate: displayDateWithoutHours(bypassDate),
      nextReviewDate: 'Unavailable (bypassed)',
      moduleStatus: AccessRenewalStatus.BYPASSED,
    });
  });

  it('returns Unavailable/Bypassed when the module is bypassed, regardless of other fields', () => {
    const bypassDate = nowPlusDays(-15);

    // completed more recently than bypassed
    const completionDate = nowPlusDays(-5);
    // will expire well into the future
    const expirationDate = nowPlusDays(200);

    const status: AccessModuleStatus = {
      bypassEpochMillis: bypassDate,
      completionEpochMillis: completionDate,
      expirationEpochMillis: expirationDate,
    };

    expect(computeRenewalDisplayDates(status)).toStrictEqual({
      lastConfirmedDate: displayDateWithoutHours(bypassDate),
      nextReviewDate: 'Unavailable (bypassed)',
      moduleStatus: AccessRenewalStatus.BYPASSED,
    });
  });

  it('returns valid completion and renewal times in the near future (within lookback)', () => {
    const completionDaysPast = 44; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date within the lookback
    const endOfLookback = nowPlusDays(LOOKBACK_PERIOD);
    expect(expirationDate).toBeLessThan(endOfLookback);

    const status: AccessModuleStatus = {
      completionEpochMillis: completionDate,
      expirationEpochMillis: expirationDate,
    };

    expect(computeRenewalDisplayDates(status)).toStrictEqual({
      lastConfirmedDate: displayDateWithoutHours(completionDate),
      nextReviewDate: `${displayDateWithoutHours(expirationDate)} (${
        EXPIRATION_DAYS - completionDaysPast
      } days)`,
      moduleStatus: AccessRenewalStatus.EXPIRING_SOON,
    });
  });

  it('returns valid completion and renewal times in the far future (beyond lookback)', () => {
    const completionDaysPast = 3; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date past the lookback
    const endOfLookback = nowPlusDays(LOOKBACK_PERIOD);
    expect(expirationDate).toBeGreaterThan(endOfLookback);

    const status: AccessModuleStatus = {
      completionEpochMillis: completionDate,
      expirationEpochMillis: expirationDate,
    };

    expect(computeRenewalDisplayDates(status)).toStrictEqual({
      lastConfirmedDate: displayDateWithoutHours(completionDate),
      nextReviewDate: `${displayDateWithoutHours(expirationDate)} (${
        EXPIRATION_DAYS - completionDaysPast
      } days)`,
      moduleStatus: AccessRenewalStatus.CURRENT,
    });
  });

  it('returns a valid completion time and an expired renewal time', () => {
    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-(EXPIRATION_DAYS + 1)) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    const status: AccessModuleStatus = {
      completionEpochMillis: completionDate,
      expirationEpochMillis: expirationDate,
    };

    expect(computeRenewalDisplayDates(status)).toStrictEqual({
      lastConfirmedDate: displayDateWithoutHours(completionDate),
      nextReviewDate: `${displayDateWithoutHours(expirationDate)} (expired)`,
      moduleStatus: AccessRenewalStatus.EXPIRED,
    });
  });
});

describe('useIsUserDisabled', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const HookConsumer = () => {
    const isUserDisabled = useIsUserDisabled();
    return <div data-disabled={isUserDisabled} />;
  };

  const component = async () => {
    const wrapper = mount(<HookConsumer />);
    await waitOnTimersAndUpdate(wrapper);
    return wrapper;
  };

  const simulateSignIn = async (wrapper: ReactWrapper, isSignedIn: boolean) => {
    authStore.set({ authLoaded: true, isSignedIn });
    await waitOnTimersAndUpdate(wrapper);
  };

  const getDisabled = (wrapper: ReactWrapper) => {
    return wrapper.childAt(0).prop('data-disabled');
  };

  beforeEach(() => {
    authStore.set({ authLoaded: false, isSignedIn: false });
    profileStore.set({ load, reload, updateCache });
  });

  it('is undefined while loading', async () => {
    const wrapper = await component();

    expect(getDisabled(wrapper)).toBe(undefined);
  });

  it('is not disabled for unauthenticated user', async () => {
    const wrapper = await component();

    await simulateSignIn(wrapper, false);
    expect(getDisabled(wrapper)).toBe(false);
  });

  it('is undefined during profile load', async () => {
    // profile load blocks forever
    load.mockImplementation(() => new Promise(() => {}));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(undefined);
    expect(load).toHaveBeenCalled();
  });

  it('is not disabled on profile load', async () => {
    // profile load finishes immediately
    load.mockImplementation(() => Promise.resolve({}));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(false);
  });

  it('is disabled on profile disabled error', async () => {
    // profile load fails
    load.mockImplementation(() =>
      Promise.reject(
        new Response(
          JSON.stringify({
            errorCode: ErrorCode.USERDISABLED,
          })
        )
      )
    );

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(true);
  });

  it('is undefined on other profile errors', async () => {
    // profile load fails with an unknown error
    load.mockImplementation(() => Promise.reject({}));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(undefined);
  });
});

describe('getTwoFactorSetupUrl', () => {
  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });
  });

  it('should generate expected 2FA redirect URL', () => {
    expect(getTwoFactorSetupUrl()).toMatch(
      /https:\/\/accounts\.google\.com\/AccountChooser/
    );
    expect(getTwoFactorSetupUrl()).toMatch(
      encodeURIComponent('tester@fake-research-aou.org')
    );
    expect(getTwoFactorSetupUrl()).toMatch(
      encodeURIComponent('https://myaccount.google.com/signinoptions/')
    );
  });
});

describe('buildRasRedirectUrl', () => {
  it('should generate expected RAS redirect URL', () => {
    expect(buildRasRedirectUrl()).toMatch(
      encodeURIComponent('http://localhost' + RAS_CALLBACK_PATH)
    );
  });
});
