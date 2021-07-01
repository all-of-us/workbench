import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import {TextInput} from 'app/components/inputs';
import {ProfileComponent} from 'app/pages/profile/profile-page';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {InstitutionApi, ProfileApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import SpyInstance = jest.SpyInstance;
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';


describe('ProfilePageComponent', () => {
  function getSubmitButton(wrapper: ReactWrapper): ReactWrapper {
    return wrapper.find('[data-test-id="submit-button"]');
  }

  function getDemographicSurveyButton(wrapper: ReactWrapper): ReactWrapper {
    return wrapper.find('[data-test-id="demographics-survey-button"]');
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

  const component = (controlledTierProfile = {}) => {
    return mount(<ProfileComponent controlledTierProfile={controlledTierProfile}/>);
  };

  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  beforeEach(() => {
    const profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    mockUpdateProfile = jest.spyOn(profileApi, 'updateProfile');

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async() => {
      const newProfile = await profileApi.getMe();
      profileStore.set({profile: newProfile, load, reload, updateCache});
    });

    profileStore.set({profile, load, reload, updateCache});

    serverConfigStore.set({config: {
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
      enableComplianceTraining: true
    }});

    const institutionApi = new InstitutionApiStub();
    registerApiClient(InstitutionApi, institutionApi);
  });

  it('should render the profile', () => {
    const wrapper = component();
    expect(wrapper.find(TextInput).first().prop('value')).toMatch(profile.givenName);
  });

  it('should save correctly', async() => {
    const wrapper = component();
    expect(profileStore.get().profile.givenName).toEqual(profile.givenName);

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

    const streetAddress1 = wrapper.find('[data-test-id="streetAddress1"]').first();
    expect(streetAddress1.prop('value')).toBe('Main street');

    streetAddress1.simulate('change', {target: {value: ''}});

    expect(getSaveProfileButton(wrapper).first().prop('disabled')).toBe(true);
  });

  it('should not display controlled tier card when there is no controlled tier', async() => {
    const wrapper = component();
    const profileCardCompleteButtons = wrapper.find('[data-test-id="incomplete-button"]');
    expect(profileCardCompleteButtons.length).toBe(4);
  });

  it('should display a controlled tier card when there is a controlled tier', async() => {
    const wrapper = component({controlledTierEnabled: true});
    const profileCardCompleteButtons = wrapper.find('[data-test-id="incomplete-button"]');
    expect(profileCardCompleteButtons.length).toBe(5);
  });

  it('should display a controlled tier card with a complete button when the user has completed training', async() => {
    const wrapper = component({controlledTierEnabled: true, controlledTierCompletionTime: 1});
    const profileCardCompleteButtons = wrapper.find('[data-test-id="completed-button"]');
    expect(profileCardCompleteButtons.length).toBe(1);
  });

});
