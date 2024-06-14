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
import { MembershipRequirements } from './admin-institution-options';

const getRTDetails = () => screen.getByTestId('registered-card-details');
const findRTDropdown = () =>
  screen.getByTestId('registered-agreement-dropdown');

const getCTDetails = () => screen.getByTestId('controlled-card-details');
const queryCTDetails = () => screen.queryByTestId('controlled-card-details');
const getCTDropdown = () => screen.getByTestId('controlled-agreement-dropdown');
const getRTDropdown = () => screen.getByTestId('registered-agreement-dropdown');

const getCTEnabled = (): HTMLInputElement =>
  screen.getByTestId('controlled-enabled-switch');

const getRTAddress = () => screen.getByTestId('registered-email-address');
const queryRTAddress = () => screen.queryByTestId('registered-email-address');
const getRTDomain = () => screen.getByTestId('registered-email-domain');
const queryRTDomain = () => screen.queryByTestId('registered-email-domain');
const getCTAddress = () => screen.getByTestId('controlled-email-address');
const queryCTAddress = () => screen.queryByTestId('controlled-email-address');
const findCTDomain = () => screen.getByTestId('controlled-email-domain');

const getRTAddressInput = (): HTMLInputElement =>
  screen.getByTestId('registered-email-address-input');
const getRTDomainInput = (): HTMLInputElement =>
  screen.getByTestId('registered-email-domain-input');
const getCTAddressInput = () =>
  screen.getByTestId('controlled-email-address-input');
const queryCTAddressInput = () =>
  screen.queryByTestId('controlled-email-address-input');
const getCTDomainInput = (): HTMLInputElement =>
  screen.getByTestId('controlled-email-domain-input');

const getSaveButton = () =>
  screen.getByRole('button', {
    name: /save/i,
  });

const addressesRequirementLabel = MembershipRequirements.filter(
  (requirment) =>
    requirment.value === InstitutionMembershipRequirement.ADDRESSES
)[0].label;

const domainsRequirementLabel = MembershipRequirements.filter(
  (requirment) => requirment.value === InstitutionMembershipRequirement.DOMAINS
)[0].label;
/* const getRTAddressError = () =>
  getSaveButtonDisabled().registeredTierEmailAddresses;
const getRTDomainError = () =>
  getSaveButtonDisabled().registeredTierEmailDomains;
const getCTAddressError = () =>
  getSaveButtonDisabled().controlledTierEmailAddresses;
const findCTDomainError = () =>
  getSaveButtonDisabled().controlledTierEmailDomains;*/

