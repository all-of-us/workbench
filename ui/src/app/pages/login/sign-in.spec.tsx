import * as React from 'react';

import { ProfileApi, StatusAlertApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { renderWithRouter } from 'testing/react-test-helpers';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { StatusAlertApiStub } from 'testing/stubs/status-alert-api-stub';

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
  const defaultWindowWidth = 1700;
  const component = () => renderWithRouter(<SignIn {...props} />);

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(StatusAlertApi, new StatusAlertApiStub());
    window.scrollTo = () => {};
    props = {
      windowSize: { width: defaultWindowWidth, height: 0 },
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

  test.each([
    [
      'login group background image by default',
      defaultWindowWidth,
      'login-group.png',
    ],
    [
      'small background image when window width is moderately sized',
      999,
      'login-standing.png',
    ],
  ])('should display %s', async (_, width, expectedImage) => {
    props.windowSize.width = width;
    component();
    const backgroundDiv = screen.getByTestId('sign-in-page');
    expect(backgroundDiv).toHaveStyle(
      `background-image: url(${expectedImage});`
    );
    expect(screen.getByTestId('login')).toBeInTheDocument();
  });

  test.each([
    ['USA', 'United States'],
    ['international', 'Canada'],
  ])('should handle %s account creation flow.', async (country) => {
    // This test is meant to validate the high-level flow through the sign-in component by checking
    // that each step of the user registration flow is correctly rendered in order.
    //
    // As this is simply a high-level test of this component's ability to render each sub-component,
    // we mock the inner components to avoid needing to deal with the DOM-level details of
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
    mockCountry = country;
    await screen.findByText(mockAccountDetailsTitle);
    await user.click(await screen.findByRole('button', { name: /next/i }));

    // DEMOGRAPHIC_SURVEY
    if (country === 'United States') {
      await screen.findByText(mockDemographicSurveyTitle);
      await user.click(await screen.findByRole('button', { name: /submit/i }));
    }

    // SUCCESS_PAGE
    await screen.getByRole('heading', {
      name: /congratulations!/i,
    });
  });
});
