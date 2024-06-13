import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { mount } from 'enzyme';

import {
  InstitutionApi,
  InstitutionMembershipRequirement,
} from 'generated/fetch';

import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { getAdminUrl } from 'app/utils/institutions';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  changeInputValue,
  simulateComponentChange,
  toggleCheckbox,
  waitForNoSpinner,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  InstitutionApiStub,
  VERILY,
  VERILY_WITHOUT_CT,
} from 'testing/stubs/institution-api-stub';

import { AdminInstitutionEdit } from './admin-institution-edit';

const findRTDetails = () =>
  wrapper.find('[data-test-id="registered-card-details"]');
const findRTDropdown = () =>
  wrapper.find('[data-test-id="registered-agreement-dropdown"]').first();

const findCTDetails = () =>
  wrapper.find('[data-test-id="controlled-card-details"]');
const findCTDropdown = () =>
  wrapper.find('[data-test-id="controlled-agreement-dropdown"]').first();

const findCTEnabled = () =>
  wrapper.find('input[data-test-id="controlled-enabled-switch"]').first();

const findRTAddress = () =>
  wrapper.find('[data-test-id="registered-email-address"]');
const findRTDomain = () =>
  wrapper.find('[data-test-id="registered-email-domain"]');
const findCTAddress = () =>
  wrapper.find('[data-test-id="controlled-email-address"]');
const findCTDomain = () =>
  wrapper.find('[data-test-id="controlled-email-domain"]');

const findRTAddressInput = () =>
  wrapper.find('[data-test-id="registered-email-address-input"]');
const findRTDomainInput = () =>
  wrapper.find('[data-test-id="registered-email-domain-input"]');
const findCTAddressInput = () =>
  wrapper.find('[data-test-id="controlled-email-address-input"]');
const findCTDomainInput = () =>
  wrapper.find('[data-test-id="controlled-email-domain-input"]');

const textInputValue = () => wrapper.first().prop('value');

const findSaveButton = () =>
  wrapper.find('[data-test-id="save-institution-button"]');
const findSaveButtonDisabled = () => findSaveButton().first().props().disabled;

const findRTAddressError = () =>
  findSaveButtonDisabled().registeredTierEmailAddresses;
const findRTDomainError = () =>
  findSaveButtonDisabled().registeredTierEmailDomains;
const findCTAddressError = () =>
  findSaveButtonDisabled().controlledTierEmailAddresses;
const findCTDomainError = () =>
  findSaveButtonDisabled().controlledTierEmailDomains;

