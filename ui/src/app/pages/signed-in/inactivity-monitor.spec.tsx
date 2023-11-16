import * as React from 'react';
import { mount } from 'enzyme';

import { environment } from 'environments/environment';
import {
  INACTIVITY_CONFIG,
  InactivityMonitor,
} from 'app/pages/signed-in/inactivity-monitor';
import * as Authentication from 'app/utils/authentication';
import { authStore, notificationStore } from 'app/utils/stores';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';

describe(InactivityMonitor.name, () => {
  it('should show an error when signout fails', async () => {
    const signOutSpy = jest.spyOn(Authentication, 'signOut');
    signOutSpy.mockImplementation(() => {
      throw new Error();
    });
    authStore.set({
      authLoaded: true,
      isSignedIn: true,
    });

    window.localStorage.setItem(
      INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE,
      String(Date.now() - environment.inactivityTimeoutSeconds * 1000 - 1)
    );

    expect(notificationStore.get()).toBeNull();
    const wrapper = mount(<InactivityMonitor />);
    await waitOneTickAndUpdate(wrapper);
    expect(notificationStore.get()).toBeTruthy();
  });
});
