import * as React from 'react';

import { ProfileApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { renderWithRouter } from 'testing/react-test-helpers';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

import { SignIn, SignInProps } from './sign-in';

const mockTOSTitle = 'Mock TOS';
jest.mock('app/components/terms-of-service', () => {
  return {
    TermsOfService: (props) => (
      <div>
        <div>{mockTOSTitle}</div>
        <button onClick={props.onComplete}>Next</button>
      </div>
    ),
  };
});

const mockAccountCreationInstitutionTitle = 'Mock Account Creation Institution';
jest.mock(
  'app/pages/login/account-creation/account-creation-institution',
  () => {
    return {
      AccountCreationInstitution: (props) => (
        <div>
          <div>{mockAccountCreationInstitutionTitle}</div>
          <button
            onClick={() =>
              props.onComplete({
                username: 'testUser',
                generalDiscoverySources: ['OTHER'],
              })
            }
          >
            Next
          </button>
        </div>
      ),
    };
  }
);

const mockAccountDetailsTitle = 'Mock Account Details';
let mockCountry;
jest.mock('app/pages/login/account-creation/account-creation', () => {
  return {
    AccountCreation: (props) => (
      <div>
        <div>{mockAccountDetailsTitle}</div>
        <button
          onClick={() =>
            props.onComplete({
              username: 'testUser',
              generalDiscoverySources: ['OTHER'],
              address: { country: mockCountry },
            })
          }
        >
          Next
        </button>
        <button onClick={props.onSubmit}>Submit</button>
      </div>
    ),
  };
});

const mockDemographicSurveyTitle = 'Mock Demographic Survey';
jest.mock('app/components/demographic-survey-v2', () => {
  return {
    DemographicSurvey: () => (
      <div>
        <div>{mockDemographicSurveyTitle}</div>
      </div>
    ),
  };
});

describe('SignIn', () => {
  let props: SignInProps;
  let user;

  const component = () => renderWithRouter(<SignIn {...props} />);

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    window.scrollTo = () => {};
    props = {
      windowSize: { width: 1700, height: 0 },
      hideSpinner: () => {},
      showSpinner: () => {},
      showProfileErrorModal: () => {},
    };
    serverConfigStore.set({
      config: defaultServerConfig,
    });
    user = userEvent.setup();
    mockCountry = null;
  });

  it('should display login background image and directive by default', () => {
    component();
    const backgroundDiv = screen.getByTestId('sign-in-page');
    expect(backgroundDiv).toHaveStyle(
      'background-image: url(login-group.png);'
    );
    expect(screen.getByTestId('login')).toBeInTheDocument();
  });

  it('should display small background image when window width is moderately sized', () => {
    props.windowSize.width = 999;
    component();
    const backgroundDiv = screen.getByTestId('sign-in-page');
    expect(backgroundDiv).toHaveStyle(
      'background-image: url(login-standing.png);'
    );
    expect(screen.getByTestId('login')).toBeInTheDocument();
  });

  it('should handle sign-up flow', async () => {
    // This test is meant to validate the high-level flow through the sign-in component by checking
    // that each step of the user registration flow is correctly rendered in order.
    //
    // As this is simply a high-level test of this component's ability to render each sub-component,
    // we use Enzyme's shallow rendering to avoid needing to deal with the DOM-level details of
    // each of the sub-components. Tests within the 'account-creation' folder should cover those
    // details.
    component();
    // the sign-up flow steps are enumerated by `SignInStep`:
    // LANDING, TERMS_OF_SERVICE, INSTITUTIONAL_AFFILIATION, ACCOUNT_DETAILS, DEMOGRAPHIC_SURVEY, SUCCESS_PAGE,

    // To start, the LANDING page / login component should be shown.
    await user.click(
      await screen.findByRole('button', { name: 'Create Account' })
    );
    // TERMS_OF_SERVICE
    await screen.findByText(mockTOSTitle);
    await user.click(await screen.findByRole('button', { name: /next/i }));

    // INSTITUTIONAL_AFFILIATION
    await screen.findByText(mockAccountCreationInstitutionTitle);
    await user.click(await screen.findByRole('button', { name: /next/i }));

    // ACCOUNT_DETAILS
    mockCountry = 'United States';
    await screen.findByText(mockAccountDetailsTitle);
    await user.click(await screen.findByRole('button', { name: /next/i }));

    // DEMOGRAPHIC_SURVEY
    await screen.findByText(mockDemographicSurveyTitle);
    await user.click(await screen.findByRole('button', { name: /submit/i }));

    // SUCCESS_PAGE
    await screen.getByRole('heading', {
      name: /congratulations!/i,
    });
  });

  it('should not show demographic survey of international user', async () => {
    // This test is meant to validate the high-level flow through the sign-in component by checking
    // that each step of the user registration flow is correctly rendered in order.
    //
    // As this is simply a high-level test of this component's ability to render each sub-component,
    // we use Enzyme's shallow rendering to avoid needing to deal with the DOM-level details of
    // each of the sub-components. Tests within the 'account-creation' folder should cover those
    // details.
    component();
    // the sign-up flow steps are enumerated by `SignInStep`:
    // LANDING, TERMS_OF_SERVICE, INSTITUTIONAL_AFFILIATION, ACCOUNT_DETAILS, SUCCESS_PAGE,

    // To start, the LANDING page / login component should be shown.
    await user.click(
      await screen.findByRole('button', { name: 'Create Account' })
    );
    // TERMS_OF_SERVICE
    await screen.findByText(mockTOSTitle);
    await user.click(await screen.findByRole('button', { name: /next/i }));

    // INSTITUTIONAL_AFFILIATION
    await screen.findByText(mockAccountCreationInstitutionTitle);
    await user.click(await screen.findByRole('button', { name: /next/i }));

    // ACCOUNT_DETAILS
    mockCountry = 'Canada';
    await screen.findByText(mockAccountDetailsTitle);
    await user.click(await screen.findByRole('button', { name: /submit/i }));

    // SUCCESS_PAGE
    await screen.findByRole('heading', {
      name: /congratulations!/i,
    });
  });
});
