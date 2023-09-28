import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  AccessModule,
  AccessModuleStatus,
  ErrorCode,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import {
  AccessRenewalStatus,
  buildRasRedirectUrl,
  computeRenewalDisplayDates,
  getTwoFactorSetupUrl,
  hasExpired,
  isExpiringOrExpired,
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

import { AccessTierShortNames } from './access-tiers';

const ONE_MINUTE_IN_MILLIS = 1000 * 60;
const arbitraryModuleName = AccessModule.PUBLICATION_CONFIRMATION;

describe('maybeDaysRemaining', () => {
  beforeEach(() => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

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
            moduleName: AccessModule.RAS_LINK_LOGIN_GOV,
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
            moduleName: AccessModule.COMPLIANCE_TRAINING,
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
            moduleName: AccessModule.COMPLIANCE_TRAINING,
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
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            expirationEpochMillis: nowPlusDays(soonDays) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.DATA_USER_CODE_OF_CONDUCT,
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
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            expirationEpochMillis: nowPlusDays(30) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.DATA_USER_CODE_OF_CONDUCT,
            expirationEpochMillis: nowPlusDays(31) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    expect(maybeDaysRemaining(thirtyDaysPlusExpiration)).toEqual(30);
  });

  // RW-9550
  it('is not affected by Controlled Tier training by default', () => {
    const ctExpected = 5;
    const rtExpected = 10;

    const expirationsInWindow: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(rtExpected) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(ctExpected) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    // even though CT expires sooner
    expect(maybeDaysRemaining(expirationsInWindow)).toEqual(rtExpected);
  });

  // RW-9550
  it('is not affected by Controlled Tier training when RT is requested explicitly', () => {
    const ctExpected = 5;
    const rtExpected = 10;

    const expirationsInWindow: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(rtExpected) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(ctExpected) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    // even though CT expires sooner
    expect(
      maybeDaysRemaining(expirationsInWindow, AccessTierShortNames.Registered)
    ).toEqual(rtExpected);
  });

  // RW-9546
  it('returns the earliest Controlled Tier expiration when CT is requested', () => {
    const ctExpected = 5;
    const rtExpected = 10;

    const expirationsInWindow: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(rtExpected) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(ctExpected) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    expect(
      maybeDaysRemaining(expirationsInWindow, AccessTierShortNames.Controlled)
    ).toEqual(ctExpected);
  });

  // RW-9546
  it('includes RT expirations in the CT earliest-expiration calculation', () => {
    const rtExpires = 5;
    const ctExpires = 10;
    const expected = 5;

    const expirationsInWindow: Profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      accessModules: {
        modules: [
          {
            moduleName: AccessModule.COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(rtExpires) + ONE_MINUTE_IN_MILLIS,
          },
          {
            moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
            expirationEpochMillis:
              nowPlusDays(ctExpires) + ONE_MINUTE_IN_MILLIS,
          },
        ],
      },
    };

    expect(
      maybeDaysRemaining(expirationsInWindow, AccessTierShortNames.Controlled)
    ).toEqual(expected);
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
    expect(
      computeRenewalDisplayDates({
        moduleName: arbitraryModuleName,
      })
    ).toStrictEqual({
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
      moduleName: arbitraryModuleName,
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
      moduleName: arbitraryModuleName,
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
      moduleName: arbitraryModuleName,
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
      moduleName: arbitraryModuleName,
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
      moduleName: arbitraryModuleName,
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
      moduleName: arbitraryModuleName,
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
            errorCode: ErrorCode.USER_DISABLED,
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

describe('hasExpired', () => {
  it('should return hasExpired=true for a date in the past', () => {
    const testTime = nowPlusDays(-10);
    expect(hasExpired(testTime)).toBeTruthy();
  });

  it('should return hasExpired=false for a date in the future', () => {
    const testTime = nowPlusDays(10);
    expect(hasExpired(testTime)).toBeFalsy();
  });

  it('should return hasExpired=false for a null date', () => {
    expect(hasExpired(null)).toBeFalsy();
  });

  it('should return hasExpired=false for an undefined date', () => {
    expect(hasExpired(undefined)).toBeFalsy();
  });
});

describe('isExpiringOrExpired', () => {
  const LOOKBACK_PERIOD = 99; // arbitrary for testing; actual prod value is 330
  const TRAINING_LOOKBACK_PERIOD = 20; // arbitrary for testing; actual prod value is 30
  const EXPIRATION_DAYS = 123; // arbitrary for testing; actual prod value is 365

  beforeEach(() => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        accessRenewalLookback: LOOKBACK_PERIOD,
        complianceTrainingRenewalLookback: TRAINING_LOOKBACK_PERIOD,
      },
    });
  });

  it('should return isExpiringOrExpired=true if a module has expired', () => {
    const completionDaysPast = 555; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking an expiration in the past
    expect(expirationDate).toBeLessThan(Date.now());

    expect(
      isExpiringOrExpired(expirationDate, AccessModule.PUBLICATION_CONFIRMATION)
    ).toEqual(true);
  });

  it('should return isExpiringOrExpired=true if a module will expire in the future and within the lookback period', () => {
    const completionDaysPast = 44; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date within the lookback
    const endOfLookback = nowPlusDays(LOOKBACK_PERIOD);
    expect(expirationDate).toBeLessThan(endOfLookback);

    expect(
      isExpiringOrExpired(expirationDate, AccessModule.PUBLICATION_CONFIRMATION)
    ).toEqual(true);
  });

  it('should return isExpiringOrExpired=false if a module will expire in the future beyond the lookback period', () => {
    const completionDaysPast = 3; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date beyond the lookback
    const endOfLookback = nowPlusDays(LOOKBACK_PERIOD);
    expect(expirationDate).toBeGreaterThan(endOfLookback);

    expect(
      isExpiringOrExpired(expirationDate, AccessModule.PUBLICATION_CONFIRMATION)
    ).toEqual(false);
  });

  it('should return isExpiringOrExpired=true if a training module will expire in the future and within the lookback period', () => {
    const completionDaysPast = 120; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date within the lookback
    const endOfLookback = nowPlusDays(TRAINING_LOOKBACK_PERIOD);
    expect(expirationDate).toBeLessThan(endOfLookback);

    expect(
      isExpiringOrExpired(expirationDate, AccessModule.CT_COMPLIANCE_TRAINING)
    ).toEqual(true);
  });

  it('should return isExpiringOrExpired=false if a training module will expire in the future beyond the lookback period', () => {
    // Verifying that training modules are not considered to be expiring in the traditional lookback period.
    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate = nowPlusDays(-LOOKBACK_PERIOD) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date beyond the lookback
    const endOfLookback = nowPlusDays(TRAINING_LOOKBACK_PERIOD);
    expect(expirationDate).toBeGreaterThan(endOfLookback);

    expect(
      isExpiringOrExpired(expirationDate, AccessModule.CT_COMPLIANCE_TRAINING)
    ).toEqual(false);
  });

  it('should return isExpiringOrExpired=false if a module has a null expiration', () => {
    expect(
      isExpiringOrExpired(null, AccessModule.PUBLICATION_CONFIRMATION)
    ).toEqual(false);
  });

  it('should return isExpiringOrExpired=false if a module has an undefined expiration', () => {
    expect(
      isExpiringOrExpired(undefined, AccessModule.PUBLICATION_CONFIRMATION)
    ).toEqual(false);
  });
});
