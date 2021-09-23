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

const findRTDetails = (wrapper) => wrapper.find('[data-test-id="registered-card-details"]');
const findRTDropdown = (wrapper) => wrapper.find('[data-test-id="registered-agreement-dropdown"]');

const findCTDetails = (wrapper) => wrapper.find('[data-test-id="controlled-card-details"]');
const findCTDropdown = (wrapper) => wrapper.find('[data-test-id="controlled-agreement-dropdown"]');
const findCTEnabled = (wrapper) => wrapper.find('[data-test-id="controlled-enabled-switch"]').first().instance() as InputSwitch;

const findRTAddress = (wrapper) => wrapper.find('[data-test-id="registered-email-address"]');
const findRTDomain = (wrapper) => wrapper.find('[data-test-id="registered-email-domain"]');
const findCTAddress = (wrapper) => wrapper.find('[data-test-id="controlled-email-address"]');
const findCTDomain = (wrapper) => wrapper.find('[data-test-id="controlled-email-domain"]');

const findRTAddressInput = (wrapper) => wrapper.find('[data-test-id="registered-email-address-input"]');
const findRTDomainInput = (wrapper) => wrapper.find('[data-test-id="registered-email-domain-input"]');
const findCTAddressInput = (wrapper) => wrapper.find('[data-test-id="controlled-email-address-input"]');
const findCTDomainInput = (wrapper) => wrapper.find('[data-test-id="controlled-email-domain-input"]');

