import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import { Profile, ProfileApi } from 'generated/fetch';

import {
  DataUserCodeOfConduct,
  DuccSignatureState,
} from 'app/components/data-user-code-of-conduct';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { getLiveDUCCVersion } from 'app/utils/code-of-conduct';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';

describe('DataUserCodeOfConduct', () => {
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

  const component = (
    signatureState: DuccSignatureState = DuccSignatureState.UNSIGNED
  ) =>
    mount(
      <MemoryRouter>
        <DataUserCodeOfConduct
          {...{ signatureState }}
          hideSpinner={() => {}}
          showSpinner={() => {}}
        />
      </MemoryRouter>
    );

  const duccComponent = (wrapper) =>
    wrapper.childAt(0).childAt(0).childAt(0).childAt(0).childAt(0);

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    reload.mockImplementation(async () => {
      const newProfile = await profileApi().getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });
    serverConfigStore.set({ config: defaultServerConfig });
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should not allow DataUserCodeOfConduct without identical initials', async () => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    duccComponent(wrapper).setState({ proceedDisabled: false });
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')
    ).toBeTruthy();

    // fill required fields
    wrapper
      .find('[data-test-id="ducc-initials-input"]')
      .forEach((node, index) => {
        node.simulate('change', { target: { value: 'X' + index.toString() } });
      });
    expect(
      wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')
    ).toBeTruthy();
  });

  it('should not allow DataUserCodeOfConduct with only one field populated', async () => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    duccComponent(wrapper).setState({ proceedDisabled: false });
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')
    ).toBeTruthy();

    // add initials to just one initials input field.
    wrapper
      .find('[data-test-id="ducc-initials-input"]')
      .first()
      .simulate('change', { target: { value: 'XX' } });

    expect(
      wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')
    ).toBeTruthy();
  });

  it('should populate username and name from the profile automatically', async () => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    duccComponent(wrapper).setState({ proceedDisabled: false });
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(
      wrapper.find('[data-test-id="ducc-name-input"]').first().props().value
    ).toBe(
      ProfileStubVariables.PROFILE_STUB.givenName +
        ' ' +
        ProfileStubVariables.PROFILE_STUB.familyName
    );
    expect(
      wrapper.find('[data-test-id="ducc-user-id-input"]').first().props().value
    ).toBe(ProfileStubVariables.PROFILE_STUB.username);
  });

  it('should submit DataUserCodeOfConduct acceptance with version number', async () => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    duccComponent(wrapper).setState({ proceedDisabled: false });
    await waitOneTickAndUpdate(wrapper);

    const spy = jest.spyOn(profileApi(), 'submitDUCC');
    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')
    ).toBeTruthy();

    // add initials to each initials input field.
    wrapper.find('[data-test-id="ducc-initials-input"]').forEach((node) => {
      node.simulate('change', { target: { value: 'XX' } });
    });

    expect(
      wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')
    ).toBeFalsy();
    wrapper.find('[data-test-id="submit-ducc-button"]').simulate('click');
    expect(spy).toHaveBeenCalledWith(getLiveDUCCVersion(), 'XX');
  });

  it('should display the Content and Signature pages in SIGNED mode if the user is up to date', async () => {
    const wrapper = component(DuccSignatureState.SIGNED);
    expect(
      wrapper.find('[data-test-id="ducc-content-page"]').exists()
    ).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="ducc-signature-page"]').exists()
    ).toBeTruthy();
  });

  it('should not display the Content and Signature pages in SIGNED mode if the user has not signed a DUCC', async () => {
    updateProfile({ duccSignedVersion: undefined });

    const wrapper = component(DuccSignatureState.SIGNED);
    expect(
      wrapper.find('[data-test-id="ducc-content-page"]').exists()
    ).toBeFalsy();
    expect(
      wrapper.find('[data-test-id="ducc-signature-page"]').exists()
    ).toBeFalsy();
  });
});
