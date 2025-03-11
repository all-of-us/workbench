import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { StatusAlertApi, StatusAlertLocation } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import {
  clearApiClients,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { StatusAlertApiStub } from 'testing/stubs/status-alert-api-stub';

import LoginReactComponent from './login';

describe('LoginComponent', () => {
  let loginProps: { signIn: Function; onCreateAccount: Function };

  const component = (props) =>
    render(
      <MemoryRouter>
        <LoginReactComponent {...props} />
      </MemoryRouter>
    );

  beforeEach(() => {
    loginProps = {
      signIn: () => {},
      onCreateAccount: () => {},
    };
  });

  afterEach(() => {
    clearApiClients();
    jest.resetAllMocks();
  });

  it('should render', () => {
    registerApiClient(
      StatusAlertApi,
      new StatusAlertApiStub(StatusAlertLocation.AFTER_LOGIN)
    );
    component(loginProps);
    expect(
      screen.getByText('Already have a Researcher Workbench account?')
    ).toBeInTheDocument();
  });

  it('should show banner status alert is BEFORE_LOGIN', async () => {
    registerApiClient(
      StatusAlertApi,
      new StatusAlertApiStub(StatusAlertLocation.BEFORE_LOGIN)
    );
    component(loginProps);
    expect(await screen.findByText(/Stub Title/i)).toBeInTheDocument();
  });
});