describe('AdminInstitutionEditSpec - edit mode', () => {
  let user;
  const component = (institutionShortName = VERILY.shortName) => {
    return mount(
      <MemoryRouter initialEntries={[getAdminUrl(institutionShortName)]}>
        <Route path='/admin/institution/edit/:institutionId'>
          <AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  const componentAlt = (institutionShortName = VERILY.shortName) => {
    return render(
      <MemoryRouter initialEntries={[getAdminUrl(institutionShortName)]}>
        <Route path='/admin/institution/edit/:institutionId'>
          <AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    registerApiClient(InstitutionApi, new InstitutionApiStub());
    user = userEvent.setup();
  });

  it('should render', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(screen.getByText('Institution Name')).toBeInTheDocument();
  });

  it('should throw an error for existing Institution if the display name is more than 80 characters', async () => {
    componentAlt();
    await waitForNoSpinner();
    const testInput = fp.repeat(83, 'a');
    const displayNameText: HTMLInputElement = screen.getByRole('textbox', {
      name: /institution name/i,
    });
    await changeInputValue(displayNameText, testInput, user);
    expect(
      screen.getByText('Display name must be 80 characters or less')
    ).toBeInTheDocument();
  });

  it('should always show RT card details', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(findRTDetails().exists()).toBeTruthy();
  });

  it('should show CT card details when institution has controlled tier access enabled', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(findCTDetails().exists()).toBeTruthy();
  });

  it('should hide CT card details when institution has controlled tier access disabled', async () => {
    const wrapper = component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(findCTDetails().exists()).toBeFalsy();
  });

  it('should hide/show CT card details when controlled tier disabled/enabled', async () => {
    componentAlt();
    await waitForNoSpinner();

    expect(findCTDetails().exists()).toBeTruthy();
    expect(textInputValue(findCTAddressInput())).toBe('foo@verily.com');
    expect(findCTEnabled().props().checked).toBeTruthy();
    await toggleCheckbox(findCTEnabled());

    expect(findCTEnabled().props().checked).toBeFalsy();
    expect(findCTDetails().exists()).toBeFalsy();
    expect(findCTAddressInput().exists()).toBeFalsy();

    await toggleCheckbox(findCTEnabled());

    expect(findCTEnabled().props().checked).toBeTruthy();
    expect(findCTDetails().exists()).toBeTruthy();
    expect(textInputValue(findCTAddressInput())).toBe('foo@verily.com');
  });

  it('should populate CT requirements from RT when enabling CT if RT matches on DOMAIN', async () => {
    const wrapper = component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(findCTDetails().exists()).toBeFalsy();

    // update RT domains

    const testDomains = 'domain1.com,\n' + 'domain2.com,\n' + 'domain3.com';
    findRTDomainInput()
      .first()
      .simulate('change', {
        target: {
          value: testDomains,
        },
      });
    expect(findCTEnabled().props().checked).toBeFalsy();
    await toggleCheckbox(findCTEnabled());
    expect(findCTEnabled().props().checked).toBeTruthy();
    expect(findCTDetails().exists()).toBeTruthy();

    // CT copies RT's requirements: domain, ERA = true, domain list is equal

    expect(findCTDomain().exists()).toBeTruthy();
    expect(findCTAddress().exists()).toBeFalsy();

    expect(textInputValue(findCTDomainInput())).toBe(testDomains);
  });

  it('should populate CT requirements from RT when enabling CT if RT matches on ADDRESS', async () => {
    const wrapper = component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(findCTDetails().exists()).toBeFalsy();

    // change Registered from DOMAIN to ADDRESS

    expect(findRTDomain().exists()).toBeTruthy();
    expect(findRTAddress().exists()).toBeFalsy();

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(findRTAddress().exists()).toBeTruthy();
    expect(findRTDomain().exists()).toBeFalsy();

    // update RT addresses

    findRTAddressInput()
      .first()
      .simulate('change', {
        target: {
          value:
            'test1@domain.com,\n' + 'test2@domain.com,\n' + 'test3@domain.com',
        },
      });

    expect(findCTEnabled().props().checked).toBeFalsy();
    await toggleCheckbox(findCTEnabled());
    expect(findCTEnabled().props().checked).toBeTruthy();
    expect(findCTDetails().exists()).toBeTruthy();

    // CT copies RT's requirements: address, ERA = true
    // but the CT address list is empty

    expect(findCTAddress().exists()).toBeTruthy();
    expect(findCTDomain().exists()).toBeFalsy();

    expect(textInputValue(findCTAddressInput())).toBe('');
  });

  it('should update institution tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // Value before test.
    expect(textInputValue(findRTDomainInput())).toBe('verily.com,\ngoogle.com');
    expect(textInputValue(findCTAddressInput())).toBe('foo@verily.com');

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    findCTDomainInput()
      .first()
      .simulate('change', { target: { value: 'domain.com' } });

    wrapper
      .find('[data-test-id="save-institution-button"]')
      .first()
      .simulate('click');
    // RT no change
    expect(textInputValue(findRTDomainInput())).toBe(
      'verily.com,\n' + 'google.com'
    );
    // CT changed to email domains
    expect(textInputValue(findCTDomainInput())).toBe('domain.com');
    // CT email addresses become empty
    expect(findCTAddressInput().exists()).toBeFalsy();
  });

  it('should show appropriate section after changing agreement type in Registered Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with RT = DOMAINS

    expect(findRTAddress().exists()).toBeFalsy();
    expect(findRTDomain().exists()).toBeTruthy();

    const rtEmailDomainLabel = findRTDomain().first().props().children[0];
    expect(rtEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(findRTAddress().exists()).toBeTruthy();
    expect(findRTDomain().exists()).toBeFalsy();

    const rtEmailAddressLabel = findRTAddress().first().props().children[0];
    expect(rtEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('should update RT and CT requirements simultaneously when both changed', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS and CT ADDRS

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );
    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    findRTAddressInput()
      .first()
      .simulate('change', { target: { value: 'correctEmail@domain.com' } });
    findRTAddressInput().first().simulate('blur');
    expect(textInputValue(findRTAddressInput())).toBe(
      'correctEmail@domain.com'
    );

    // one entry has an incorrect Email Domain format (whitespace)
    findCTDomainInput()
      .first()
      .simulate('change', {
        target: { value: 'someDomain.com,\njustSomeRandom.domain,\n,' },
      });
    findCTDomainInput().first().simulate('blur');
    expect(textInputValue(findCTDomainInput())).toBe(
      'someDomain.com,\njustSomeRandom.domain'
    );
  });

  it('should show appropriate section after changing agreement type in Controlled Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    expect(findCTAddress().exists()).toBeFalsy();
    expect(findCTDomain().exists()).toBeTruthy();

    const ctEmailDomainLabel = findCTDomain().first().props().children[0];
    expect(ctEmailDomainLabel.props.children).toBe('Accepted Email Domains');

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(findCTAddress().exists()).toBeTruthy();
    expect(findCTDomain().exists()).toBeFalsy();

    const ctEmailAddressLabel = findCTAddress().first().props().children[0];
    expect(ctEmailAddressLabel.props.children).toBe('Accepted Email Addresses');
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(findRTAddressError()).toBeTruthy();
    expect(findRTAddressError()[0]).toBe(
      'Registered tier email addresses should not be empty'
    );

    // In case of a single entry which is not in the correct format
    findRTAddressInput()
      .first()
      .simulate('change', { target: { value: 'rtInvalidEmail@domain' } });
    findRTAddressInput().first().simulate('blur');

    expect(findRTAddressError()).toBeTruthy();
    expect(findRTAddressError()[0]).toBe(
      'Registered tier email addresses are not valid: rtInvalidEmail@domain'
    );

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    findRTAddressInput()
      .first()
      .simulate('change', {
        target: {
          value:
            'invalidEmail@domain@org,\n' +
            'correctEmail@someDomain.org,\n' +
            ' correctEmail.123.hello@someDomain567.org.com   \n' +
            ' invalidEmail   ,\n' +
            ' justDomain.org,\n' +
            'someEmail@broadinstitute.org\n' +
            'nope@just#plain#wrong',
        },
      });
    findRTAddressInput().first().simulate('blur');
    expect(findRTAddressError()[0]).toBe(
      'Registered tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
        'justDomain.org, nope@just#plain#wrong'
    );

    // Single correct format Email Address entries
    findRTAddressInput()
      .first()
      .simulate('change', { target: { value: 'correctEmail@domain.com' } });
    findRTAddressInput().first().simulate('blur');
    expect(findRTAddressError()).toBeFalsy();
  });

  it('Should display error in case of invalid email Address Format in Controlled Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    expect(findCTDetails().exists()).toBeTruthy();

    // In case of a single entry which is not in the correct format
    findCTAddressInput()
      .first()
      .simulate('change', { target: { value: 'ctInvalidEmail@domain' } });
    findCTAddressInput().first().simulate('blur');
    expect(findCTAddressError()[0]).toBe(
      'Controlled tier email addresses are not valid: ctInvalidEmail@domain'
    );

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    findCTAddressInput()
      .first()
      .simulate('change', {
        target: {
          value:
            'invalidEmail@domain@org,\n' +
            'correctEmail@someDomain.org,\n' +
            ' correctEmail.123.hello@someDomain567.org.com   \n' +
            ' invalidEmail   ,\n' +
            ' justDomain.org,\n' +
            'someEmail@broadinstitute.org\n' +
            'nope@just#plain#wrong',
        },
      });
    findCTAddressInput().first().simulate('blur');
    expect(findCTAddressError()[0]).toBe(
      'Controlled tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
        'justDomain.org, nope@just#plain#wrong'
    );

    // Single correct format Email Address entries
    findCTAddressInput()
      .first()
      .simulate('change', { target: { value: 'correctEmail@domain.com' } });
    findCTAddressInput().first().simulate('blur');
    expect(findCTAddressError()).toBeFalsy();
  });

  it('Should display error in case of invalid email Domain Format in Registered Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    expect(findRTDomainError()).toBeFalsy();

    // Single Entry with incorrect Email Domain format
    findRTDomainInput()
      .first()
      .simulate('change', { target: { value: 'invalidEmail@domain' } });
    findRTDomainInput().first().simulate('blur');
    expect(findRTDomainError()[0]).toBe(
      'Registered tier email domains are not valid: invalidEmail@domain'
    );

    // Multiple Entries with correct and incorrect Email Domain format
    findRTDomainInput()
      .first()
      .simulate('change', {
        target: {
          value:
            'someEmailAddress@domain@org,\n' +
            'someDomain123.org.com        ,\n' +
            ' justSomeText,\n' +
            ' justDomain.org,\n' +
            'broadinstitute.org#wrongTest',
        },
      });
    findRTDomainInput().first().simulate('blur');
    expect(findRTDomainError()[0]).toBe(
      'Registered tier email domains are not valid: someEmailAddress@domain@org, ' +
        'justSomeText, broadinstitute.org#wrongTest'
    );

    findRTDomainInput()
      .first()
      .simulate('change', { target: { value: 'domain.com' } });
    findRTDomainInput().first().simulate('blur');
    expect(findRTDomainError()).toBeFalsy();
  });

  it('Should display error in case of invalid email Domain Format in Controlled Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    expect(findCTDomainError()).toBeTruthy();
    expect(findCTDomainError()[0]).toBe(
      'Controlled tier email domains should not be empty'
    );

    // Single Entry with incorrect Email Domain format
    findCTDomainInput()
      .first()
      .simulate('change', { target: { value: 'invalidEmail@domain' } });
    findCTDomainInput().first().simulate('blur');
    expect(findCTDomainError()[0]).toBe(
      'Controlled tier email domains are not valid: invalidEmail@domain'
    );

    // Multiple Entries with correct and incorrect Email Domain format
    findCTDomainInput()
      .first()
      .simulate('change', {
        target: {
          value:
            'someEmailAddress@domain@org,\n' +
            'someDomain123.org.com        ,\n' +
            ' justSomeText,\n' +
            ' justDomain.org,\n' +
            'broadinstitute.org#wrongTest',
        },
      });
    findCTDomainInput().first().simulate('blur');
    expect(findCTDomainError()[0]).toBe(
      'Controlled tier email domains are not valid: someEmailAddress@domain@org, ' +
        'justSomeText, broadinstitute.org#wrongTest'
    );

    findCTDomainInput()
      .first()
      .simulate('change', { target: { value: 'domain.com' } });
    findCTDomainInput().first().simulate('blur');
    expect(findCTDomainError()).toBeFalsy();
  });

  it('Should ignore empty string in email Domain in Registered Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    // one entry has an incorrect Email Domain format (whitespace)
    findRTDomainInput()
      .first()
      .simulate('change', {
        target: { value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,' },
      });
    findRTDomainInput().first().simulate('blur');
    expect(textInputValue(findRTDomainInput())).toBe(
      'validEmail.com,\njustSomeRandom.domain'
    );

    expect(findRTDomainError()).toBeFalsy();
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    // one entry has an incorrect Email Domain format (whitespace)
    findCTDomainInput()
      .first()
      .simulate('change', {
        target: { value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,' },
      });
    findCTDomainInput().first().simulate('blur');
    expect(textInputValue(findCTDomainInput())).toBe(
      'validEmail.com,\njustSomeRandom.domain'
    );

    expect(findCTDomainError()).toBeFalsy();
  });

  it('Should ignore whitespaces in email domains in Registered Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    // one entry has an incorrect Email Domain format (whitespace)
    findRTDomainInput()
      .first()
      .simulate('change', {
        target: { value: '  someDomain.com,\njustSomeRandom.domain   ,\n,' },
      });
    findRTDomainInput().first().simulate('blur');
    expect(textInputValue(findRTDomainInput())).toBe(
      'someDomain.com,\njustSomeRandom.domain'
    );

    expect(findRTDomainError()).toBeFalsy();
  });

  it('Should ignore whitespaces in email domains in Controlled Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    // one entry has an incorrect Email Domain format (whitespace)
    findCTDomainInput()
      .first()
      .simulate('change', {
        target: { value: '  someDomain.com,\njustSomeRandom.domain   ,\n,' },
      });
    findCTDomainInput().first().simulate('blur');
    expect(textInputValue(findCTDomainInput())).toBe(
      'someDomain.com,\njustSomeRandom.domain'
    );

    expect(findCTDomainError()).toBeFalsy();
  });
});

