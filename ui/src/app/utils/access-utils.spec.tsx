import * as React from "react";
import {mount, ReactWrapper} from 'enzyme';
import {waitOnTimersAndUpdate} from 'testing/react-test-helpers';

import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {authStore, profileStore} from 'app/utils/stores';
import {ErrorCode} from 'generated/fetch';
import {Profile, RenewableAccessModuleStatus} from 'generated/fetch';
import {
  maybeDaysRemaining,
  MILLIS_PER_DAY,
  NOTIFICATION_THRESHOLD_DAYS,
  useIsUserDisabled
} from "app/utils/access-utils";
import ModuleNameEnum = RenewableAccessModuleStatus.ModuleNameEnum;

// 10 minutes, in millis
const SHORT_TIME_BUFFER = 10 * 60 * 1000;

// return a time (in epoch millis) which is today + `days` days, plus a short buffer
const todayPlusDays = (days: number): number => {
  return Date.now() + (MILLIS_PER_DAY * days) + SHORT_TIME_BUFFER ;
}

const noModules: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  renewableAccessModules: {}
}

const noExpModules: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  renewableAccessModules: {
    modules: [{
      moduleName: ModuleNameEnum.ComplianceTraining,
      hasExpired: false,
      expirationEpochMillis: undefined,
    }]
  }
}

const laterExpiration: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  renewableAccessModules: {
    modules: [{
      moduleName: ModuleNameEnum.ComplianceTraining,
      hasExpired: false,
      expirationEpochMillis: todayPlusDays(NOTIFICATION_THRESHOLD_DAYS + 1),
    }]
  }
}

const expirationsInWindow: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  renewableAccessModules: {
    modules: [{
      moduleName: ModuleNameEnum.ComplianceTraining,
      hasExpired: false,
      expirationEpochMillis: todayPlusDays(5),
    },
    {
      moduleName: ModuleNameEnum.DataUseAgreement,
      hasExpired: false,
      expirationEpochMillis: todayPlusDays(10),
    }]
  }
}

const thirtyDaysPlusExpiration: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  renewableAccessModules: {
    modules: [{
      moduleName: ModuleNameEnum.ComplianceTraining,
      hasExpired: false,
      expirationEpochMillis: todayPlusDays(30),
    },
    {
      moduleName: ModuleNameEnum.DataUseAgreement,
      hasExpired: false,
      expirationEpochMillis: todayPlusDays(31),
    }]
  }
}

describe('maybeDaysRemaining', () => {
  it('returns undefined when the profile has no renewableAccessModules', () => {
     expect(maybeDaysRemaining(noModules)).toBeUndefined();
  });

  it('returns undefined when the profile has no renewableAccessModules with expirations', () => {
     expect(maybeDaysRemaining(noExpModules)).toBeUndefined();
  });

  it('returns undefined when the renewableAccessModules have expirations past the window', () => {
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
    authStore.set({authLoaded: true, isSignedIn, enableInactivityTimeout: true});
    await waitOnTimersAndUpdate(wrapper);
  };

  const getDisabled = (wrapper: ReactWrapper) => {
    return wrapper.childAt(0).prop('data-disabled');
  };

  beforeEach(() => {
    authStore.set({authLoaded: false, isSignedIn: false, enableInactivityTimeout: false});
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