const selectDropdownOption = async (
  dropdown: HTMLElement,
  optionText: string
) => {
  await userEvent.click(dropdown);
  const optionElement = within(dropdown).getByText(optionText);
  await userEvent.click(optionElement);
};

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
    expect(getRTDetails()).toBeInTheDocument();
  });

  it('should show CT card details when institution has controlled tier access enabled', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(getCTDetails()).toBeInTheDocument();
  });

  it('should hide CT card details when institution has controlled tier access disabled', async () => {
    componentAlt(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();
  });

  it('should hide/show CT card details when controlled tier disabled/enabled', async () => {
    componentAlt();
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
    componentAlt(VERILY_WITHOUT_CT.shortName);
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

    expect(findCTDomain()).toBeInTheDocument();
    expect(queryCTAddress()).not.toBeInTheDocument();

    expect(getCTDomainInput()).toHaveValue(testDomains);
  });

  it('should populate CT requirements from RT when enabling CT if RT matches on ADDRESS', async () => {
    const wrapper = component(VERILY_WITHOUT_CT.shortName);
    await waitForNoSpinner();
    expect(queryCTDetails()).not.toBeInTheDocument();

    // change Registered from DOMAIN to ADDRESS

    expect(getRTDomain()).toBeInTheDocument();
    expect(queryRTAddress()).not.toBeInTheDocument();

    await simulateComponentChange(
      wrapper,
      findRTDropdown(),
      InstitutionMembershipRequirement.ADDRESSES
    );

    expect(getRTAddress()).toBeInTheDocument();
    expect(getRTDomain()).not.toBeInTheDocument();

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
    expect(findCTDomain()).not.toBeInTheDocument();

    expect(getCTAddressInput()).toHaveValue('');
  });

  it('should update institution tier requirement', async () => {
    componentAlt();
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
    componentAlt();
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

  //   it('should update RT and CT requirements simultaneously when both changed', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with RT DOMAINS and CT ADDRS
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       findRTDropdown(),
  //       InstitutionMembershipRequirement.ADDRESSES
  //     );
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.DOMAINS
  //     );
  //
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'correctEmail@domain.com' } });
  //     getRTAddressInput().first().simulate('blur');
  //     expect(getRTAddressInput())).toHaveValue(
  //       'correctEmail@domain.com'
  //     );
  //
  //     // one entry has an incorrect Email Domain format (whitespace)
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: { value: 'someDomain.com,\njustSomeRandom.domain,\n,' },
  //       });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(getCTDomainInput())).toHaveValue(
  //       'someDomain.com,\njustSomeRandom.domain'
  //     );
  //   });
  //
  //   it('should show appropriate section after changing agreement type in Controlled Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with CT ADDRS
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.DOMAINS
  //     );
  //
  //     expect(getCTAddress()).not.toBeInTheDocument();
  //     expect(findCTDomain()).toBeInTheDocument();
  //
  //     const ctEmailDomainLabel = findCTDomain().first().props().children[0];
  //     expect(ctEmailDomainLabel.props.children).toHaveValue('Accepted Email Domains');
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.ADDRESSES
  //     );
  //
  //     expect(getCTAddress()).toBeInTheDocument();
  //     expect(findCTDomain()).not.toBeInTheDocument();
  //
  //     const ctEmailAddressLabel = getCTAddress().first().props().children[0];
  //     expect(ctEmailAddressLabel.props.children).toHaveValue('Accepted Email Addresses');
  //   });
  //
  //   it('Should display error in case of invalid email Address Format in Registered Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with RT DOMAINS
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       findRTDropdown(),
  //       InstitutionMembershipRequirement.ADDRESSES
  //     );
  //
  //     expect(getRTAddressError()).toBeTruthy();
  //     expect(getRTAddressError()[0]).toHaveValue(
  //       'Registered tier email addresses should not be empty'
  //     );
  //
  //     // In case of a single entry which is not in the correct format
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'rtInvalidEmail@domain' } });
  //     getRTAddressInput().first().simulate('blur');
  //
  //     expect(getRTAddressError()).toBeTruthy();
  //     expect(getRTAddressError()[0]).toHaveValue(
  //       'Registered tier email addresses are not valid: rtInvalidEmail@domain'
  //     );
  //
  //     // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', {
  //         target: {
  //           value:
  //             'invalidEmail@domain@org,\n' +
  //             'correctEmail@someDomain.org,\n' +
  //             ' correctEmail.123.hello@someDomain567.org.com   \n' +
  //             ' invalidEmail   ,\n' +
  //             ' justDomain.org,\n' +
  //             'someEmail@broadinstitute.org\n' +
  //             'nope@just#plain#wrong',
  //         },
  //       });
  //     getRTAddressInput().first().simulate('blur');
  //     expect(getRTAddressError()[0]).toHaveValue(
  //       'Registered tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
  //         'justDomain.org, nope@just#plain#wrong'
  //     );
  //
  //     // Single correct format Email Address entries
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'correctEmail@domain.com' } });
  //     getRTAddressInput().first().simulate('blur');
  //     expect(getRTAddressError()).toBeFalsy();
  //   });
  //
  //   it('Should display error in case of invalid email Address Format in Controlled Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with CT ADDRS
  //
  //     expect(getCTDetails()).toBeInTheDocument();
  //
  //     // In case of a single entry which is not in the correct format
  //     getCTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'ctInvalidEmail@domain' } });
  //     getCTAddressInput().first().simulate('blur');
  //     expect(getCTAddressError()[0]).toHaveValue(
  //       'Controlled tier email addresses are not valid: ctInvalidEmail@domain'
  //     );
  //
  //     // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
  //     getCTAddressInput()
  //       .first()
  //       .simulate('change', {
  //         target: {
  //           value:
  //             'invalidEmail@domain@org,\n' +
  //             'correctEmail@someDomain.org,\n' +
  //             ' correctEmail.123.hello@someDomain567.org.com   \n' +
  //             ' invalidEmail   ,\n' +
  //             ' justDomain.org,\n' +
  //             'someEmail@broadinstitute.org\n' +
  //             'nope@just#plain#wrong',
  //         },
  //       });
  //     getCTAddressInput().first().simulate('blur');
  //     expect(getCTAddressError()[0]).toHaveValue(
  //       'Controlled tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
  //         'justDomain.org, nope@just#plain#wrong'
  //     );
  //
  //     // Single correct format Email Address entries
  //     getCTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'correctEmail@domain.com' } });
  //     getCTAddressInput().first().simulate('blur');
  //     expect(getCTAddressError()).toBeFalsy();
  //   });
  //
  //   it('Should display error in case of invalid email Domain Format in Registered Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with RT DOMAINS
  //
  //     expect(getRTDomainError()).toBeFalsy();
  //
  //     // Single Entry with incorrect Email Domain format
  //     getRTDomainInput()
  //       .first()
  //       .simulate('change', { target: { value: 'invalidEmail@domain' } });
  //     getRTDomainInput().first().simulate('blur');
  //     expect(getRTDomainError()[0]).toHaveValue(
  //       'Registered tier email domains are not valid: invalidEmail@domain'
  //     );
  //
  //     // Multiple Entries with correct and incorrect Email Domain format
  //     getRTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: {
  //           value:
  //             'someEmailAddress@domain@org,\n' +
  //             'someDomain123.org.com        ,\n' +
  //             ' justSomeText,\n' +
  //             ' justDomain.org,\n' +
  //             'broadinstitute.org#wrongTest',
  //         },
  //       });
  //     getRTDomainInput().first().simulate('blur');
  //     expect(getRTDomainError()[0]).toHaveValue(
  //       'Registered tier email domains are not valid: someEmailAddress@domain@org, ' +
  //         'justSomeText, broadinstitute.org#wrongTest'
  //     );
  //
  //     getRTDomainInput()
  //       .first()
  //       .simulate('change', { target: { value: 'domain.com' } });
  //     getRTDomainInput().first().simulate('blur');
  //     expect(getRTDomainError()).toBeFalsy();
  //   });
  //
  //   it('Should display error in case of invalid email Domain Format in Controlled Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with CT ADDRS
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.DOMAINS
  //     );
  //
  //     expect(findCTDomainError()).toBeTruthy();
  //     expect(findCTDomainError()[0]).toHaveValue(
  //       'Controlled tier email domains should not be empty'
  //     );
  //
  //     // Single Entry with incorrect Email Domain format
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', { target: { value: 'invalidEmail@domain' } });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(findCTDomainError()[0]).toHaveValue(
  //       'Controlled tier email domains are not valid: invalidEmail@domain'
  //     );
  //
  //     // Multiple Entries with correct and incorrect Email Domain format
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: {
  //           value:
  //             'someEmailAddress@domain@org,\n' +
  //             'someDomain123.org.com        ,\n' +
  //             ' justSomeText,\n' +
  //             ' justDomain.org,\n' +
  //             'broadinstitute.org#wrongTest',
  //         },
  //       });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(findCTDomainError()[0]).toHaveValue(
  //       'Controlled tier email domains are not valid: someEmailAddress@domain@org, ' +
  //         'justSomeText, broadinstitute.org#wrongTest'
  //     );
  //
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', { target: { value: 'domain.com' } });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(findCTDomainError()).toBeFalsy();
  //   });
  //
  //   it('Should ignore empty string in email Domain in Registered Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with RT DOMAINS
  //
  //     // one entry has an incorrect Email Domain format (whitespace)
  //     getRTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: { value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,' },
  //       });
  //     getRTDomainInput().first().simulate('blur');
  //     expect(getRTDomainInput())).toHaveValue(
  //       'validEmail.com,\njustSomeRandom.domain'
  //     );
  //
  //     expect(getRTDomainError()).toBeFalsy();
  //   });
  //
  //   it('Should ignore empty string in email Domain in Controlled Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with CT ADDRS
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.DOMAINS
  //     );
  //
  //     // one entry has an incorrect Email Domain format (whitespace)
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: { value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,' },
  //       });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(getCTDomainInput())).toHaveValue(
  //       'validEmail.com,\njustSomeRandom.domain'
  //     );
  //
  //     expect(findCTDomainError()).toBeFalsy();
  //   });
  //
  //   it('Should ignore whitespaces in email domains in Registered Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with RT DOMAINS
  //
  //     // one entry has an incorrect Email Domain format (whitespace)
  //     getRTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: { value: '  someDomain.com,\njustSomeRandom.domain   ,\n,' },
  //       });
  //     getRTDomainInput().first().simulate('blur');
  //     expect(getRTDomainInput())).toHaveValue(
  //       'someDomain.com,\njustSomeRandom.domain'
  //     );
  //
  //     expect(getRTDomainError()).toBeFalsy();
  //   });
  //
  //   it('Should ignore whitespaces in email domains in Controlled Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // VERILY inst starts with CT ADDRS
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.DOMAINS
  //     );
  //
  //     // one entry has an incorrect Email Domain format (whitespace)
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: { value: '  someDomain.com,\njustSomeRandom.domain   ,\n,' },
  //       });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(getCTDomainInput())).toHaveValue(
  //       'someDomain.com,\njustSomeRandom.domain'
  //     );
  //
  //     expect(findCTDomainError()).toBeFalsy();
  //   });
  // });
  //
  // describe('AdminInstitutionEditSpec - add mode', () => {
  //   const component = () => {
  //     return mount(
  //       <MemoryRouter initialEntries={['/admin/institution/add']}>
  //         <Route path='/admin/institution/add'>
  //           <AdminInstitutionEdit hideSpinner={() => {}} showSpinner={() => {}} />
  //         </Route>
  //       </MemoryRouter>
  //     );
  //   };
  //
  //   beforeEach(() => {
  //     serverConfigStore.set({ config: defaultServerConfig });
  //     registerApiClient(InstitutionApi, new InstitutionApiStub());
  //   });
  //
  //   it('should render', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //   });
  //
  //   it('should throw error for a new Institution if the display name is more than 80 characters', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     const testInput = fp.repeat(83, 'a');
  //     const displayNameText = wrapper.find('[id="displayName"]').first();
  //     displayNameText.simulate('change', { target: { value: testInput } });
  //     displayNameText.simulate('blur');
  //     expect(
  //       screen.getByTestId("displayNameError"]').first().prop('children')
  //     ).toContain('Display name must be 80 characters or less');
  //   });
  //
  //   it('should always show RT card details', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //     expect(getRTDetails()).toBeInTheDocument();
  //   });
  //
  //   it('should not initially show CT card details', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //     expect(getCTDetails()).not.toBeInTheDocument();
  //   });
  //
  //   it('should hide/show CT card details when controlled tier enabled/disabled', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //     expect(getCTDetails()).not.toBeInTheDocument();
  //     expect(getCTEnabled().checked).toBeFalsy();
  //     toggleCheckbox(getCTEnabled());
  //     expect(getCTEnabled().checked).toBeTruthy();
  //     expect(getCTDetails()).toBeInTheDocument();
  //
  //     toggleCheckbox(getCTEnabled());
  //     expect(getCTEnabled().checked).toBeFalsy();
  //     expect(getCTDetails()).not.toBeInTheDocument();
  //
  //     // both RT and CT are uninitialized
  //     expect(getRTDomainInput()).not.toBeInTheDocument();
  //     expect(getRTAddressInput()).not.toBeInTheDocument();
  //     expect(getCTAddressInput()).not.toBeInTheDocument();
  //     expect(getCTDomainInput()).not.toBeInTheDocument();
  //   });
  //
  //   it('should update institution tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     // uninitialized
  //     expect(getRTDomainInput()).not.toBeInTheDocument();
  //     expect(getRTAddressInput()).not.toBeInTheDocument();
  //     expect(getCTAddressInput()).not.toBeInTheDocument();
  //     expect(getCTDomainInput()).not.toBeInTheDocument();
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       findRTDropdown(),
  //       InstitutionMembershipRequirement.ADDRESSES
  //     );
  //
  //     expect(getRTAddressInput()).toBeInTheDocument();
  //     expect(getRTAddressInput())).toHaveValue('');
  //
  //     expect(getRTDomainInput()).not.toBeInTheDocument();
  //     expect(getCTAddressInput()).not.toBeInTheDocument();
  //     expect(getCTDomainInput()).not.toBeInTheDocument();
  //
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'user@domain.com' } });
  //     getRTAddressInput().first().simulate('blur');
  //
  //     // RT no change
  //     expect(getRTAddressInput())).toHaveValue('user@domain.com');
  //   });
  //
  //   it('Should display error in case of invalid email Address Format in Registered Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       findRTDropdown(),
  //       InstitutionMembershipRequirement.ADDRESSES
  //     );
  //
  //     expect(getRTAddressError()).toBeTruthy();
  //     expect(getRTAddressError()[0]).toHaveValue(
  //       'Registered tier email addresses should not be empty'
  //     );
  //
  //     // In case of a single entry which is not in the correct format
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'rtInvalidEmail@domain' } });
  //     getRTAddressInput().first().simulate('blur');
  //     expect(getRTAddressError()[0]).toHaveValue(
  //       'Registered tier email addresses are not valid: rtInvalidEmail@domain'
  //     );
  //
  //     // Multiple Email Address entries with a mix of correct (someEmail@broadinstitute.org') and incorrect format
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', {
  //         target: {
  //           value:
  //             'invalidEmail@domain@org,\n' +
  //             'correctEmail@someDomain.org,\n' +
  //             ' correctEmail.123.hello@someDomain567.org.com   \n' +
  //             ' invalidEmail   ,\n' +
  //             ' justDomain.org,\n' +
  //             'someEmail@broadinstitute.org\n' +
  //             'nope@just#plain#wrong',
  //         },
  //       });
  //     getRTAddressInput().first().simulate('blur');
  //     expect(getRTAddressError()[0]).toHaveValue(
  //       'Registered tier email addresses are not valid: invalidEmail@domain@org, invalidEmail, ' +
  //         'justDomain.org, nope@just#plain#wrong'
  //     );
  //
  //     // Single correct format Email Address entries
  //     getRTAddressInput()
  //       .first()
  //       .simulate('change', { target: { value: 'correctEmail@domain.com' } });
  //     getRTAddressInput().first().simulate('blur');
  //     expect(getRTAddressError()).toBeFalsy();
  //   });
  //
  //   it('Should ignore empty string in email Domain in Controlled Tier requirement', async () => {
  //     componentAlt();
  //     await waitForNoSpinner();
  //
  //     expect(getCTEnabled().checked).toBeFalsy();
  //     await user.click(getCTEnabled());
  //     expect(getCTEnabled().checked).toBeTruthy();
  //
  //     await waitOneTickAndUpdate();
  //
  //     await simulateComponentChange(
  //       wrapper,
  //       getCTDropdown(),
  //       InstitutionMembershipRequirement.DOMAINS
  //     );
  //
  //     // one entry has an incorrect Email Domain format (whitespace)
  //     getCTDomainInput()
  //       .first()
  //       .simulate('change', {
  //         target: { value: 'validEmail.com,\n     ,\njustSomeRandom.domain,\n,' },
  //       });
  //     getCTDomainInput().first().simulate('blur');
  //     expect(getCTDomainInput())).toHaveValue(
  //       'validEmail.com,\njustSomeRandom.domain'
  //     );
  //
  //     expect(findCTDomainError()).toBeFalsy();
  //   });
});
