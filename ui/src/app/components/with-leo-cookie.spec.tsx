import * as React from 'react';
import { mount } from 'enzyme';

import { withLeoCookie } from 'app/components/with-leo-cookie';
import {
  leoProxyApi,
  registerApiClient as leoRegisterApiClient,
} from 'app/services/notebooks-swagger-fetch-clients';
import { authStore, notificationStore } from 'app/utils/stores';
import { ProxyApi } from 'notebooks-generated/fetch';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { LeoProxyApiStub } from 'testing/stubs/leo-proxy-api-stub';

const createWrapper = () => {
  const Component = () => <div />;
  const WrappedComponent = withLeoCookie(Component);
  return mount(<WrappedComponent />);
};

describe(withLeoCookie.name, () => {
  beforeEach(() => {
    jest.useFakeTimers();

    const leoProxyApiStub = new LeoProxyApiStub();
    leoRegisterApiClient(ProxyApi, leoProxyApiStub);

    authStore.set({
      authLoaded: true,
      isSignedIn: true,
      auth: {
        // @ts-ignore
        settings: {
          accessTokenExpiringNotificationTimeInSeconds: 500,
        },
      },
    });
    notificationStore.set(null);
  });

  it('should render the children once the cookie loads', async () => {
    const setCookieStub = jest
      .spyOn(leoProxyApi(), 'setCookie')
      .mockResolvedValue(null);

    const Component = () => <div />;
    const WrappedComponent = withLeoCookie(Component);
    const wrapper = mount(<WrappedComponent />);

    expect(setCookieStub).toHaveBeenCalled();

    // Children should not be rendered until the cookie loads
    expect(wrapper.find(Component).exists()).toBeFalsy();

    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find(Component).exists()).toBeTruthy();
  });

  it('should show an error notification if the cookie fails to load', async () => {
    const setCookieStub = jest
      .spyOn(leoProxyApi(), 'setCookie')
      .mockRejectedValue(null);

    const wrapper = createWrapper();

    expect(setCookieStub).toHaveBeenCalled();

    // Notification should not be shown until the cookie loads
    expect(notificationStore.get()).toBeNull();

    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get()).not.toBeNull();
  });

  it('should not show an error notification if the cookie loads successfully', async () => {
    const setCookieStub = jest
      .spyOn(leoProxyApi(), 'setCookie')
      .mockResolvedValue(null);

    const wrapper = createWrapper();

    expect(setCookieStub).toHaveBeenCalled();

    // Notification should not be shown until the cookie loads
    expect(notificationStore.get()).toBeNull();

    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get()).toBeNull();
  });

  it('should periodically refresh the cookie', async () => {
    const authRefreshPeriodSeconds = 500;
    const expectedCookieRefreshPeriodMs =
      (authRefreshPeriodSeconds - 30) * 1000;

    authStore.set({
      ...authStore.get(),
      auth: {
        ...authStore.get().auth,
        settings: {
          ...authStore.get().auth.settings,
          accessTokenExpiringNotificationTimeInSeconds:
            authRefreshPeriodSeconds,
        },
      },
    });

    const setCookieStub = jest
      .spyOn(leoProxyApi(), 'setCookie')
      .mockResolvedValue(null);

    const wrapper = createWrapper();

    expect(setCookieStub).toHaveBeenCalledTimes(1);

    // check that we don't immediately refresh the cookie
    jest.advanceTimersByTime(expectedCookieRefreshPeriodMs - 1);
    await waitOneTickAndUpdate(wrapper);
    expect(setCookieStub).toHaveBeenCalledTimes(1);

    // the cookie refreshes after the expected refresh period
    jest.advanceTimersByTime(1);
    await waitOneTickAndUpdate(wrapper);
    expect(setCookieStub).toHaveBeenCalledTimes(2);

    // the cookie refreshes again after the expected refresh period
    jest.advanceTimersByTime(expectedCookieRefreshPeriodMs);
    await waitOneTickAndUpdate(wrapper);
    expect(setCookieStub).toHaveBeenCalledTimes(3);
  });

  it('should stop periodically refreshing the cookie after unmounting', async () => {
    const authRefreshPeriodSeconds = 500;
    const expectedCookieRefreshPeriodMs =
      (authRefreshPeriodSeconds - 30) * 1000;

    authStore.set({
      ...authStore.get(),
      auth: {
        ...authStore.get().auth,
        settings: {
          ...authStore.get().auth.settings,
          accessTokenExpiringNotificationTimeInSeconds:
            authRefreshPeriodSeconds,
        },
      },
    });

    const setCookieStub = jest
      .spyOn(leoProxyApi(), 'setCookie')
      .mockResolvedValue(null);

    const wrapper = createWrapper();

    expect(setCookieStub).toHaveBeenCalledTimes(1);

    wrapper.unmount();

    // the cookie does not refresh again
    jest.advanceTimersByTime(expectedCookieRefreshPeriodMs);
    await waitOneTickAndUpdate(wrapper);
    expect(setCookieStub).toHaveBeenCalledTimes(1);
  });
});
