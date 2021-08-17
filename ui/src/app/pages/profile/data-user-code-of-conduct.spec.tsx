import {mount} from 'enzyme';
import * as React from 'react';

import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore} from 'app/utils/stores';
import {Profile, ProfileApi} from 'generated/fetch';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

jest.mock('app/utils/navigation', () => ({
  ...(jest.requireActual('app/utils/navigation')),
  navigate: jest.fn()
}));

describe('DataUserCodeOfConduct', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;

  const component = () => mount(<DataUserCodeOfConduct hideSpinner={() => {}}
                                                       showSpinner={() => {}}/>);

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    reload.mockImplementation(async() => {
      const newProfile = await profileApi().getMe();
      profileStore.set({profile: newProfile, load, reload, updateCache});
    });

    profileStore.set({profile, load, reload, updateCache});
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should not allow DataUserCodeOfConduct without identical initials', async() => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    wrapper.childAt(0).setState({proceedDisabled: false});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="ducc-initials-input"]').forEach((node, index) => {
      node.simulate('change', {target: {value: 'X' + index.toString()}});
    });
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();
  });

  it('should not allow DataUserCodeOfConduct with only one field populated', async() => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    wrapper.childAt(0).setState({proceedDisabled: false});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="ducc-name-input"]').simulate('change', {target: {value: 'Fake Name'}});
    // add initials to just one initials input field.
    wrapper.find('[data-test-id="ducc-initials-input"]').first().simulate('change', {target: {value: 'XX'}});

    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();
  });

  it('should populate username and name from the profile automatically', async() => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    wrapper.childAt(0).setState({proceedDisabled: false});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="ducc-name-input"]').props().value)
      .toBe(ProfileStubVariables.PROFILE_STUB.givenName + ' ' + ProfileStubVariables.PROFILE_STUB.familyName);
    expect(wrapper.find('[data-test-id="ducc-user-id-input"]').props().value)
      .toBe(ProfileStubVariables.PROFILE_STUB.username);
  });

  it('should submit DataUserCodeOfConduct acceptance with version number', async() => {
    const wrapper = component();
    // Need to step past the HOC before setting state.
    wrapper.childAt(0).setState({proceedDisabled: false});
    await waitOneTickAndUpdate(wrapper);

    const spy = jest.spyOn(profileApi(), 'submitDataUseAgreement');
    wrapper.find('[data-test-id="ducc-next-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeTruthy();

    // fill required fields
    wrapper.find('[data-test-id="ducc-name-input"]').simulate('change', {target: {value: 'Fake Name'}});
    // add initials to each initials input field.
    wrapper.find('[data-test-id="ducc-initials-input"]').forEach((node) => {
      node.simulate('change', {target: {value: 'XX'}});
    });

    expect(wrapper.find('[data-test-id="submit-ducc-button"]').prop('disabled')).toBeFalsy();
    wrapper.find('[data-test-id="submit-ducc-button"]').simulate('click');
    expect(spy).toHaveBeenCalledWith(3, 'XX'); // dataUseAgreementVersion
  });

});
