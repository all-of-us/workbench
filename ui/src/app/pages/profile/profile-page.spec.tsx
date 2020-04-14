import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import {TextInput} from 'app/components/inputs';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {ProfileApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {ProfilePage} from 'app/pages/profile/profile-page';
import SpyInstance = jest.SpyInstance;


describe('ProfilePageComponent', () => {
  function getSubmitButton(wrapper: ReactWrapper): ReactWrapper {
    return wrapper.find('[data-test-id="submit-button"]');
  }

  function getDemographicSurveyButton(wrapper: ReactWrapper): ReactWrapper {
    return wrapper.find('[data-test-id="demographics-survey-button"]')
  }

  function getSaveProfileButton(wrapper: ReactWrapper): ReactWrapper {
    return wrapper.find('[data-test-id="save_profile"]');
  }
  function clickSaveProfileButton(wrapper: ReactWrapper) {
    const saveProfileButton = getSaveProfileButton(wrapper);
    saveProfileButton.first().simulate('click');
    return;
  }

  const profile = ProfileStubVariables.PROFILE_STUB;

  let mockUpdateProfile: SpyInstance;

  const component = () => {
    return mount(<ProfilePage/>);
  };

  const reload = jest.fn();
  const updateCache = jest.fn();

  beforeEach(() => {
    const profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    mockUpdateProfile = jest.spyOn(profileApi, 'updateProfile');

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
    clickSaveProfileButton(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(reload).toHaveBeenCalled();
  });

  it('should invalidate inputs correctly', () => {
    const wrapper = component();
    wrapper.find(TextInput).first().simulate('change', {target: {value: ''}});
    expect(wrapper.find(TextInput).first().prop('invalid')).toBeTruthy();
  });

  it('should handle error when creating an account', async() => {
    const errorResponseJson = {
      message: 'Could not create account: invalid institutional affiliation',
      statusCode: 412
    };
    mockUpdateProfile.mockRejectedValueOnce(
        new Response(JSON.stringify(errorResponseJson), {status: 412}));

    const wrapper = component();

    getDemographicSurveyButton(wrapper).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    getSubmitButton(wrapper).simulate('click');
    // We need to await one tick to allow async processing of the error response to resolve.
    await waitOneTickAndUpdate(wrapper);

    const errorModal = wrapper.find('Modal[data-test-id="update-profile-error"]');
    // Ensure the error modal contains explanatory intro text.
    expect(errorModal.getDOMNode().textContent).toContain('An error occurred while updating your profile');
    // Ensure the error modal contains the server-side error message.
    expect(errorModal.getDOMNode().textContent).toContain('Could not create account: invalid institutional affiliation');
  });

  it('should display/update address', async() => {

    const wrapper = component();

    let streetAddress1 = wrapper.find('[data-test-id="streetAddress1"]').first();
    console.log(streetAddress1.props());
    expect(streetAddress1.prop('value')).toBe('Main street');

    streetAddress1.simulate('change', {target: {value: 'Broadway'}});

    clickSaveProfileButton(wrapper);
    await waitOneTickAndUpdate(wrapper);
    streetAddress1 = wrapper.find('[data-test-id="streetAddress1"]').first();

    expect(getSaveProfileButton(wrapper).first().prop('disabled')).toBe(true);
    expect(streetAddress1.prop('value')).toBe('Broadway');

  });

  it('should throw error if street Address 1 is empty', async() => {

    const wrapper = component();

    let streetAddress1 = wrapper.find('[data-test-id="streetAddress1"]').first();
    expect(streetAddress1.prop('value')).toBe('Main street');

    streetAddress1.simulate('change', {target: {value: ''}});

    expect(getSaveProfileButton(wrapper).first().prop('disabled')).toBe(true);
  });
});
