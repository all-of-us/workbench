import * as React from 'react';

import { environment } from 'environments/environment';
import { render, waitFor } from '@testing-library/react';
import { InactivityMonitor } from 'app/pages/signed-in/inactivity-monitor';
import * as Authentication from 'app/utils/authentication';
import { setLastActive } from 'app/utils/inactivity';
import { authStore, notificationStore, profileStore } from 'app/utils/stores';

import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

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
    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });

    setLastActive(
      Date.now() - environment.inactivityTimeoutSecondsRt * 1000 - 1
    );

    expect(notificationStore.get()).toBeNull();
    render(<InactivityMonitor />);
    await waitFor(() => expect(notificationStore.get()).toBeTruthy());
  });
});