describe('AdminInstitutionEditSpec - add mode', () => {
  const component = () => {
    return mount(
      <MemoryRouter initialEntries={['/admin/institution/add']}>
        <Route path='/admin/institution/add'>
          <AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async () => {
    componentAlt();
    await waitForNoSpinner();
  });

  it('should throw error for a new Institution if the display name is more than 80 characters', async () => {
    componentAlt();
    await waitForNoSpinner();

    const testInput = fp.repeat(83, 'a');
    const displayNameText = wrapper.find('[id="displayName"]').first();
    displayNameText.simulate('change', { target: { value: testInput } });
    displayNameText.simulate('blur');
    expect(
      wrapper.find('[data-test-id="displayNameError"]').first().prop('children')
    ).toContain('Display name must be 80 characters or less');
  });

  it('should always show RT card details', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(findRTDetails().exists()).toBeTruthy();
  });

  it('should not initially show CT card details', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(findCTDetails().exists()).toBeFalsy();
  });

  it('should hide/show CT card details when controlled tier enabled/disabled', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(findCTDetails().exists()).toBeFalsy();
    expect(findCTEnabled().props().checked).toBeFalsy();
    toggleCheckbox(findCTEnabled());
    expect(findCTEnabled().props().checked).toBeTruthy();
    expect(findCTDetails().exists()).toBeTruthy();

    toggleCheckbox(findCTEnabled());
    expect(findCTEnabled().props().checked).toBeFalsy();
    expect(findCTDetails().exists()).toBeFalsy();

    // both RT and CT are uninitialized
    expect(findRTDomainInput().exists()).toBeFalsy();
    expect(findRTAddressInput().exists()).toBeFalsy();
    expect(findCTAddressInput().exists()).toBeFalsy();
    expect(findCTDomainInput().exists()).toBeFalsy();
  });

  it('should update institution tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    // uninitialized
    expect(findRTDomainInput().exists()).toBeFalsy();
    expect(findRTAddressInput().exists()).toBeFalsy();
    expect(findCTAddressInput().exists()).toBeFalsy();
    expect(findCTDomainInput().exists()).toBeFalsy();

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(findRTAddressInput().exists()).toBeTruthy();
    expect(textInputValue(findRTAddressInput())).toBe('');

    expect(findRTDomainInput().exists()).toBeFalsy();
    expect(findCTAddressInput().exists()).toBeFalsy();
    expect(findCTDomainInput().exists()).toBeFalsy();

    findRTAddressInput()
      .first()
      .simulate('change', { target: { value: 'user@domain.com' } });
    findRTAddressInput().first().simulate('blur');

    // RT no change
    expect(textInputValue(findRTAddressInput())).toBe('user@domain.com');
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(findRTAddressError()).toBeTruthy();
    expect(findRTAddressError()[0]).toBe(
      'Registered tier email addresses should not be empty'
    );

    // In case of a single entry which is not in the correct format
    findRTAddressInput()
      .first()
      .simulate('change', { target: { value: 'rtInvalidEmail@domain' } });
    findRTAddressInput().first().simulate('blur');
    expect(findRTAddressError()[0]).toBe(
      'Registered tier email addresses are not valid: rtInvalidEmail@domain'
    );

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    findRTAddressInput()
      .first()
      .simulate('change', {
        target: {
          value:
            'invalidEmail@domain@org,\n' +
            'correctEmail@someDomain.org,\n' +
            ' correctEmail.123.hello@someDomain567.org.com   \n' +
            ' invalidEmail   ,\n' +
            ' justDomain.org,\n' +
            'someEmail@broadinstitute.org\n' +
            'nope@just#plain#wrong',
        },
      });
    findRTAddressInput().first().simulate('blur');
    expect(findRTAddressError()[0]).toBe(
      'Registered tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
        'justDomain.org, nope@just#plain#wrong'
    );

    // Single correct format Email Address entries
    findRTAddressInput()
      .first()
      .simulate('change', { target: { value: 'correctEmail@domain.com' } });
    findRTAddressInput().first().simulate('blur');
    expect(findRTAddressError()).toBeFalsy();
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async () => {
    componentAlt();
    await waitForNoSpinner();

    expect(findCTEnabled().props().checked).toBeFalsy();
    await toggleCheckbox(findCTEnabled());
    expect(findCTEnabled().props().checked).toBeTruthy();

    await waitOneTickAndUpdate();

    await simulateComponentChange(
      wrapper,
      findCTDropdown(),
      InstitutionMembershipRequirement.DOMAINS
    );

    // one entry has an incorrect Email Domain format (whitespace)
    findCTDomainInput()
      .first()
      .simulate('change', {
        target: { value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,' },
      });
    findCTDomainInput().first().simulate('blur');
    expect(textInputValue(findCTDomainInput())).toBe(
      'validEmail.com,\njustSomeRandom.domain'
    );

    expect(findCTDomainError()).toBeFalsy();
  });
});
