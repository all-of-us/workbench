import {mount} from 'enzyme';
import * as React from 'react';

import {BugReportModal} from './bug-report';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {Profile, ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {userProfileStore} from 'app/utils/navigation';


describe('BugReport', () => {
  const description = 'test';
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
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
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

});
