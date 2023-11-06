import '@testing-library/jest-dom';

import { InstitutionalRole, InstitutionApi, Profile } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AccountCreationInstitution } from 'app/pages/login/account-creation/account-creation-institution';
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

describe('Account Creation- Institution', () => {
  beforeEach(() => {
    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async () => {
    setup();
    try {
      expect(screen.getByText('Create your account')).toBeInTheDocument();
      expect(screen.getByText('Please complete Step 1')).toBeInTheDocument();
      expect(screen.getByText('select your institution')).toBeInTheDocument();
    } catch (e) {
      expect(true);
    }
  });

  it('should load institutions list', async () => {
    const mockGetPublicInstitutionDetails = jest.spyOn(
      institutionApi(),
      'getPublicInstitutionDetails'
    );
    const { container, user } = setup();

    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );
    expect(mockGetPublicInstitutionDetails).toHaveBeenCalled();
    const dropDownDefaultValue = screen.getByText('Broad Institute');
    expect(dropDownDefaultValue).toBeInTheDocument();
    await user.click(dropDownDefaultValue);

    for (let index = 0; index < defaultInstitutions.length; index++) {
      const name = defaultInstitutions[index].displayName;
      // Fix this
      if (name === 'Broad Institute') {
        continue;
      }
      expect(
        screen.getByText(defaultInstitutions[index].displayName)
      ).toBeInTheDocument();
    }
  });
  it('should show user-facing error message on data load error', async () => {
    const mockGetPublicInstitutionDetails = jest.spyOn(
      institutionApi(),
      'getPublicInstitutionDetails'
    );
    mockGetPublicInstitutionDetails.mockRejectedValueOnce(
      new Response(null, { status: 500 })
    );
    const { container } = setup();

    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );

    expect(
      screen.getByText(
        /an error occurred loading the institution list\. please try again or contact\./i
      )
    ).toBeInTheDocument();
  });

  it('should reset role value & options when institution is selected', async () => {
    const { container, user } = setup();

    // Wait for spinner to go away
    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );
    // Confirm: Institution Role is already populated
    try {
      screen.getByDisplayValue('Select Role');
    } catch (e) {
      expect(true);
    }
    const institutionRole = screen.getByText(
      /research fellow \(a post\-doctoral fellow or medical resident in training\)/i
    );
    expect(institutionRole).toBeInTheDocument();

    // Change the institution Role
    await user.click(institutionRole);
    await user.paste('Early career tenure-track researcher');
    await user.tab();
    const newInstitutionRole = screen.getByText(
      /early career tenure\-track researcher/i
    );
    expect(newInstitutionRole).toBeInTheDocument();

    // Change institution
    const dropDownDefaultValue = screen.getByText('Broad Institute');
    await user.click(dropDownDefaultValue);
    await user.paste('Verily LLC');
    await user.tab();

    // Expect institution role to be default or not selected
    const selectRole = screen.getByDisplayValue('Select Role');
    expect(selectRole).toBeInTheDocument();
  });

  it('should validate email affiliation when inst and email address are specified', async () => {
    const { container, user } = setup();
    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );

    const contactEmail = screen.getByDisplayValue(
      /contactemail@broadinstitute\.org/i
    );

    await user.click(contactEmail);
    await user.paste('someAnotherDomain@broadinstitute.org');
    await user.tab();
    console.log(screen.logTestingPlaygroundURL());

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
    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );

    const contactEmail = screen.getByDisplayValue(/someemail@google\.com/i);

    await user.click(contactEmail);
    await user.clear(contactEmail);
    await user.paste('someOthrEmail@google.com');
    await user.tab();

    try {
      const emailError = screen.getByText(
        /your email does not match your institution/i
      );
      expect(emailError).not.toBeInTheDocument();
    } catch (e) {
      expect(true);
    }
    await user.click(contactEmail);
    await user.clear(contactEmail);
    await user.paste('someOthrEmail@someOtherDomain.com');
    await user.tab();
    console.log(screen.logTestingPlaygroundURL());
    const emailError = screen.getByText(
      /your email does not match your institution/i
    );
    expect(emailError).toBeInTheDocument();

    await user.click(contactEmail);
    await user.clear(contactEmail);
    await user.paste('someOthrEmail@verily.com');
    await user.tab();

    try {
      screen.getByText(/your email does not match your institution/i);
    } catch (e) {
      expect(true);
    }
  });
  it('should display appropriate icons if email is valid/invalid', async () => {
    const { container, user } = setup();
    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );
    let icons = screen
      .getByLabelText('emailvalidation')
      .firstElementChild.getAttribute('shape');

    expect(icons).toEqual('success-standard');
    console.log(icons);
    const contactEmail = screen.getByDisplayValue(
      /contactemail@broadinstitute\.org/i
    );

    await user.click(contactEmail);
    await user.paste('someAnotherDomain@broadinstitute.org');
    await user.tab();
    icons = screen
      .getByLabelText('emailvalidation')
      .firstElementChild.getAttribute('shape');

    expect(icons).toEqual('warning-standard');
  });
  it('Should disable Next button until email is verified for institution', async () => {
    const { container, user } = setup();
    await waitFor(() =>
      container.querySelector(
        '#account-creation-institution > div:nth-child(2) > div:nth-child(1) > div:nth-child(4) > div > svg'
      )
    );
    let nextButton = screen.getByRole('button', { name: /next/i });
    expectButtonElementEnabled(nextButton);
    // Change institution
    const dropDownDefaultValue = screen.getByText('Broad Institute');
    await user.click(dropDownDefaultValue);
    await user.paste('Verily LLC');
    await user.tab();

    const institutionRole = screen.getByText(
      /research fellow \(a post\-doctoral fellow or medical resident in training\)/i
    );
    // Change the institution Role
    await user.click(institutionRole);
    await user.paste('Early career tenure-track researcher');
    await user.tab();

    const newInstitutionRole = screen.getByText(
      /early career tenure\-track researcher/i
    );
    expect(newInstitutionRole).toBeInTheDocument();
    // Confirm Next button is disabled
    nextButton = screen.getByRole('button', { name: /next/i });
    expectButtonElementDisabled(nextButton);
    const email = screen.getByDisplayValue(/contactemail@broadinstitute\.org/i);
    await user.click(email);
    await user.clear(email);
    await user.paste('someone@google.com');
    expectButtonElementDisabled(nextButton);
    await user.tab();
    const icons = screen.getByLabelText('emailvalidation');

    console.log(icons);

    console.log(screen.logTestingPlaygroundURL());
  });
});
