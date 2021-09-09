import * as React from 'react';
import {mount, ReactWrapper} from 'enzyme';

import {AccessModule, ErrorCode, ProfileApi} from 'generated/fetch';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {Profile} from 'generated/fetch';
import {authStore, profileStore} from 'app/utils/stores';
import {waitOnTimersAndUpdate} from 'testing/react-test-helpers';
import {
  buildRasRedirectUrl,
  getTwoFactorSetupUrl,
  maybeDaysRemaining,
  MILLIS_PER_DAY,
  NOTIFICATION_THRESHOLD_DAYS,
  RAS_CALLBACK_PATH,
  useIsUserDisabled
} from 'app/utils/access-utils';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';

// 10 minutes, in millis
const SHORT_TIME_BUFFER = 10 * 60 * 1000;

// return a time (in epoch millis) which is today + `days` days, plus a short buffer
const todayPlusDays = (days: number): number => {
  return Date.now() + (MILLIS_PER_DAY * days) + SHORT_TIME_BUFFER ;
};

const noModules: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  accessModules: {}
};

const noExpirableModules: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  accessModules : {
    modules: [{
      moduleName: AccessModule.RASLINKLOGINGOV,
      expirationEpochMillis: undefined,
    }]
  }
};

const noExpModules: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  accessModules : {
    modules: [{
      moduleName: AccessModule.COMPLIANCETRAINING,
      expirationEpochMillis: undefined,
    }]
  }
};

const laterExpiration: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  accessModules : {
    modules: [{
      moduleName: AccessModule.COMPLIANCETRAINING,
      expirationEpochMillis: todayPlusDays(NOTIFICATION_THRESHOLD_DAYS + 1),
    }]
  }
};

const expirationsInWindow: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  accessModules : {
    modules: [{
      moduleName: AccessModule.COMPLIANCETRAINING,
      expirationEpochMillis: todayPlusDays(5),
    }, {
      moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
      expirationEpochMillis: todayPlusDays(10),
    }]
  }
};

const thirtyDaysPlusExpiration: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  accessModules : {
    modules: [{
      moduleName: AccessModule.COMPLIANCETRAINING,
      expirationEpochMillis: todayPlusDays(30),
    }, {
      moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
      expirationEpochMillis: todayPlusDays(31),
    }]
  }
};

describe('maybeDaysRemaining', () => {
  it('returns undefined when the profile has no accessModules', () => {
     expect(maybeDaysRemaining(noModules)).toBeUndefined();
  });

  it('returns undefined when the profile has no expirable accessModules', () => {
    expect(maybeDaysRemaining(noExpirableModules)).toBeUndefined();
  });

  it('returns undefined when the profile has no accessModules with expirations', () => {
     expect(maybeDaysRemaining(noExpModules)).toBeUndefined();
  });

  it('returns undefined when the accessModules have expirations past the window', () => {
    expect(maybeDaysRemaining(laterExpiration)).toBeUndefined();
  });

  it('returns the soonest of all expirations within the window', () => {
    expect(maybeDaysRemaining(expirationsInWindow)).toEqual(5);
  });

  // regression test for RW-7108
  it('returns 30 days when the max expiration is between 30 and 31 days', () => {
    expect(maybeDaysRemaining(thirtyDaysPlusExpiration)).toEqual(30);
  });
});

describe('useIsUserDisabled', () => {

  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const HookConsumer = () => {
    const isUserDisabled = useIsUserDisabled();
    return <div data-disabled={isUserDisabled}></div>;
  };

  const component = async() => {
    const wrapper = mount(<HookConsumer/>);
    await waitOnTimersAndUpdate(wrapper);
    return wrapper;
  };

  const simulateSignIn = async(wrapper: ReactWrapper, isSignedIn: boolean) => {
    authStore.set({authLoaded: true, isSignedIn});
    await waitOnTimersAndUpdate(wrapper);
  };

  const getDisabled = (wrapper: ReactWrapper) => {
    return wrapper.childAt(0).prop('data-disabled');
  };

  beforeEach(() => {
    authStore.set({authLoaded: false, isSignedIn: false});
    profileStore.set({load, reload, updateCache});
  });

  it('is undefined while loading', async() => {
    const wrapper = await component();

    expect(getDisabled(wrapper)).toBe(undefined);
  });

  it('is not disabled for unauthenticated user', async() => {
    const wrapper = await component();

    await simulateSignIn(wrapper, false);
    expect(getDisabled(wrapper)).toBe(false);
  });

  it('is undefined during profile load', async() => {
    // profile load blocks forever
    load.mockImplementation(() => new Promise(() => {}));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(undefined);
    expect(load).toHaveBeenCalled();
  });

  it('is not disabled on profile load', async() => {
    // profile load finishes immediately
    load.mockImplementation(() => Promise.resolve({}));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(false);
  });

  it('is disabled on profile disabled error', async() => {
    // profile load fails
    load.mockImplementation(() => Promise.reject(new Response(JSON.stringify({
      errorCode: ErrorCode.USERDISABLED
    }))));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(true);
  });

  it('is undefined on other profile errors', async() => {
    // profile load fails with an unknown error
    load.mockImplementation(() => Promise.reject({}));

    const wrapper = await component();
    await simulateSignIn(wrapper, true);

    expect(getDisabled(wrapper)).toBe(undefined);
  });
});

describe('getTwoFactorSetupUrl', () => {
  beforeEach(async() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn()
    });
  });

  it('should generate expected 2FA redirect URL', () => {
    expect(getTwoFactorSetupUrl()).toMatch(/https:\/\/accounts\.google\.com\/AccountChooser/);
    expect(getTwoFactorSetupUrl()).toMatch(encodeURIComponent('tester@fake-research-aou.org'));
    expect(getTwoFactorSetupUrl()).toMatch(encodeURIComponent('https://myaccount.google.com/signinoptions/'));
  });
});

describe('buildRasRedirectUrl', () => {
  it('should generate expected RAS redirect URL', () => {
    expect(buildRasRedirectUrl()).toMatch(encodeURIComponent('http://localhost' + RAS_CALLBACK_PATH));
  });
});

