import {mount} from 'enzyme';
import * as React from 'react';

import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {DataUseAgreement} from 'app/pages/profile/data-use-agreement';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

describe('DataUseAgreement', () => {
  const component = () => mount(<DataUseAgreement/>);

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should submit DataUseAgreement acceptance with version number', () => {
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
    expect(spy).toHaveBeenCalledWith(1, 'XX'); // dataUseAgreementVersion
  });

});
