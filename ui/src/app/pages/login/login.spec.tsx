import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { StatusAlertApi, StatusAlertLocation } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import {
  clearApiClients,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { StatusAlertApiStub } from 'testing/stubs/status-alert-api-stub';

import LoginReactComponent from './login';

describe('LoginComponent', () => {
  let loginProps: { signIn: Function; onCreateAccount: Function };
  let statusAlertStub: StatusAlertApiStub;

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

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableLoginIssueBanner: false,
      },
    });

    statusAlertStub = new StatusAlertApiStub();
    registerApiClient(StatusAlertApi, statusAlertStub);
  });

  afterEach(() => {
    clearApiClients();
    jest.resetAllMocks();
  });

  it('should render', () => {
    component(loginProps);
    expect(
      screen.getByText('Already have a Researcher Workbench account?')
    ).toBeInTheDocument();
  });

  it('should show multiple banner status alerts that are BEFORE_LOGIN', async () => {
    statusAlertStub.addAlerts([
      {
        title: 'Alert 1',
        message: 'Message 1',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
      },
      {
        title: 'Alert 2',
        message: 'Message 2',
        link: 'link2',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
      },
    ]);

    component(loginProps);

    expect(await screen.findByText(/Alert 1/)).toBeInTheDocument();
    expect(screen.getByText(/Message 1/)).toBeInTheDocument();
    expect(await screen.findByText(/Alert 2/)).toBeInTheDocument();
    expect(screen.getByText(/Message 2/)).toBeInTheDocument();
  });

  it('should only show BEFORE_LOGIN alerts', async () => {
    statusAlertStub.addAlerts([
      {
        title: 'Before Login Alert',
        message: 'Should show',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
      },
      {
        title: 'After Login Alert',
        message: 'Should not show',
        link: 'link2',
        alertLocation: StatusAlertLocation.AFTER_LOGIN,
      },
    ]);

    component(loginProps);

    expect(await screen.findByText(/Before Login Alert/)).toBeInTheDocument();
    expect(screen.getByText(/Should show/)).toBeInTheDocument();
    expect(screen.queryByText(/After Login Alert/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Should not show/)).not.toBeInTheDocument();
  });
});
