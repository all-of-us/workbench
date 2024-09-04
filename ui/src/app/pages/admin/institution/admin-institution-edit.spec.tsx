import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  InstitutionApi,
  InstitutionMembershipRequirement,
} from 'generated/fetch';

import { render, screen, within } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { getAdminUrl } from 'app/utils/institutions';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  changeInputValue,
  expectTooltip,
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import {
  InstitutionApiStub,
  VERILY,
  VERILY_WITHOUT_CT,
} from 'testing/stubs/institution-api-stub';

import { AdminInstitutionEdit } from './admin-institution-edit';
import { MembershipRequirements } from './admin-institution-options';

const getCTAddress = () => screen.getByTestId('controlled-email-address');
const getCTAddressInput = (): HTMLInputElement =>
  screen.getByTestId('controlled-email-address-input');
const getCTDetails = () => screen.getByTestId('controlled-card-details');
const getCTDomain = () => screen.getByTestId('controlled-email-domain');
const getCTDomainInput = (): HTMLInputElement =>
  screen.getByTestId('controlled-email-domain-input');
const getCTDropdown = () => screen.getByTestId('controlled-agreement-dropdown');
const getCTEnabled = (): HTMLInputElement =>
  screen.getByTestId('controlled-enabled-switch');

const getRTAddress = () => screen.getByTestId('registered-email-address');
const getRTAddressInput = (): HTMLInputElement =>
  screen.getByTestId('registered-email-address-input');
const getRTDetails = () => screen.getByTestId('registered-card-details');
const getRTDomain = () => screen.getByTestId('registered-email-domain');
const getRTDomainInput = (): HTMLInputElement =>
  screen.getByTestId('registered-email-domain-input');
const getRTDropdown = () => screen.getByTestId('registered-agreement-dropdown');

const queryCTAddress = () => screen.queryByTestId('controlled-email-address');
const queryCTAddressInput = () =>
  screen.queryByTestId('controlled-email-address-input');
const queryCTDetails = () => screen.queryByTestId('controlled-card-details');
const queryCTDomain = () => screen.queryByTestId('controlled-email-domain');
const queryCTDomainInput = (): HTMLInputElement =>
  screen.queryByTestId('controlled-email-domain-input');

const queryRTAddress = () => screen.queryByTestId('registered-email-address');
const queryRTAddressInput = (): HTMLInputElement =>
  screen.queryByTestId('registered-email-address-input');
const queryRTDomain = () => screen.queryByTestId('registered-email-domain');
const queryRTDomainInput = (): HTMLInputElement =>
  screen.queryByTestId('registered-email-domain-input');

const getAddButton = () =>
  screen.getByRole('button', {
    name: /add/i,
  });
const getSaveButton = () =>
  screen.getByRole('button', {
    name: /save/i,
  });

const addressesRequirementLabel = MembershipRequirements.filter(
  (requirement) =>
    requirement.value === InstitutionMembershipRequirement.ADDRESSES
)[0].label;

const domainsRequirementLabel = MembershipRequirements.filter(
  (requirment) => requirment.value === InstitutionMembershipRequirement.DOMAINS
)[0].label;

const selectDropdownOption = async (
  dropdown: HTMLElement,
  optionText: string
) => {
  await userEvent.click(dropdown);
  const optionElement = within(dropdown).getByText(optionText);
  await userEvent.click(optionElement);
};

export const expectTooltipAbsence = async (
  element: HTMLElement,
  user: UserEvent
) => {
  await user.hover(element);
  expect(
    screen.queryByText(/Please correct the following errors/i)
  ).not.toBeInTheDocument();
  await user.unhover(element);
};

