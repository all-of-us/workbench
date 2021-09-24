import {mount} from 'enzyme';
import * as React from 'react';

import {profileStore} from 'app/utils/stores';
import {CreateBillingAccountModal} from "./create-billing-account-modal";
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

describe('CreateBillingAccountModal', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();
  const profile = ProfileStubVariables.PROFILE_STUB;

  const component = () => {
    return mount(<CreateBillingAccountModal onClose={() => {}}/>);
  };

  beforeEach(() => {
    profileStore.set({profile, load, reload, updateCache});
  });

  it('simulate creating billing account with all steps correctly', async() => {
    const wrapper = component();
    // Show step 0 at start.
    expect(wrapper.find('[data-test-id="step-0-modal"]').exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="step-1-modal"]').exists()).toBeFalsy();
    expect(wrapper.find('[data-test-id="step-2-modal"]').exists()).toBeFalsy();
    expect(wrapper.find('[data-test-id="step-3-modal"]').exists()).toBeFalsy();

    // Show step 1 when click use-billing-partner-button button, verify value populated.
    wrapper.find('[data-test-id="use-billing-partner-button"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="step-0-modal"]').exists()).toBeFalsy();
    expect(wrapper.find('[data-test-id="step-1-modal"]').exists()).toBeTruthy();

    // The only enabled input is user phone number. Others are populated from profile API.
    expect(wrapper.find('[data-test-id="user-first-name"]').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find('[data-test-id="user-last-name"]').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find('[data-test-id="user-contact-email"]').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find('[data-test-id="user-workbench-id"]').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find('[data-test-id="user-institution"]').first().prop('disabled')).toBeTruthy();
    expect(wrapper.find('[data-test-id="user-phone-number"]').first().prop('disabled')).toBeFalsy();

    expect(wrapper.find('[data-test-id="user-first-name"]').first().prop('value'))
    .toEqual('Tester!@#$%^&*()><script>alert("hello");</script>');
    expect(wrapper.find('[data-test-id="user-last-name"]').first().prop('value'))
    .toEqual('MacTesterson!@#$%^&*()><script>alert("hello");</script>');
    expect(wrapper.find('[data-test-id="user-phone-number"]').first().prop('value')).toBeUndefined();
    expect(wrapper.find('[data-test-id="user-contact-email"]').first().prop('value'))
    .toEqual('tester@mactesterson.edu><script>alert("hello");</script>');
    expect(wrapper.find('[data-test-id="user-workbench-id"]').first().prop('value')).toEqual('tester@fake-research-aou.org');
    expect(wrapper.find('[data-test-id="user-institution"]').first().prop('value')).toEqual('The Broad Institute');

    // Type invalid phone number then valid phone number
    wrapper.find('[data-test-id="user-phone-number"]').first().simulate('change', {target: {value: '1234x'}});
    expect(wrapper.find('[data-test-id="next-button"]').first().prop('disabled')).toBeTruthy();
    wrapper.find('[data-test-id="user-phone-number"]').first().simulate('change', {target: {value: '1234567890'}});
    expect(wrapper.find('[data-test-id="next-button"]').first().prop('disabled')).toBeFalsy();

    // Go to step 2 by clicking next button
    wrapper.find('[data-test-id="next-button"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="step-1-modal"]').exists()).toBeFalsy();
    expect(wrapper.find('[data-test-id="step-2-modal"]').exists()).toBeTruthy();

    // Go to step 3 and review user input
    wrapper.find('[data-test-id="next-button"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="step-2-modal"]').exists()).toBeFalsy();

    expect(wrapper.find('[data-test-id="user-first-name-text"]').first().props().children)
    .toEqual('Tester!@#$%^&*()><script>alert("hello");</script>');
    expect(wrapper.find('[data-test-id="user-last-name-text"]').first().props().children)
    .toEqual('MacTesterson!@#$%^&*()><script>alert("hello");</script>');
    expect(wrapper.find('[data-test-id="user-phone-number-text"]').first().props().children)
    .toEqual('1234567890');
    expect(wrapper.find('[data-test-id="user-contact-email-text"]').first().props().children)
    .toEqual('tester@mactesterson.edu><script>alert("hello");</script>');
    expect(wrapper.find('[data-test-id="user-workbench-id-text"]').first().props().children)
    .toEqual('tester@fake-research-aou.org');
    expect(wrapper.find('[data-test-id="user-institution-text"]').first().props().children)
    .toEqual('The Broad Institute');
    expect(wrapper.find('[data-test-id="nih-funded-text"]').first().props().children).toEqual('N/A');
  });
});