const findRTAddressError = (wrapper) => wrapper.find('[data-test-id="registered-email-address-error"]');
const findRTDomainError= (wrapper) => wrapper.find('[data-test-id="registered-email-domain-error"]');
const findCTAddressError = (wrapper) => wrapper.find('[data-test-id="controlled-email-address-error"]');
const findCTDomainError= (wrapper) => wrapper.find('[data-test-id="controlled-email-domain-error"]');

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
    expect(findRTDetails(wrapper).exists()).toBeTruthy();
  });

  it('should show CT card details when institution has controlled tier access enabled', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeTruthy();
  });

  it('should hide CT card details when institution has controlled tier access disabled', async() => {
    const wrapper = component(VERILY_WITHOUT_CT.shortName);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeFalsy();
  });

  it('should hide/show CT card details when controlled tier disabled/enabled', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(findCTDetails(wrapper).exists()).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, false);
    expect(findCTDetails(wrapper).exists()).toBeFalsy();

    await simulateComponentChange(wrapper, ctEnabledToggle, true);
    expect(ctEnabledToggle.props.checked).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeTruthy();
  });

  it('should not change eRA Required and tier enabled switches when changing tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect((wrapper.find('[data-test-id="registered-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect((wrapper.find('[data-test-id="controlled-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect(findCTEnabled(wrapper).props.checked).toBeTruthy();

    // change Registered from DOMAIN to ADDRESS

    expect(findRTDomain(wrapper).exists()).toBeTruthy();
    expect(findRTAddress(wrapper).exists()).toBeFalsy();

    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    expect(findRTAddress(wrapper).exists()).toBeTruthy();
    expect(findRTDomain(wrapper).exists()).toBeFalsy();

    expect((wrapper.find('[data-test-id="registered-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect((wrapper.find('[data-test-id="controlled-era-required-switch"]').first().instance() as InputSwitch).props.checked).toBeTruthy();
    expect(findCTEnabled(wrapper).props.checked).toBeTruthy();
  });

  it('should update institution tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    // Value before test.
    expect(findRTDomainInput(wrapper).first().prop('value'))
    .toBe('verily.com,\ngoogle.com');
    expect(findCTAddressInput(wrapper).first().prop('value'))
    .toBe('foo@verily.com');

    const ctAgreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, ctAgreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    findCTDomainInput(wrapper).first()
    .simulate('change', {target: {value: 'domain.com'}});

    wrapper.find('[data-test-id="save-institution-button"]').first().simulate('click');
    // RT no change
    expect(findRTDomainInput(wrapper).first().prop('value'))
    .toBe('verily.com,\n' + 'google.com');
    // CT changed to email domains
    expect(findCTDomainInput(wrapper).first().prop('value'))
    .toBe('domain.com');
    // CT email addresses become empty
    expect(findCTAddressInput(wrapper).length).toBe(0);
  });

  it('should show appropriate section after changing agreement type in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    let rtEmailAddressDiv = findRTAddress(wrapper);
    let rtEmailDomainDiv = findRTDomain(wrapper);
    expect(rtEmailAddressDiv.length).toBe(0);
    expect(rtEmailDomainDiv.length).toBe(2);

    const rtEmailDomainLabel = rtEmailDomainDiv.first().props().children[0];
    expect(rtEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    rtEmailAddressDiv = findRTAddress(wrapper);
    rtEmailDomainDiv = findRTDomain(wrapper);
    expect(rtEmailAddressDiv.length).toBe(2);
    expect(rtEmailDomainDiv.length).toBe(0);

    const rtEmailAddressLabel = rtEmailAddressDiv.first().props().children[0];
    expect(rtEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('should update RT and CT requirements simultaneously when both changed', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    const rtAgreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    const ctAgreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, rtAgreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);
    await simulateComponentChange(wrapper, ctAgreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
    .simulate('change', {target: {value: 'someDomain.com,\njustSomeRandom.domain,\n,'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    findCTAddressInput(wrapper).first()
    .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    findCTAddressInput(wrapper).first().simulate('blur');

    expect(findRTDomainInput(wrapper).first().prop('value'))
    .toBe('someDomain.com,\njustSomeRandom.domain');
    expect(findCTAddressInput(wrapper).first().prop('value'))
    .toBe('correctEmail@domain.com');
  });

  it('should show appropriate section after changing agreement type in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    const agreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    let ctEmailAddressDiv = findCTAddress(wrapper);
    let ctEmailDomainDiv = findCTDomain(wrapper);
    expect(ctEmailAddressDiv.length).toBe(0);
    expect(ctEmailDomainDiv.length).toBe(2);

    const ctEmailDomainLabel = ctEmailDomainDiv.first().props().children[0];
    expect(ctEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    ctEmailAddressDiv = findCTAddress(wrapper);
    ctEmailDomainDiv = findCTDomain(wrapper);
    expect(ctEmailAddressDiv.length).toBe(2);
    expect(ctEmailDomainDiv.length).toBe(0);

    const ctEmailAddressLabel = ctEmailAddressDiv.first().props().children[0];
    expect(ctEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    let rtEmailAddressError = findRTAddressError(wrapper);
    expect(rtEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    rtEmailAddressError = findRTAddressError(wrapper);
    expect(rtEmailAddressError.length).toBe(0);

    // In case of a single entry which is not in the correct format
    findRTAddressInput(wrapper).first().simulate('change', {target: {value: 'rtInvalidEmail@domain'}});
    findRTAddressInput(wrapper).first().simulate('blur');
    rtEmailAddressError = findRTAddressError(wrapper);
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : rtInvalidEmail@domain');

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    findRTAddressInput(wrapper).first()
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
    findRTAddressInput(wrapper).first().simulate('blur');
    rtEmailAddressError = findRTAddressError(wrapper);
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    findRTAddressInput(wrapper).first()
      .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    findRTAddressInput(wrapper).first().simulate('blur');
    rtEmailAddressError = findRTAddressError(wrapper);
    expect(rtEmailAddressError.length).toBe(0);
  });

  it('Should display error in case of invalid email Address Format in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    let ctEmailAddressError = findCTAddressError(wrapper);
    expect(ctEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    ctEmailAddressError = findCTAddressError(wrapper);
    expect(ctEmailAddressError.length).toBe(0);

    // In case of a single entry which is not in the correct format
    findCTAddressInput(wrapper).first().simulate('change', {target: {value: 'ctInvalidEmail@domain'}});
    findCTAddressInput(wrapper).first().simulate('blur');
    ctEmailAddressError = findCTAddressError(wrapper);
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Addresses are not valid : ctInvalidEmail@domain');

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    findCTAddressInput(wrapper).first()
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
    findCTAddressInput(wrapper).first().simulate('blur');
    ctEmailAddressError = findCTAddressError(wrapper);
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    findCTAddressInput(wrapper).first()
    .simulate('change', {target: {value: 'correctEmail@domain.com'}});
    findCTAddressInput(wrapper).first().simulate('blur');
    ctEmailAddressError = findCTAddressError(wrapper);
    expect(ctEmailAddressError.length).toBe(0);
  });

  it('Should display error in case of invalid email Domain Format in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    let rtEmailAddressError = findRTDomainError(wrapper);
    expect(rtEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    rtEmailAddressError = findRTDomainError(wrapper);
    expect(rtEmailAddressError.length).toBe(0);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: 'invalidEmail@domain'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    rtEmailAddressError = findRTDomainError(wrapper);
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : invalidEmail@domain');

    // Multiple Entries with correct and incorrect Email Domain format
    findRTDomainInput(wrapper).first()
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
    findRTDomainInput(wrapper).first().simulate('blur');
    rtEmailAddressError = findRTDomainError(wrapper);
    expect(rtEmailAddressError.first().props().children)
      .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');


    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: 'domain.com'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    rtEmailAddressError = findRTDomainError(wrapper);
    expect(rtEmailAddressError.length).toBe(0);
  });

  it('Should display error in case of invalid email Domain Format in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    let ctEmailAddressError = findCTDomainError(wrapper);
    expect(ctEmailAddressError.length).toBe(0);
    const agreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    ctEmailAddressError = findCTDomainError(wrapper);
    expect(ctEmailAddressError.length).toBe(0);

    // Single Entry with incorrect Email Domain format
    findCTDomainInput(wrapper).first()
    .simulate('change', {target: {value: 'invalidEmail@domain'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    ctEmailAddressError = findCTDomainError(wrapper);
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Domains are not valid : invalidEmail@domain');

    // Multiple Entries with correct and incorrect Email Domain format
    findCTDomainInput(wrapper).first()
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
    findCTDomainInput(wrapper).first().simulate('blur');
    ctEmailAddressError = findCTDomainError(wrapper);
    expect(ctEmailAddressError.first().props().children)
    .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');

    findCTDomainInput(wrapper).first()
    .simulate('change', {target: {value: 'domain.com'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    ctEmailAddressError = findCTDomainError(wrapper);
    expect(ctEmailAddressError.length).toBe(0);
  });

  it('Should ignore empty string in email Domain in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    expect(findRTDomainInput(wrapper).first().prop('value'))
      .toBe('validEmail.com,\njustSomeRandom.domain');

    expect(findRTDomainError(wrapper).length).toBe(0);
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);
    await waitOneTickAndUpdate(wrapper);

    const agreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findCTDomainInput(wrapper).first()
    .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    expect(findCTDomainInput(wrapper).first().prop('value'))
    .toBe('validEmail.com,\njustSomeRandom.domain');

    expect(findCTDomainError(wrapper).length).toBe(0);
  });

  it('Should ignore whitespaces in email domains in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    expect(findRTDomainInput(wrapper).first().prop('value'))
      .toBe('someDomain.com,\njustSomeRandom.domain');

    expect(findRTDomainError(wrapper).length).toBe(0);
  });

  it('Should ignore whitespaces in email domains in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);

    const agreementTypeDropDown = findCTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findCTDomainInput(wrapper).first()
    .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    expect(findCTDomainInput(wrapper).first().prop('value'))
    .toBe('someDomain.com,\njustSomeRandom.domain');

    expect(findCTDomainError(wrapper).length).toBe(0);
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
    expect(findRTDetails(wrapper).exists()).toBeTruthy();
  });

  it('should not initially show CT card details', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeFalsy();
  });

  it('should hide/show CT card details when controlled tier enabled/disabled', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(findCTDetails(wrapper).exists()).toBeFalsy();

    const ctEnabledToggle = findCTEnabled(wrapper);
    await simulateComponentChange(wrapper, ctEnabledToggle, true);
    expect(ctEnabledToggle.props.checked).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeTruthy();

    await simulateComponentChange(wrapper, ctEnabledToggle, false);
    expect(ctEnabledToggle.props.checked).toBeFalsy();
    expect(findCTDetails(wrapper).exists()).toBeFalsy();
  });

  it('should update institution tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(findRTDomainInput(wrapper).exists()).toBeFalsy();
    expect(findRTAddressInput(wrapper).exists()).toBeFalsy();
    expect(findCTAddressInput(wrapper).exists()).toBeFalsy();
    expect(findCTDomainInput(wrapper).exists()).toBeFalsy();

    const agreementTypeDropDown = findRTDropdown(wrapper).instance() as Dropdown;
    await simulateComponentChange(wrapper, agreementTypeDropDown, InstitutionMembershipRequirement.ADDRESSES);

    expect(findRTAddressInput(wrapper).exists()).toBeTruthy();
    expect(findRTAddressInput(wrapper).first().prop('value')).toBe('');

    expect(findRTDomainInput(wrapper).exists()).toBeFalsy();
    expect(findCTAddressInput(wrapper).exists()).toBeFalsy();
    expect(findCTDomainInput(wrapper).exists()).toBeFalsy();

    findRTAddressInput(wrapper).first()
        .simulate('change', {target: {value: 'user@domain.com'}});
    findRTAddressInput(wrapper).first()
        .simulate('blur');

    // RT no change
    expect(findRTAddressInput(wrapper).first().prop('value'))
        .toBe('user@domain.com');
  });
});
