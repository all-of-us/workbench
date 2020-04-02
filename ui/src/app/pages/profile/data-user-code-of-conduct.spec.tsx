import {mount} from 'enzyme';
import * as React from 'react';

import {DataUseAgreement} from 'app/pages/profile/data-user-code-of-conduct';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {Profile, ProfileApi} from 'generated/fetch';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {waitOneTickAndUpdate} from "../../../testing/react-test-helpers";
import {DataUserCodeOfConductPage} from "./data-user-code-of-conduct";

const defaultConfig = {
  gsuiteDomain: 'researchallofus.org',
  requireInstitutionalVerification: false,
};

describe('DataUseAgreement', () => {
  const reload = jest.fn();
  const updateCache = jest.fn();
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;

  const component = () => mount(<DataUseAgreement/>);

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    reload.mockImplementation(async () => {
      const newProfile = await profileApi().getMe();
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
  });

  it('should render - v1', () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: false});
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should not allow DataUseAgreement without identical initials - v1', () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: false});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="submit-dua-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="dua-initials-input"]').forEach((node, index) => {
      node.simulate('change', {target: {value: 'X' + index.toString()}});
    });
    expect(wrapper.find('[data-test-id="submit-dua-button"]').prop('disabled')).toBeTruthy();
  });

  it('should not allow DataUseAgreement with only one field populated - v1', () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: false});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="submit-dua-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="dua-name-input"]')
    .simulate('change', {target: {value: 'Fake Name'}});
    // add initials to just one initials input field.
    wrapper.find('[data-test-id="dua-initials-input"]').first().simulate('change', {target: {value: 'XX'}});

    expect(wrapper.find('[data-test-id="submit-dua-button"]').prop('disabled')).toBeTruthy();
  });

  it('should populate username and name from the profile automatically - v1', async() => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: false});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="dua-name-input"]').props().value)
    .toBe(ProfileStubVariables.PROFILE_STUB.givenName + ' ' + ProfileStubVariables.PROFILE_STUB.familyName);
    expect(wrapper.find('[data-test-id="dua-contact-email-input"]').props().value)
    .toBe(ProfileStubVariables.PROFILE_STUB.contactEmail);
    expect(wrapper.find('[data-test-id="dua-username-input"]').props().value)
    .toBe(ProfileStubVariables.PROFILE_STUB.username);

  });

  it('should submit DataUseAgreement acceptance with version number - v1', async () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: false});
    const wrapper = component();
    const spy = jest.spyOn(profileApi(), 'submitDataUseAgreement');
    expect(wrapper.find('[data-test-id="submit-dua-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="dua-name-input"]')
    .simulate('change', {target: {value: 'Fake Name'}});
    // add initials to each initials input field.
    wrapper.find('[data-test-id="dua-initials-input"]').forEach((node) => {
      node.simulate('change', {target: {value: 'XX'}});
    });

    expect(wrapper.find('[data-test-id="submit-dua-button"]').prop('disabled')).toBeFalsy();
    wrapper.find('[data-test-id="submit-dua-button"]').simulate('click');
    expect(spy).toHaveBeenCalledWith(2, 'XX'); // dataUseAgreementVersion
  });

  it('should render', () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: true});
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should not allow DataUseAgreement without identical initials', async () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: true});
    const wrapper = component();
    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="dua-initials-input"]').forEach((node, index) => {
      node.simulate('change', {target: {value: 'X' + index.toString()}});
    });
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();
  });

  it('should not allow DataUseAgreement with only one field populated', async () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: true});
    const wrapper = component();
    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="ducc-name-input"]').simulate('change', {target: {value: 'Fake Name'}});
    // add initials to just one initials input field.
    wrapper.find('[data-test-id="dua-initials-input"]').first().simulate('change', {target: {value: 'XX'}});

    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();
  });

  it('should populate username and name from the profile automatically', async() => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: true});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="ducc-name-input"]').props().value)
    .toBe(ProfileStubVariables.PROFILE_STUB.givenName + ' ' + ProfileStubVariables.PROFILE_STUB.familyName);
    expect(wrapper.find('[data-test-id="ducc-user-id-input"]').props().value)
    .toBe(ProfileStubVariables.PROFILE_STUB.username);

  });

  it('should submit DataUseAgreement acceptance with version number', async () => {
    serverConfigStore.next({...defaultConfig, enableV2DataUserCodeOfConduct: true});
    const wrapper = component();
    const spy = jest.spyOn(profileApi(), 'submitDataUseAgreement');
    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="ducc-name-input"]')
    .simulate('change', {target: {value: 'Fake Name'}});
    // add initials to each initials input field.
    wrapper.find('[data-test-id="dua-initials-input"]').forEach((node) => {
      node.simulate('change', {target: {value: 'XX'}});
    });

    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeFalsy();
    wrapper.find('[data-test-id="submit-ducc-button"]').simulate('click');
    expect(spy).toHaveBeenCalledWith(3, 'XX'); // dataUseAgreementVersion
  });

});