describe('AdminInstitutionEditSpec - edit mode', () => {
  let user: UserEvent;

  const component = (institutionShortName = VERILY.shortName) => {
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
    component();
    await waitForNoSpinner();
    expect(screen.getByText('Institution Name')).toBeInTheDocument();
  });

  it('should throw an error for existing Institution if the display name is more than 80 characters', async () => {
    component();
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
    component();
    await waitForNoSpinner();
    expect(getRTDetails()).toBeInTheDocument();
  });

  it('should show CT card details when institution has controlled tier access enabled', async () => {
    component();
    await waitForNoSpinner();
    expect(getCTDetails()).toBeInTheDocument();
  });

  it('should hide CT card details when institution has controlled tier access disabled', async () => {
    component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();
  });

  it('should hide/show CT card details when controlled tier disabled/enabled', async () => {
    component();
    await waitForNoSpinner();

    expect(getCTDetails()).toBeInTheDocument();
    expect(getCTAddressInput()).toHaveValue('foo@verily.com');
    expect(getCTEnabled().checked).toBeTruthy();
    await user.click(getCTEnabled());

    expect(getCTEnabled().checked).toBeFalsy();
    expect(queryCTDetails()).not.toBeInTheDocument();
    expect(queryCTAddressInput()).not.toBeInTheDocument();

    await user.click(getCTEnabled());

    expect(getCTEnabled().checked).toBeTruthy();
    expect(getCTDetails()).toBeInTheDocument();
    expect(getCTAddressInput()).toHaveValue('foo@verily.com');
  });

  it('should populate CT requirements from RT when enabling CT if RT matches on DOMAIN', async () => {
    component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();

    // update RT domains

    const testDomains = 'domain1.com,\n' + 'domain2.com,\n' + 'domain3.com';
    await changeInputValue(getRTDomainInput(), testDomains, user);
    expect(getCTEnabled().checked).toBeFalsy();
    await user.click(getCTEnabled());
    expect(getCTEnabled().checked).toBeTruthy();
    expect(getCTDetails()).toBeInTheDocument();

    // CT copies RT's requirements: domain, ERA = true, domain list is equal

    expect(getCTDomain()).toBeInTheDocument();
    expect(queryCTAddress()).not.toBeInTheDocument();

    expect(getCTDomainInput()).toHaveValue(testDomains);
  });

  it('should populate CT requirements from RT when enabling CT if RT matches on ADDRESS', async () => {
    component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();

    // change Registered from DOMAIN to ADDRESS

    expect(getRTDomain()).toBeInTheDocument();
    expect(queryRTAddress()).not.toBeInTheDocument();

    await selectDropdownOption(getRTDropdown(), addressesRequirementLabel);

    expect(getRTAddress()).toBeInTheDocument();
    expect(queryRTDomain()).not.toBeInTheDocument();

    // update RT addresses
    await changeInputValue(
      getRTAddressInput(),
      'test1@domain.com,\n' + 'test2@domain.com,\n' + 'test3@domain.com',
      user
    );

    expect(getCTEnabled().checked).toBeFalsy();
    await user.click(getCTEnabled());
    expect(getCTEnabled().checked).toBeTruthy();
    expect(getCTDetails()).toBeInTheDocument();

    // CT copies RT's requirements: address, ERA = true
    // but the CT address list is empty

    expect(getCTAddress()).toBeInTheDocument();
    expect(queryCTDomain()).not.toBeInTheDocument();

    expect(getCTAddressInput()).toHaveValue('');
  });

  it('should update institution tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // Value before test.
    expect(getRTDomainInput()).toHaveValue('verily.com,\ngoogle.com');
    expect(getCTAddressInput()).toHaveValue('foo@verily.com');

    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);

    await changeInputValue(getCTDomainInput(), 'domain.com', user);

    await user.click(getSaveButton());

    // RT no change
    expect(getRTDomainInput()).toHaveValue('verily.com,\n' + 'google.com');
    // CT changed to email domains
    expect(getCTDomainInput()).toHaveValue('domain.com');
    // CT email addresses become empty
    expect(queryCTAddressInput()).not.toBeInTheDocument();
  });

  it('should show appropriate section after changing agreement type in Registered Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with RT = DOMAINS

    expect(queryRTAddress()).not.toBeInTheDocument();
    expect(getRTDomain()).toBeInTheDocument();

    expect(
      within(getRTDetails()).getByText('Accepted Email Domains')
    ).toBeInTheDocument();

    await selectDropdownOption(getRTDropdown(), addressesRequirementLabel);

    expect(getRTAddress()).toBeInTheDocument();
    expect(queryRTDomain()).not.toBeInTheDocument();

    expect(
      within(getRTDetails()).getByText('Accepted Email Addresses')
    ).toBeInTheDocument();
  });

  it('should update RT and CT requirements simultaneously when both changed', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS and CT ADDRS

    await selectDropdownOption(getRTDropdown(), addressesRequirementLabel);
    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);

    await changeInputValue(
      getRTAddressInput(),
      'correctEmail@domain.com',
      user
    );

    expect(getRTAddressInput()).toHaveValue('correctEmail@domain.com');

    // one entry has an incorrect Email Domain format (whitespace)
    await changeInputValue(
      getCTDomainInput(),
      'someDomain.com,\njustSomeRandom.domain,\n,',
      user
    );
    expect(getCTDomainInput()).toHaveValue(
      'someDomain.com,\njustSomeRandom.domain'
    );
  });

  it('should show appropriate section after changing agreement type in Controlled Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS
    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);

    expect(queryCTAddress()).not.toBeInTheDocument();
    expect(getCTDomain()).toBeInTheDocument();

    expect(
      within(getCTDetails()).getByText('Accepted Email Domains')
    ).toBeInTheDocument();

    await selectDropdownOption(getCTDropdown(), addressesRequirementLabel);

    expect(getCTAddress()).toBeInTheDocument();
    expect(queryCTDomain()).not.toBeInTheDocument();

    expect(
      within(getCTDetails()).getByText('Accepted Email Addresses')
    ).toBeInTheDocument();
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    await selectDropdownOption(getRTDropdown(), addressesRequirementLabel);

    await expectTooltip(
      getSaveButton(),
      'Registered tier email addresses should not be empty',
      user
    );
    // In case of a single entry which is not in the correct format
    changeInputValue(getRTAddressInput(), 'rtInvalidEmail@domain', user);

    await expectTooltip(
      getSaveButton(),
      'Registered tier email addresses are not valid: rtInvalidEmail@domain',
      user
    );

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    changeInputValue(
      getRTAddressInput(),
      'invalidEmail@domain@org,\n' +
        'correctEmail@someDomain.org,\n' +
        ' correctEmail.123.hello@someDomain567.org.com   \n' +
        ' invalidEmail   ,\n' +
        ' justDomain.org,\n' +
        'someEmail@broadinstitute.org\n' +
        'nope@just#plain#wrong',
      user
    );

    await expectTooltip(
      getSaveButton(),
      'Registered tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
        'justDomain.org, nope@just#plain#wrong',
      user
    );

    // Single correct format Email Address entries
    changeInputValue(getRTAddressInput(), 'correctEmail@domain.com', user);
    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should display error in case of invalid email Address Format in Controlled Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    expect(getCTDetails()).toBeInTheDocument();

    // In case of a single entry which is not in the correct format
    await changeInputValue(getCTAddressInput(), 'ctInvalidEmail@domain', user);

    await expectTooltip(
      getSaveButton(),
      'Controlled tier email addresses are not valid: ctInvalidEmail@domain',
      user
    );

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    await changeInputValue(
      getCTAddressInput(),
      'invalidEmail@domain@org,\n' +
        'correctEmail@someDomain.org,\n' +
        ' correctEmail.123.hello@someDomain567.org.com   \n' +
        ' invalidEmail   ,\n' +
        ' justDomain.org,\n' +
        'someEmail@broadinstitute.org\n' +
        'nope@just#plain#wrong',
      user
    );

    await expectTooltip(
      getSaveButton(),
      'Controlled tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
        'justDomain.org, nope@just#plain#wrong',
      user
    );

    // Single correct format Email Address entries
    await changeInputValue(
      getCTAddressInput(),
      'correctEmail@domain.com',
      user
    );
    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should display error in case of invalid email Domain Format in Registered Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    await expectTooltipAbsence(getSaveButton(), user);

    // Single Entry with incorrect Email Domain format
    await changeInputValue(getRTDomainInput(), 'invalidEmail@domain', user);

    await expectTooltip(
      getSaveButton(),
      'Registered tier email domains are not valid: invalidEmail@domain',
      user
    );

    // Multiple Entries with correct and incorrect Email Domain format
    await changeInputValue(
      getRTDomainInput(),
      'someEmailAddress@domain@org,\n' +
        'someDomain123.org.com        ,\n' +
        ' justSomeText,\n' +
        ' justDomain.org,\n' +
        'broadinstitute.org#wrongTest',
      user
    );
    await expectTooltip(
      getSaveButton(),
      'Registered tier email domains are not valid: someEmailAddress@domain@org, ' +
        'justSomeText, broadinstitute.org#wrongTest',
      user
    );

    await changeInputValue(getRTDomainInput(), 'domain.com', user);
    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should display error in case of invalid email Domain Format in Controlled Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);

    await expectTooltip(
      getSaveButton(),
      'Controlled tier email domains should not be empty',
      user
    );

    // Single Entry with incorrect Email Domain format
    await changeInputValue(getCTDomainInput(), 'invalidEmail@domain', user);
    await expectTooltip(
      getSaveButton(),
      'Controlled tier email domains are not valid: invalidEmail@domain',
      user
    );

    // Multiple Entries with correct and incorrect Email Domain format
    await changeInputValue(
      getCTDomainInput(),
      'someEmailAddress@domain@org,\n' +
        'someDomain123.org.com        ,\n' +
        ' justSomeText,\n' +
        ' justDomain.org,\n' +
        'broadinstitute.org#wrongTest',
      user
    );
    await expectTooltip(
      getSaveButton(),
      'Controlled tier email domains are not valid: someEmailAddress@domain@org, ' +
        'justSomeText, broadinstitute.org#wrongTest',
      user
    );

    await changeInputValue(getCTDomainInput(), 'domain.com', user);
    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should ignore empty string in email Domain in Registered Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    // one entry has an incorrect Email Domain format (whitespace)
    await changeInputValue(
      getRTDomainInput(),
      'validEmail.com,\n     ,\njustSomeRandom.domain,\n,',
      user
    );
    expect(getRTDomainInput()).toHaveValue(
      'validEmail.com,\njustSomeRandom.domain'
    );

    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS
    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);

    // one entry has an incorrect Email Domain format (whitespace)
    await changeInputValue(
      getCTDomainInput(),
      'validEmail.com,\n     ,\njustSomeRandom.domain,\n,',
      user
    );
    expect(getCTDomainInput()).toHaveValue(
      'validEmail.com,\njustSomeRandom.domain'
    );

    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should ignore whitespaces in email domains in Registered Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with RT DOMAINS

    // one entry has an incorrect Email Domain format (whitespace)
    await changeInputValue(
      getRTDomainInput(),
      '  someDomain.com,\njustSomeRandom.domain   ,\n,',
      user
    );
    expect(getRTDomainInput()).toHaveValue(
      'someDomain.com,\njustSomeRandom.domain'
    );

    await expectTooltipAbsence(getSaveButton(), user);
  });

  it('Should ignore whitespaces in email domains in Controlled Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // VERILY inst starts with CT ADDRS

    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);

    // one entry has an incorrect Email Domain format (whitespace)
    await changeInputValue(
      getCTDomainInput(),
      '  someDomain.com,\njustSomeRandom.domain   ,\n,',
      user
    );
    expect(getCTDomainInput()).toHaveValue(
      'someDomain.com,\njustSomeRandom.domain'
    );

    await expectTooltipAbsence(getSaveButton(), user);
  });
});

