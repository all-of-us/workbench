import { mount } from 'enzyme';
import * as React from 'react';

import { BugReportModal } from './bug-report';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { ProfileApi } from 'generated/fetch';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { profileStore } from 'app/utils/stores';

describe('BugReport', () => {
  const description = 'test';
  const profile = ProfileStubVariables.PROFILE_STUB;
  let profileApi: ProfileApiStub;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = () => {
    return mount(
      <BugReportModal bugReportDescription={description} onClose={() => {}} />
    );
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });

    profileStore.set({ profile, load, reload, updateCache });
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });
});
