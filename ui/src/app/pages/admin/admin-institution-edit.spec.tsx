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
const findRTDropdown = (wrapper) => wrapper.find('[data-test-id="registered-agreement-dropdown"]').instance() as Dropdown;
const findRTERARequired = (wrapper) => wrapper.find('[data-test-id="registered-era-required-switch"]').first().instance() as InputSwitch;

const findCTDetails = (wrapper) => wrapper.find('[data-test-id="controlled-card-details"]');
const findCTDropdown = (wrapper) => wrapper.find('[data-test-id="controlled-agreement-dropdown"]').instance() as Dropdown;
const findCTERARequired = (wrapper) => wrapper.find('[data-test-id="controlled-era-required-switch"]').first().instance() as InputSwitch;
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
const findRTDomainError = (wrapper) => wrapper.find('[data-test-id="registered-email-domain-error"]');
const findCTAddressError = (wrapper) => wrapper.find('[data-test-id="controlled-email-address-error"]');
const findCTDomainError = (wrapper) => wrapper.find('[data-test-id="controlled-email-domain-error"]');

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

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), false);
    expect(findCTDetails(wrapper).exists()).toBeFalsy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    expect(findCTEnabled(wrapper).props.checked).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeTruthy();
  });

  it('should not change eRA Required and tier enabled switches when changing tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    expect(findRTERARequired(wrapper).props.checked).toBeTruthy();
    expect(findCTERARequired(wrapper).props.checked).toBeTruthy();
    expect(findCTEnabled(wrapper).props.checked).toBeTruthy();

    // change Registered from DOMAIN to ADDRESS

    expect(findRTDomain(wrapper).exists()).toBeTruthy();
    expect(findRTAddress(wrapper).exists()).toBeFalsy();

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    expect(findRTAddress(wrapper).exists()).toBeTruthy();
    expect(findRTDomain(wrapper).exists()).toBeFalsy();

    expect(findRTERARequired(wrapper).props.checked).toBeTruthy();
    expect(findCTERARequired(wrapper).props.checked).toBeTruthy();
    expect(findCTEnabled(wrapper).props.checked).toBeTruthy();
  });

  it('should update institution tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    // Value before test.
    expect(findRTDomainInput(wrapper).first().prop('value')).toBe('verily.com,\ngoogle.com');
    expect(findCTAddressInput(wrapper).first().prop('value')).toBe('foo@verily.com');

    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    findCTDomainInput(wrapper).first().simulate('change', {target: {value: 'domain.com'}});

    wrapper.find('[data-test-id="save-institution-button"]').first().simulate('click');
    // RT no change
    expect(findRTDomainInput(wrapper).first().prop('value')).toBe('verily.com,\n' + 'google.com');
    // CT changed to email domains
    expect(findCTDomainInput(wrapper).first().prop('value')).toBe('domain.com');
    // CT email addresses become empty
    expect(findCTAddressInput(wrapper).exists()).toBeFalsy();
  });

  it('should show appropriate section after changing agreement type in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    expect(findRTAddress(wrapper).exists()).toBeFalsy();
    expect(findRTDomain(wrapper).exists()).toBeTruthy();

    const rtEmailDomainLabel = findRTDomain(wrapper).first().props().children[0];
    expect(rtEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    expect(findRTAddress(wrapper).exists()).toBeTruthy();
    expect(findRTDomain(wrapper).exists()).toBeFalsy();

    const rtEmailAddressLabel = findRTAddress(wrapper).first().props().children[0];
    expect(rtEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('should update RT and CT requirements simultaneously when both changed', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);
    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
        .simulate('change', {target: {value: 'someDomain.com,\njustSomeRandom.domain,\n,'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    findCTAddressInput(wrapper).first().simulate('change', {target: {value: 'correctEmail@domain.com'}});
    findCTAddressInput(wrapper).first().simulate('blur');

    expect(findRTDomainInput(wrapper).first().prop('value')).toBe('someDomain.com,\njustSomeRandom.domain');
    expect(findCTAddressInput(wrapper).first().prop('value')).toBe('correctEmail@domain.com');
  });

  it('should show appropriate section after changing agreement type in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    expect(findCTAddress(wrapper).exists()).toBeFalsy();
    expect(findCTDomain(wrapper).exists()).toBeTruthy();

    const ctEmailDomainLabel = findCTDomain(wrapper).first().props().children[0];
    expect(ctEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    expect(findCTAddress(wrapper).exists()).toBeTruthy();
    expect(findCTDomain(wrapper).exists()).toBeFalsy();

    const ctEmailAddressLabel = findCTAddress(wrapper).first().props().children[0];
    expect(ctEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    expect(findRTAddressError(wrapper).exists()).toBeFalsy();

    // In case of a single entry which is not in the correct format
    findRTAddressInput(wrapper).first().simulate('change', {target: {value: 'rtInvalidEmail@domain'}});
    findRTAddressInput(wrapper).first().simulate('blur');
    expect(findRTAddressError(wrapper).first().props().children)
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
    expect(findRTAddressError(wrapper).first().props().children)
        .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    findRTAddressInput(wrapper).first().simulate('change', {target: {value: 'correctEmail@domain.com'}});
    findRTAddressInput(wrapper).first().simulate('blur');
    expect(findRTAddressError(wrapper).exists()).toBeFalsy();
  });

  it('Should display error in case of invalid email Address Format in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    expect(findCTAddressError(wrapper).exists()).toBeFalsy();

    // In case of a single entry which is not in the correct format
    findCTAddressInput(wrapper).first().simulate('change', {target: {value: 'ctInvalidEmail@domain'}});
    findCTAddressInput(wrapper).first().simulate('blur');
    expect(findCTAddressError(wrapper).first().props().children)
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
    expect(findCTAddressError(wrapper).first().props().children)
        .toBe('Following Email Addresses are not valid : invalidEmail@domain@org , invalidEmail , justDomain.org , nope@just#plain#wrong');

    // Single correct format Email Address entries
    findCTAddressInput(wrapper).first().simulate('change', {target: {value: 'correctEmail@domain.com'}});
    findCTAddressInput(wrapper).first().simulate('blur');
    expect(findCTAddressError(wrapper).exists()).toBeFalsy();
  });

  it('Should display error in case of invalid email Domain Format in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    expect(findRTDomainError(wrapper).exists()).toBeFalsy();

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
        .simulate('change', {target: {value: 'invalidEmail@domain'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    expect(findRTDomainError(wrapper).first().props().children)
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
    expect(findRTDomainError(wrapper).first().props().children)
      .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
        'justSomeText , broadinstitute.org#wrongTest');


    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: 'domain.com'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    expect(findRTDomainError(wrapper).exists()).toBeFalsy();
  });

  it('Should display error in case of invalid email Domain Format in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    expect(findCTDomainError(wrapper).exists()).toBeFalsy();

    // Single Entry with incorrect Email Domain format
    findCTDomainInput(wrapper).first().simulate('change', {target: {value: 'invalidEmail@domain'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    expect(findCTDomainError(wrapper).first().props().children)
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
    expect(findCTDomainError(wrapper).first().props().children)
        .toBe('Following Email Domains are not valid : someEmailAddress@domain@org , ' +
            'justSomeText , broadinstitute.org#wrongTest');

    findCTDomainInput(wrapper).first().simulate('change', {target: {value: 'domain.com'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    expect(findCTDomainError(wrapper).exists()).toBeFalsy();
  });

  it('Should ignore empty string in email Domain in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    expect(findRTDomainInput(wrapper).first().prop('value'))
      .toBe('validEmail.com,\njustSomeRandom.domain');

    expect(findRTDomainError(wrapper).exists()).toBeFalsy();
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    await waitOneTickAndUpdate(wrapper);

    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findCTDomainInput(wrapper).first()
        .simulate('change', {target: {value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    expect(findCTDomainInput(wrapper).first().prop('value')).toBe('validEmail.com,\njustSomeRandom.domain');

    expect(findCTDomainError(wrapper).exists()).toBeFalsy();
  });

  it('Should ignore whitespaces in email domains in Registered Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findRTDomainInput(wrapper).first()
      .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    findRTDomainInput(wrapper).first().simulate('blur');
    expect(findRTDomainInput(wrapper).first().prop('value'))
      .toBe('someDomain.com,\njustSomeRandom.domain');

    expect(findRTDomainError(wrapper).exists()).toBeFalsy();
  });

  it('Should ignore whitespaces in email domains in Controlled Tier requirement', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    await simulateComponentChange(wrapper, findCTDropdown(wrapper), InstitutionMembershipRequirement.DOMAINS);

    // Single Entry with incorrect Email Domain format
    findCTDomainInput(wrapper).first()
        .simulate('change', {target: {value: '  someDomain.com,\njustSomeRandom.domain   ,\n,'}});
    findCTDomainInput(wrapper).first().simulate('blur');
    expect(findCTDomainInput(wrapper).first().prop('value')).toBe('someDomain.com,\njustSomeRandom.domain');

    expect(findCTDomainError(wrapper).exists()).toBeFalsy();
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

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), true);
    expect(findCTEnabled(wrapper).props.checked).toBeTruthy();
    expect(findCTDetails(wrapper).exists()).toBeTruthy();

    await simulateComponentChange(wrapper, findCTEnabled(wrapper), false);
    expect(findCTEnabled(wrapper).props.checked).toBeFalsy();
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

    await simulateComponentChange(wrapper, findRTDropdown(wrapper), InstitutionMembershipRequirement.ADDRESSES);

    expect(findRTAddressInput(wrapper).exists()).toBeTruthy();
    expect(findRTAddressInput(wrapper).first().prop('value')).toBe('');

    expect(findRTDomainInput(wrapper).exists()).toBeFalsy();
    expect(findCTAddressInput(wrapper).exists()).toBeFalsy();
    expect(findCTDomainInput(wrapper).exists()).toBeFalsy();

    findRTAddressInput(wrapper).first().simulate('change', {target: {value: 'user@domain.com'}});
    findRTAddressInput(wrapper).first().simulate('blur');

    // RT no change
    expect(findRTAddressInput(wrapper).first().prop('value')).toBe('user@domain.com');
  });
});
