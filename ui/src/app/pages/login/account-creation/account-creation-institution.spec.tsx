import '@testing-library/jest-dom';

import { InstitutionalRole, InstitutionApi, Profile } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AccountCreationInstitution } from 'app/pages/login/account-creation/account-creation-institution';
import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import {
  institutionApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  BROAD,
  defaultInstitutions,
  InstitutionApiStub,
  VERILY_WITHOUT_CT,
} from 'testing/stubs/institution-api-stub';

const profile: Profile = {
  generalDiscoverySources: undefined,
  partnerDiscoverySources: undefined,
  username: '',
  contactEmail: 'contactEmail@broadinstitute.org',
  verifiedInstitutionalAffiliation: {
    institutionShortName: BROAD.shortName,
    institutionDisplayName: BROAD.displayName,
    institutionalRoleEnum: InstitutionalRole.FELLOW,
  },
};
const setup = (profileArg = profile) => {
  return {
    container: render(
      <AccountCreationInstitution
        profile={profileArg}
        onComplete={() => {}}
        onPreviousClick={() => {}}
      />
    ).container,
    user: userEvent.setup(),
  };
};

const waitForSpinnerToGoAway = async (container) => {
  await waitFor(() =>
    container.querySelector(
      '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
    )
  );
};
describe('Account Creation- Institution', () => {
  beforeEach(() => {
    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render and display default values', async () => {
    const { container } = setup();
    await waitForSpinnerToGoAway(container);
    // Static text: Headers
    expect(screen.getByText('Create your account')).toBeInTheDocument();
    expect(screen.getByText('Please complete Step 1')).toBeInTheDocument();
    expect(screen.getByText('Select your institution')).toBeInTheDocument();

    // Default values of institution, contact email and role are selected
    const dropDownDefaultValue = screen.getByText('Broad Institute');
    expect(dropDownDefaultValue).toBeInTheDocument();

    expect(
      screen.getByDisplayValue(/contactemail@broadinstitute\.org/i)
    ).toBeInTheDocument();

    expect(
      screen.getByRole('combobox', {
        hidden: true,
      }).textContent
    ).toBe(
      'Research fellow (a post-doctoral fellow or medical resident in training)'
    );
  });

  it('should load dropdown with institutions list and institutions Role list', async () => {
    const mockGetPublicInstitutionDetails = jest.spyOn(
      institutionApi(),
      'getPublicInstitutionDetails'
    );
    const { container, user } = setup();
    await waitForSpinnerToGoAway(container);

    expect(mockGetPublicInstitutionDetails).toHaveBeenCalled();

    const defaultInstitutionNameList = defaultInstitutions.map(
      (institution) => institution.displayName
    );

    const institutionDropDown = screen.getByText('Broad Institute');
    await user.click(institutionDropDown);

    const dropdownOptions = document.querySelector('.react-select__menu-list');

    // Dropdown options should match defaultInstitution list
    expect(dropdownOptions.children.length).toBe(
      defaultInstitutionNameList.length
    );

    // Find and click the institution Role dropdown
    await userEvent.click(
      within(
        screen.getByDisplayValue(
          'Research fellow (a post-doctoral fellow or medical resident in training)'
        ).parentElement.parentElement
      ).getByRole('button')
    );

    // Check institution Role drop down lists
    const mockInstOpt = AccountCreationOptions.institutionalRoleOptions;
    const institutionRoleOptionsHTMLElement = within(
      screen.getByRole('listbox', { hidden: true })
    ).getAllByRole('option', { hidden: true });

    const institutionRoleOptionsText = institutionRoleOptionsHTMLElement.map(
      (role) => role.textContent
    );
    expect(institutionRoleOptionsText.length).toBe(mockInstOpt.length);
    for (let index = 0; index < mockInstOpt.length; index++) {
      expect(institutionRoleOptionsText.includes(mockInstOpt[index].value));
    }
  });

  it('should show user-facing error message on data load error', async () => {
    // Mock institution detail api call to return error
    const mockGetPublicInstitutionDetails = jest.spyOn(
      institutionApi(),
      'getPublicInstitutionDetails'
    );
    mockGetPublicInstitutionDetails.mockRejectedValueOnce(
      new Response(null, { status: 500 })
    );
    const { container } = setup();

    await waitForSpinnerToGoAway(container);

    // Confirm it shows error after spinner goes away
    expect(
      screen.getByText(
        /an error occurred loading the institution list\. please try again or contact\./i
      )
    ).toBeInTheDocument();
  });

  it('should reset role value & options when institution is selected', async () => {
    const { container, user } = setup();

    await waitForSpinnerToGoAway(container);

    // Confirm: Institution Role is already populated
    const institutionRole = screen.getByDisplayValue(
      /research fellow \(a post\-doctoral fellow or medical resident in training\)/i
    );
    expect(institutionRole).toBeInTheDocument();

    // Change the institution Role and confirm role has been changed
    await userEvent.click(
      within(institutionRole.parentElement.parentElement).getByRole('button')
    );
    await userEvent.click(
      await screen.findByText('Early career tenure-track researcher')
    );
    await user.tab();

    const newInstitutionRole = screen.getByDisplayValue(
      /early career tenure\-track researcher/i
    );
    expect(newInstitutionRole).toBeInTheDocument();

    // Now: Change institution
    const dropDownDefaultValue = screen.getByText('Broad Institute');
    await user.click(dropDownDefaultValue);
    await user.paste('Verily LLC');
    await user.tab();

    // Expect institution role to be NOT selected
    const selectRole = screen.getByDisplayValue('Select Role');
    expect(selectRole).toBeInTheDocument();
  });

  it('should validate email affiliation when inst and email address are specified', async () => {
    const { container, user } = setup();
    await waitForSpinnerToGoAway(container);

    // Broad Institution has email authorized to speicific email address
    const contactEmail = screen.getByDisplayValue(
      /contactemail@broadinstitute\.org/i
    );

    await user.click(contactEmail);
    await user.paste('someAnotherDomain@broadinstitute.org');
    await user.tab();

    const emailError = screen.getByText(
      /the institution has authorized access only to select members\.please to request to be added to the institution/i
    );
    expect(emailError).toBeInTheDocument();
  });

  it('should validate email affiliation when inst and email domain are specified', async () => {
    const updatedProfile = {
      ...profile,
      contactEmail: 'someemail@google.com',
      verifiedInstitutionalAffiliation: {
        ...profile.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY_WITHOUT_CT.shortName,
        institutionDisplayName: VERILY_WITHOUT_CT.displayName,
      },
    };
    const { container, user } = setup(updatedProfile);
    await waitForSpinnerToGoAway(container);

    // Confirm: Verily institution is valid for all users with google domain
    const contactEmail = screen.getByDisplayValue(/someemail@google\.com/i);

    await user.click(contactEmail);
    await user.clear(contactEmail);
    await user.paste('someOthrEmail@google.com');
    await user.tab();

    // Should not display error as the new user id has google domain
    const emailError1 = screen.queryByText(
      /your email does not match your institution/i
    );
    expect(emailError1).toBeNull();
    //  expect(emailError1).not.toBeInTheDocument();

    // Change user email to another domain and we should see error message then
    await user.click(contactEmail);
    await user.clear(contactEmail);
    await user.paste('someOthrEmail@someOtherDomain.com');
    await user.tab();
    const emailError = screen.getByText(
      /your email does not match your institution/i
    );
    expect(emailError).toBeInTheDocument();

    await user.click(contactEmail);
    await user.clear(contactEmail);
    await user.paste('someOthrEmail@verily.com');
    await user.tab();

    // verily is also a valid domain for the institution so confirm no error
    expect(
      screen.queryByText(/your email does not match your institution/i)
    ).toBeNull();
  });

  it('should display appropriate icons if email is valid/invalid', async () => {
    const { container, user } = setup();
    await waitForSpinnerToGoAway(container);

    let icons = screen
      .getByLabelText('emailvalidation')
      .firstElementChild.getAttribute('shape');

    expect(icons).toEqual('success-standard');
    const contactEmail = screen.getByDisplayValue(
      /contactemail@broadinstitute\.org/i
    );

    // Email someAnotherDomain@broadinstitute.org is not valid for Broad Institute hence show warning
    await user.click(contactEmail);
    await user.paste('someAnotherDomain@broadinstitute.org');
    await user.tab();
    icons = screen
      .getByLabelText('emailvalidation')
      .firstElementChild.getAttribute('shape');

    expect(icons).toEqual('warning-standard');
  });

  it('Should disable Next button until all required fields are populated and valid', async () => {
    const { container, user } = setup();
    await waitForSpinnerToGoAway(container);

    let nextButton = screen.getByRole('button', { name: /next/i });
    expectButtonElementEnabled(nextButton);
    // Change institution
    const dropDownDefaultValue = screen.getByText('Broad Institute');
    await user.click(dropDownDefaultValue);
    await user.paste('Verily LLC');
    await user.tab();

    // Next button should be disabled since institution change caused role to be reset
    nextButton = screen.getByRole('button', { name: /next/i });
    expectButtonElementDisabled(nextButton);

    await userEvent.click(
      within(
        screen.getByDisplayValue('Select Role').parentElement.parentElement
      ).getByRole('button')
    );
    await userEvent.click(
      await screen.findByText('Early career tenure-track researcher')
    );
    await user.tab();
    const newInstitutionRole = screen.getByDisplayValue(
      /early career tenure\-track researcher/i
    );
    expect(newInstitutionRole).toBeInTheDocument();

    // Next button is still disabled as  we change the institution so email is not valid
    nextButton = screen.getByRole('button', { name: /next/i });
    expectButtonElementDisabled(nextButton);

    const email = screen.getByDisplayValue(/contactemail@broadinstitute\.org/i);
    await user.click(email);
    await user.clear(email);
    await user.paste('someone@google.com');
    expectButtonElementDisabled(nextButton);
    await user.tab();
    expectButtonElementEnabled(nextButton);
  });
});
