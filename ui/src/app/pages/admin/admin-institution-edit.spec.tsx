import {mount} from 'enzyme';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import {InputSwitch} from "primereact/inputswitch";
import {MemoryRouter, Route} from 'react-router-dom';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/stores';
import {InstitutionApi, InstitutionMembershipRequirement} from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';
import {simulateComponentChange, waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {InstitutionApiStub, VERILY, VERILY_WITHOUT_CT} from 'testing/stubs/institution-api-stub';
import {AdminInstitutionEdit} from './admin-institution-edit';

describe('AdminInstitutionEditSpec - edit mode', () => {

  const component = (institution = VERILY.shortName) => {
    return mount(<MemoryRouter initialEntries={[`/admin/institution/edit/${institution}`]}>
      <Route path='/admin/institution/edit/:institutionId'>
        <AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}}/>
      </Route>
    </MemoryRouter>);
  }

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

  it('should always show RT card details', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="registered-card-details"]').exists()).toBeTruthy();
  });

  it('should show CT card details when institution has controlled tier access enabled', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeTruthy();
  });

  it('should hide CT card details when institution has controlled tier access disabled', async() => {
    const wrapper = component(VERILY_WITHOUT_CT.shortName);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeFalsy();
  });

  it('should hide/show CT card details when controlled tier disabled/enabled', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, false);
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeFalsy();

    await simulateComponentChange(wrapper, ctEnabledToggle, true);
    expect(ctEnabledToggle.props.checked).toBeTruthy();
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeTruthy();
  });

  it('should not change eRA Required and tier enabled switches when changing tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect((wrapper.find('[data-test-id="registered-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect((wrapper.find('[data-test-id="controlled-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect((wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();

    // change Registered from DOMAIN to ADDRESS

    expect(wrapper.find('[data-test-id="registered-email-domain"]').exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="registered-email-address"]').exists()).toBeFalsy();

    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    expect(wrapper.find('[data-test-id="registered-email-address"]').exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="registered-email-domain"]').exists()).toBeFalsy();

    expect((wrapper.find('[data-test-id="registered-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect((wrapper.find('[data-test-id="controlled-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect((wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
  });

  it('should update institution tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    // Value before test.
    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').first().prop('value'))
    .toBe('verily.com,\ngoogle.com');
    expect(wrapper.find('[data-test-id="controlled-email-address-input"]').first().prop('value'))
    .toBe('foo@verily.com');

    const ctAgreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, ctAgreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    wrapper.find('[data-test-id="controlled-email-domain-input"]').first()
    .simulate('change', {target: {value: 'domain.com'}});

    wrapper.find('[data-test-id="save-institution-button"]').first().simulate('click');
    // RT no change
    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').first().prop('value'))
    .toBe('verily.com,\n' + 'google.com');
    // CT changed to email domains
    expect(wrapper.find('[data-test-id="controlled-email-domain-input"]').first().prop('value'))
    .toBe('domain.com');
    // CT email addresses become empty
    expect(wrapper.find('[data-test-id="controlled-email-address-input"]').length).toBe(0);
  });

  it('should show appropriate section after changing agreement type in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    let rtEmailAddressDiv = wrapper.find('[data-test-id="registered-email-address"]');
    let rtEmailDomainDiv = wrapper.find('[data-test-id="registered-email-domain"]');
    expect(rtEmailAddressDiv.length).toBe(0);
    expect(rtEmailDomainDiv.length).toBe(2);

    const rtEmailDomainLabel = rtEmailDomainDiv.first().props().children[0];
    expect(rtEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    rtEmailAddressDiv = wrapper.find('[data-test-id="registered-email-address"]');
    rtEmailDomainDiv = wrapper.find('[data-test-id="registered-email-domain"]');
    expect(rtEmailAddressDiv.length).toBe(2);
    expect(rtEmailDomainDiv.length).toBe(0);

    const rtEmailAddressLabel = rtEmailAddressDiv.first().props().children[0];
    expect(rtEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('should update RT and CT requirements simultaneously when both changed', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    const rtAgreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    const ctAgreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, rtAgreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);
    await simulateComponentChange(wrapper, ctAgreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
    .simulate('change', {target: {value: 'someDomain.com,\njustSomeRandom.domain,\n,'}});
    wrapper.find('[data-test-id="registered-email-domain-input"]').first().simulate('blur');
    wrapper.find('[data-test-id="controlled-email-address-input"]').first()
    .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    wrapper.find('[data-test-id="controlled-email-address-input"]').first().simulate('blur');

    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').first().prop('value'))
    .toBe('someDomain.com,\njustSomeRandom.domain');
    expect(wrapper.find('[data-test-id="controlled-email-address-input"]').first().prop('value'))
    .toBe('correctEmail@domain.com');
  });

  it('should show appropriate section after changing agreement type in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    const agreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    let ctEmailAddressDiv = wrapper.find('[data-test-id="controlled-email-address"]');
    let ctEmailDomainDiv = wrapper.find('[data-test-id="controlled-email-domain"]');
    expect(ctEmailAddressDiv.length).toBe(0);
    expect(ctEmailDomainDiv.length).toBe(2);

    const ctEmailDomainLabel = ctEmailDomainDiv.first().props().children[0];
    expect(ctEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    ctEmailAddressDiv = wrapper.find('[data-test-id="controlled-email-address"]');
    ctEmailDomainDiv = wrapper.find('[data-test-id="controlled-email-domain"]');
    expect(ctEmailAddressDiv.length).toBe(2);
    expect(ctEmailDomainDiv.length).toBe(0);

    const ctEmailAddressLabel = ctEmailAddressDiv.first().props().children[0];
    expect(ctEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    let rtEmailAddressError = wrapper.find('[data-test-id="registered-email-address-error"]');
    expect(rtEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-address-error"]');
    expect(rtEmailAddressError.length).toBe(0);

    // In case of a single entry which is not in the correct format
    wrapper.find('[data-test-id="registered-email-address-input"]').first().simulate('change', {target: {value: 'rtInvalidEmail@domain'}});
    wrapper.find('[data-test-id="registered-email-address-input"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-address-error"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : rtInvalidEmail@domain');

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    wrapper.find('[data-test-id="registered-email-address-input"]').first()
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
    wrapper.find('[data-test-id="registered-email-address-input"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-address-error"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    wrapper.find('[data-test-id="registered-email-address-input"]').first()
      .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    wrapper.find('[data-test-id="registered-email-address-input"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-address-error"]');
    expect(rtEmailAddressError.length).toBe(0);
  });

  it('Should display error in case of invalid email Address Format in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    let ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-address-error"]');
    expect(ctEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-address-error"]');
    expect(ctEmailAddressError.length).toBe(0);

    // In case of a single entry which is not in the correct format
    wrapper.find('[data-test-id="controlled-email-address-input"]').first().simulate('change', {target: {value: 'ctInvalidEmail@domain'}});
    wrapper.find('[data-test-id="controlled-email-address-input"]').first().simulate('blur');
    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-address-error"]');
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Addresses are not valid : ctInvalidEmail@domain');

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    wrapper.find('[data-test-id="controlled-email-address-input"]').first()
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
    wrapper.find('[data-test-id="controlled-email-address-input"]').first().simulate('blur');
    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-address-error"]');
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    wrapper.find('[data-test-id="controlled-email-address-input"]').first()
    .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    wrapper.find('[data-test-id="controlled-email-address-input"]').first().simulate('blur');
    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-address-error"]');
    expect(ctEmailAddressError.length).toBe(0);
  });

  it('Should display error in case of invalid email Domain Format in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    let rtEmailAddressError = wrapper.find('[data-test-id="registered-email-domain-error"]');
    expect(rtEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-domain-error"]');
    expect(rtEmailAddressError.length).toBe(0);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
      .simulate('change', {target: {value: 'invalidEmail@domain'}});
    wrapper.find('[data-test-id="registered-email-domain-input"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-domain-error"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : invalidEmail@domain');

    // Multiple Entries with correct and incorrect Email Domain format
    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
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
    wrapper.find('[data-test-id="registered-email-domain-input"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-domain-error"]');
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');


    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
      .simulate('change', {target: {value: 'domain.com'}});
    wrapper.find('[data-test-id="registered-email-domain-input"]').first().simulate('blur');
    rtEmailAddressError = wrapper.find('[data-test-id="registered-email-domain-error"]');
    expect(rtEmailAddressError.length).toBe(0);
  });

  it('Should display error in case of invalid email Domain Format in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    let ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-domain-error"]');
    expect(ctEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-domain-error"]');
    expect(ctEmailAddressError.length).toBe(0);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first()
    .simulate('change', {target: {value: 'invalidEmail@domain'}});
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first().simulate('blur');
    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-domain-error"]');
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Domains are not valid : invalidEmail@domain');

    // Multiple Entries with correct and incorrect Email Domain format
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first()
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
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first().simulate('blur');
    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-domain-error"]');
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');

    wrapper.find('[data-test-id="controlled-email-domain-input"]').first()
    .simulate('change', {target: {value: 'domain.com'}});
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first().simulate('blur');
    ctEmailAddressError = wrapper.find('[data-test-id="controlled-email-domain-error"]');
    expect(ctEmailAddressError.length).toBe(0);
  });

  it('Should ignore empty string in email Domain in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
      .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    wrapper.find('[data-test-id="registered-email-domain-input"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').first().prop('value'))
      .toBe('validEmail.com,\njustSomeRandom.domain');

    expect(wrapper.find('[data-test-id="registered-email-domain-domain-error"]').length).toBe(0);
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);
    await waitOneTickAndUpdate(wrapper);

    const agreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first()
    .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="controlled-email-domain-input"]').first().prop('value'))
    .toBe('validEmail.com,\njustSomeRandom.domain');

    expect(wrapper.find('[data-test-id="controlled-email-domain-domain-error"]').length).toBe(0);
  });

  it('Should ignore whitespaces in email domains in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
      .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    wrapper.find('[data-test-id="registered-email-domain-input"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').first().prop('value'))
      .toBe('someDomain.com,\njustSomeRandom.domain');

    expect(wrapper.find('[data-test-id="registered-email-domain-domain-error"]').length).toBe(0);
  });

  it('Should ignore whitespaces in email domains in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    const agreementTypeDropDown = wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first()
    .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    wrapper.find('[data-test-id="controlled-email-domain-input"]').first().simulate('blur');
    expect(wrapper.find('[data-test-id="controlled-email-domain-input"]').first().prop('value'))
    .toBe('someDomain.com,\njustSomeRandom.domain');

    expect(wrapper.find('[data-test-id="controlled-email-domain-domain-error"]').length).toBe(0);
  });
});

