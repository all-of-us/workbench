import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount, ReactWrapper } from 'enzyme';

import { InstitutionApi, Profile, ProfileApi } from 'generated/fetch';

import { TextInput } from 'app/components/inputs';
import { ProfileComponent } from 'app/pages/profile/profile-component';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { InstitutionApiStub } from 'testing/stubs/institution-api-stub';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';

describe('ProfilePageComponent', () => {
  function getSaveProfileButton(wrapper: ReactWrapper): ReactWrapper {
    return wrapper.find('[data-test-id="save_profile"]');
  }

  function clickSaveProfileButton(wrapper: ReactWrapper) {
    const saveProfileButton = getSaveProfileButton(wrapper);
    saveProfileButton.first().simulate('click');
    return;
  }

  const component = (controlledTierProfile = {}) => {
    return mount(
      <MemoryRouter>
        <ProfileComponent
          controlledTierProfile={controlledTierProfile}
          hideSpinner={() => {}}
        />
      </MemoryRouter>
    );
  };

  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const updateProfile = (newUpdates: Partial<Profile>) => {
    profileStore.set({
      profile: { ...ProfileStubVariables.PROFILE_STUB, ...newUpdates },
      load,
      reload,
      updateCache,
    });
  };

  beforeEach(() => {
    const profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      updateProfile(await profileApi.getMe());
    });

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
        enableEraCommons: true,
        enableComplianceTraining: true,
      },
    });

    const institutionApi = new InstitutionApiStub();
    registerApiClient(InstitutionApi, institutionApi);
  });

  it('should render the profile', () => {
    const wrapper = component();
    expect(wrapper.find(TextInput).first().prop('value')).toMatch(
      ProfileStubVariables.PROFILE_STUB.givenName
    );
  });

  it('should save correctly', async () => {
    const wrapper = component();
    expect(profileStore.get().profile.givenName).toEqual(
      ProfileStubVariables.PROFILE_STUB.givenName
    );

    wrapper
      .find(TextInput)
      .first()
      .simulate('change', { target: { value: 'x' } });
    clickSaveProfileButton(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(reload).toHaveBeenCalled();
  });

  it('should invalidate inputs correctly', () => {
    const wrapper = component();
    wrapper
      .find(TextInput)
      .first()
      .simulate('change', { target: { value: '' } });
    expect(wrapper.find(TextInput).first().prop('invalid')).toBeTruthy();
  });

  it('should display/update address', async () => {
    const wrapper = component();

    let streetAddress1 = wrapper
      .find('[data-test-id="streetAddress1"]')
      .first();
    console.log(streetAddress1.props());
    expect(streetAddress1.prop('value')).toBe('Main street');

    streetAddress1.simulate('change', { target: { value: 'Broadway' } });

    clickSaveProfileButton(wrapper);
    await waitOneTickAndUpdate(wrapper);
    streetAddress1 = wrapper.find('[data-test-id="streetAddress1"]').first();

    expect(getSaveProfileButton(wrapper).first().prop('disabled')).toBe(true);
    expect(streetAddress1.prop('value')).toBe('Broadway');
  });

  it('should throw error if street Address 1 is empty', async () => {
    const wrapper = component();

    const streetAddress1 = wrapper
      .find('[data-test-id="streetAddress1"]')
      .first();
    expect(streetAddress1.prop('value')).toBe('Main street');

    streetAddress1.simulate('change', { target: { value: '' } });

    expect(getSaveProfileButton(wrapper).first().prop('disabled')).toBe(true);
  });

  it('should display a link to the signed DUCC if the user is up to date', async () => {
    const wrapper = component();
    expect(
      wrapper.find('[data-test-id="signed-ducc-panel"]').exists()
    ).toBeTruthy();
  });

  it('should not display a link to the signed DUCC if the user has not signed a DUCC', async () => {
    updateProfile({ duccSignedVersion: undefined });

    const wrapper = component();
    expect(
      wrapper.find('[data-test-id="signed-ducc-panel"]').exists()
    ).toBeFalsy();
  });
});
