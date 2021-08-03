import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {urlParamsStore} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {InstitutionApi, InstitutionMembershipRequirement} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {AdminInstitutionEdit} from './admin-institution-edit';
import ReactSwitch from 'react-switch';
import {InputSwitch} from "primereact/inputswitch";

describe('AdminInstitutionEditSpec', () => {

  const component = () => {
    return mount(<AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}} />);
  };

  beforeEach(() => {
    serverConfigStore.set({config: defaultServerConfig});

    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });

  it('should throw error for existing Institution if display name is more than 80 characters', async() => {
    urlParamsStore.next({
      institutionId: 'Verily'
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const testInput = fp.repeat(83, 'a');
    const displayNameText = wrapper.find('[id="displayName"]').first();
    displayNameText.simulate('change', {target: {value: testInput}});
    displayNameText.simulate('blur');
    expect(wrapper.find('[data-test-id="displayNameError"]').first().prop('children'))
      .toContain('Display name must be 80 characters or less');
  });

  it('should default DUA to Master for RT', async() => {
    urlParamsStore.next({
      institutionId: 'Verily'
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const rtAgreementTypeDropDown = wrapper.find('[data-test-id="rt-agreement-dropdown"]');

    expect(rtAgreementTypeDropDown.first().props().value).toBe(InstitutionMembershipRequirement.DOMAINS);
  });

  it('should default DUA to Master for CT', async() => {
    urlParamsStore.next({
      institutionId: 'Verily'
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctAccessSwitch = wrapper.find('[data-test-id="ct-enabled-switch"]').instance() as InputSwitch;

    ctAccessSwitch.props.checked :{checked: true};
    const ctAgreementTypeDropDown = wrapper.find('[data-test-id="ct-agreement-dropdown"]');
    expect(ctAgreementTypeDropDown.first().props().value).toBe(InstitutionMembershipRequirement.DOMAINS);
  });

  it('should hide controlled card when controlled tier disabled', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const testInput = fp.repeat(81, 'a');

    const ctEnabledToggle = wrapper.find('[data-test-id="rt-agreement-dropdown"]').instance() as ReactSwitch;

    const agreementTypeDropDown = wrapper.find('[data-test-id="rt-agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange({
      originalEvent: undefined,
      value: InstitutionMembershipRequirement.DOMAINS,
      target: {name: 'name', id: '', value: InstitutionMembershipRequirement.DOMAINS}
    });
    await waitOneTickAndUpdate(wrapper);

    let rtEmailAddressDiv = wrapper.find('[data-test-id="rtEmailAddress"]');
    let rtEmailDomainDiv = wrapper.find('[data-test-id="rtEmailDomain"]');
    expect(rtEmailAddressDiv.length).toBe(0);
    expect(rtEmailDomainDiv.length).toBe(2);

    const rtEmailDomainLabel = rtEmailDomainDiv.first().props().children[0];
    expect(rtEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    agreementTypeDropDown.props.onChange(
        {
          originalEvent: undefined, value: InstitutionMembershipRequirement.ADDRESSES,
          target: {name: 'name', id: '', value: InstitutionMembershipRequirement.ADDRESSES}
        });
    await waitOneTickAndUpdate(wrapper);

    rtEmailAddressDiv = wrapper.find('[data-test-id="rtEmailAddress"]');
    rtEmailDomainDiv = wrapper.find('[data-test-id="rtEmailDomain"]');
    expect(rtEmailAddressDiv.length).toBe(2);
    expect(rtEmailDomainDiv.length).toBe(0);

    const rtEmailAddressLabel = rtEmailAddressDiv.first().props().children[0];

    expect(rtEmailAddressLabel.props.children).toBe('Accepted Email Addresses');

  });

  it('should show appropriate section after changing agreement type', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const testInput = fp.repeat(81, 'a');

    const agreementTypeDropDown = wrapper.find('[data-test-id="rt-agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange({
      originalEvent: undefined,
      value: InstitutionMembershipRequirement.DOMAINS,
      target: {name: 'name', id: '', value: InstitutionMembershipRequirement.DOMAINS}
    });
    await waitOneTickAndUpdate(wrapper);

    let rtEmailAddressDiv = wrapper.find('[data-test-id="rtEmailAddress"]');
    let rtEmailDomainDiv = wrapper.find('[data-test-id="rtEmailDomain"]');
    expect(rtEmailAddressDiv.length).toBe(0);
    expect(rtEmailDomainDiv.length).toBe(2);

    const rtEmailDomainLabel = rtEmailDomainDiv.first().props().children[0];
    expect(rtEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    agreementTypeDropDown.props.onChange(
      {
        originalEvent: undefined, value: InstitutionMembershipRequirement.ADDRESSES,
        target: {name: 'name', id: '', value: InstitutionMembershipRequirement.ADDRESSES}
      });
    await waitOneTickAndUpdate(wrapper);

    rtEmailAddressDiv = wrapper.find('[data-test-id="rtEmailAddress"]');
    rtEmailDomainDiv = wrapper.find('[data-test-id="rtEmailDomain"]');
    expect(rtEmailAddressDiv.length).toBe(2);
    expect(rtEmailDomainDiv.length).toBe(0);

    const rtEmailAddressLabel = rtEmailAddressDiv.first().props().children[0];

    expect(rtEmailAddressLabel.props.children).toBe('Accepted Email Addresses');

  });

  it('Should display error in case of invalid email Address Format', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    let rtEmailAddressError = wrapper.find('[data-test-id="rtEmailAddressError"]');
    expect(rtEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="rt-rt-agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: InstitutionMembershipRequirement.ADDRESSES,
        target: {name: 'name', id: '', value: InstitutionMembershipRequirement.ADDRESSES}});
    await waitOneTickAndUpdate(wrapper);

    // In case of a single entry which is not in the correct format
    wrapper.find('[data-test-id="rtEmailAddressInput"]').first().simulate('change', {target: {value: 'rtInvalidEmail@domain'}});
    wrapper.find('[data-test-id="rtEmailAddressInput"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="rtEmailAddressError"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : rtInvalidEmail@domain');

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    wrapper.find('[data-test-id="rtEmailAddressInput"]').first()
      .simulate('change', {
        target: {
          value:
            'invalidEmail@domain@org,\n' +
            'correctEmail@someDomain.org,\n' +
            ' correctEmail.123.hello@someDomain567.org.com   \n' +
            ' invalidEmail   ,\n' +
            ' justDomain.org,\n' +
            'someEmail@broadinstitute.org\n' +
            'nope@just#plain#wrong'
        }
      });
    wrapper.find('[data-test-id="rtEmailAddressInput"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="rtEmailAddressError"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    wrapper.find('[data-test-id="rtEmailAddressInput"]').first()
      .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    wrapper.find('[data-test-id="rtEmailAddressInput"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="rtEmailAddressError"]');
    expect(rtEmailAddressError.length).toBe(0);

  });

  it('Should display error in case of invalid email Domain Format', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    let rtEmailAddressError = wrapper.find('[data-test-id="rtEmailDomainError"]');
    expect(rtEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="rt-rt-agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: InstitutionMembershipRequirement.DOMAINS,
        target: {name: 'name', id: '', value: InstitutionMembershipRequirement.DOMAINS}});
    await waitOneTickAndUpdate(wrapper);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first()
      .simulate('change', {target: {value: 'rtInvalidEmail@domain'}});
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="rtEmailDomainError"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : invalidEmail@domain');

    // Multiple Entries with correct and incorrect Email Domain format
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first()
      .simulate('change', {
        target: {
          value:
            'someEmailAddress@domain@org,\n' +
            'someDomain123.org.com        ,\n' +
            ' justSomeText,\n' +
            ' justDomain.org,\n' +
            'broadinstitute.org#wrongTest'
        }
      });
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="rtEmailDomainError"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');


    wrapper.find('[data-test-id="rtEmailDomainInput"]').first()
      .simulate('change', {target: {value: 'domain.com'}});
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="rtEmailDomainError"]');
    expect(rtEmailAddressError.length).toBe(0);

  });

  it('Should ignore empty string in email Domain', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const agreementTypeDropDown = wrapper.find('[data-test-id="rt-agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: InstitutionMembershipRequirement.DOMAINS,
        target: {name: 'name', id: '', value: InstitutionMembershipRequirement.DOMAINS}});
    await waitOneTickAndUpdate(wrapper);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first()
      .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="rtEmailDomainInput"]').first().prop('value'))
      .toBe('validEmail.com,\njustSomeRandom.domain');
  });

  it('Should ignore whitespaces in email address', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    const agreementTypeDropDown = wrapper.find('[data-test-id="rt-agreement-dropdown"]').instance() as Dropdown;
    agreementTypeDropDown.props.onChange(
      {originalEvent: undefined, value: InstitutionMembershipRequirement.DOMAINS,
        target: {name: 'name', id: '', value: InstitutionMembershipRequirement.DOMAINS}});
    await waitOneTickAndUpdate(wrapper);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first()
      .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    wrapper.find('[data-test-id="rtEmailDomainInput"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="rtEmailDomainInput"]').first().prop('value'))
      .toBe('someDomain.com,\njustSomeRandom.domain');
  });
});