describe('AdminInstitutionEditSpec - add mode', () => {

  const component = () => {
    return mount(<MemoryRouter initialEntries={[`/admin/institution/add`]}>
      <Route path='/admin/institution/add'>
        <AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}}/>
      </Route>
    </MemoryRouter>);
  }

  beforeEach(() => {
    serverConfigStore.set({config: defaultServerConfig});
    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });

  it('should always show RT card details', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="registered-card-details"]').exists()).toBeTruthy();
  });

  it('should not initially show CT card details', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeFalsy();
  });

  it('should hide/show CT card details when controlled tier enabled/disabled', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeFalsy();

    const ctEnabledToggle = wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;
    await simulateComponentChange(wrapper, ctEnabledToggle, true);
    expect(ctEnabledToggle.props.checked).toBeTruthy();
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeTruthy();

    await simulateComponentChange(wrapper, ctEnabledToggle, false);
    expect(ctEnabledToggle.props.checked).toBeFalsy();
    expect(wrapper.find('[data-test-id="controlled-card-details"]').exists()).toBeFalsy();
  });

  it('should update institution tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(wrapper.find('[data-test-id="registered-email-address-input"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="controlled-email-address-input"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="controlled-email-domain-input"]').length).toBe(0);

    const agreementTypeDropDown = wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
        .simulate('change', {target: {value: 'domain.com'}});
    wrapper.find('[data-test-id="registered-email-domain-input"]').first()
        .simulate('blur');

    // RT no change
    expect(wrapper.find('[data-test-id="registered-email-domain-input"]').first().prop('value'))
        .toBe('domain.com');
  });
});