describe('AdminInstitutionEditSpec - add mode', () => {
  let user: UserEvent;

  const component = () => {
    return render(
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
    user = userEvent.setup();
  });

  it('should render', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByText('Institution Name')).toBeInTheDocument();
  });

  it('should throw error for a new Institution if the display name is more than 80 characters', async () => {
    component();
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
    component();
    await waitForNoSpinner();
    expect(getRTDetails()).toBeInTheDocument();
  });

  it('should not initially show CT card details', async () => {
    component();
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();
  });

  it('should hide/show CT card details when controlled tier enabled/disabled', async () => {
    component();
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();
    expect(getCTEnabled().checked).toBeFalsy();
    await user.click(getCTEnabled());
    expect(getCTEnabled().checked).toBeTruthy();
    expect(getCTDetails()).toBeInTheDocument();
    await user.click(getCTEnabled());
    expect(getCTEnabled().checked).toBeFalsy();
    expect(queryCTDetails()).not.toBeInTheDocument();

    // both RT and CT are uninitialized
    expect(queryRTDomainInput()).not.toBeInTheDocument();
    expect(queryRTAddressInput()).not.toBeInTheDocument();
    expect(queryCTAddressInput()).not.toBeInTheDocument();
    expect(queryCTDomainInput()).not.toBeInTheDocument();
  });

  it('should update institution tier requirement', async () => {
    component();
    await waitForNoSpinner();

    // uninitialized
    expect(queryRTDomainInput()).not.toBeInTheDocument();
    expect(queryRTAddressInput()).not.toBeInTheDocument();
    expect(queryCTAddressInput()).not.toBeInTheDocument();
    expect(queryCTDomainInput()).not.toBeInTheDocument();

    await selectDropdownOption(getRTDropdown(), addressesRequirementLabel);

    expect(getRTAddressInput()).toBeInTheDocument();
    expect(getRTAddressInput()).toHaveValue('');

    expect(queryRTDomainInput()).not.toBeInTheDocument();
    expect(queryCTAddressInput()).not.toBeInTheDocument();
    expect(queryCTDomainInput()).not.toBeInTheDocument();

    await changeInputValue(getRTAddressInput(), 'user@domain.com', user);

    // RT no change
    expect(getRTAddressInput()).toHaveValue('user@domain.com');
  });

  it('Should display error in case of invalid email Address Format in Registered Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    await selectDropdownOption(getRTDropdown(), addressesRequirementLabel);
    await expectTooltip(
      getAddButton(),
      'Registered tier email addresses should not be empty',
      user
    );

    // In case of a single entry which is not in the correct format
    await changeInputValue(getRTAddressInput(), 'rtInvalidEmail@domain', user);
    await expectTooltip(
      getAddButton(),
      'Registered tier email addresses are not valid: rtInvalidEmail@domain',
      user
    );

    // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
    await changeInputValue(
      getRTAddressInput(),
      'invalidEmail@domain@org,\n' +
        'correctEmail@someDomain.org,\n' +
        ' correctEmail.123.hello@someDomain567.org.com   \n' +
        ' invalidEmail   ,\n' +
        ' justDomain.org,\n' +
        'someEmail@broadinstitute.org\n' +
        'nope@just#plain#wrong',
      user
    );
    await expectTooltip(
      getAddButton(),
      'Registered tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
        'justDomain.org, nope@just#plain#wrong',
      user
    );

    // Single correct format Email Address entries
    await changeInputValue(
      getRTAddressInput(),
      'correctEmail@domain.com',
      user
    );

    await user.hover(getAddButton());
    expect(
      screen.queryByText(/Registered tier email addresses/i)
    ).not.toBeInTheDocument();
    await user.unhover(getAddButton());
  });

  it('Should ignore empty string in email Domain in Controlled Tier requirement', async () => {
    component();
    await waitForNoSpinner();

    expect(getCTEnabled().checked).toBeFalsy();
    await user.click(getCTEnabled());
    expect(getCTEnabled().checked).toBeTruthy();
    expect(getCTDetails()).toBeInTheDocument();
    await selectDropdownOption(getCTDropdown(), domainsRequirementLabel);
    // one entry has an incorrect Email Domain format (whitespace)
    await changeInputValue(
      getCTDomainInput(),
      'validEmail.com,\n     ,\njustSomeRandom.domain,\n,',
      user
    );

    expect(getCTDomainInput()).toHaveValue(
      'validEmail.com,\njustSomeRandom.domain'
    );

    await user.hover(getAddButton());
    expect(
      screen.queryByText(/Controlled tier email domains/i)
    ).not.toBeInTheDocument();
    await user.unhover(getAddButton());
  });
});
