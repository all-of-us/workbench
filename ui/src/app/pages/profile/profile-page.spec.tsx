import {mount} from 'enzyme';
import * as React from 'react';

import {TextInput} from 'app/components/inputs';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {Profile, ProfileApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {ProfilePage} from './profile-page';


describe('ProfilePageComponent', () => {

  const profile = ProfileStubVariables.PROFILE_STUB;

  const component = () => {
    return mount(<ProfilePage/>);
  };

  const reload = jest.fn();
  const updateCache = jest.fn();

  beforeEach(() => {
    const profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
    serverConfigStore.next({
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    });
  });

  it('should render the profile', () => {
    const wrapper = component();
    expect(wrapper.find(TextInput).first().prop('value')).toMatch(profile.givenName);
  });

  it('should save correctly', async () => {
    const wrapper = component();
    expect(userProfileStore.getValue().profile.givenName).toEqual(profile.givenName);

    wrapper.find(TextInput).first().simulate('change', {target: {value: 'x'}});
    wrapper.find('[data-test-id="save profile"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(reload).toHaveBeenCalled();
  });

  it('should invalidate inputs correctly', () => {
    const wrapper = component();
    wrapper.find(TextInput).first().simulate('change', {target: {value: ''}});
    expect(wrapper.find(TextInput).first().prop('invalid')).toBeTruthy();
  });

});
