import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {Profile, ProfileApi} from 'generated/fetch';
import * as React from 'react';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

import {BugReportModal} from './bug-report';

describe('BugReport', () => {
  const description = 'test';
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = () => {
    return mount(<BugReportModal
        bugReportDescription={description}
        onClose={() => {}}
    />);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      profileStore.set({profile: newProfile, load, reload, updateCache});
    });

    profileStore.set({profile, load, reload, updateCache});
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

});
