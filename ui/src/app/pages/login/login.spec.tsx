import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { StatusAlertApi, StatusAlertLocation } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import {
  clearApiClients,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { MILLIS_PER_HOUR } from 'app/utils/dates';
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
        startTimeEpochMillis: Date.now(),
      },
      {
        title: 'Alert 2',
        message: 'Message 2',
        link: 'link2',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: Date.now(),
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
        startTimeEpochMillis: Date.now(),
      },
      {
        title: 'After Login Alert',
        message: 'Should not show',
        link: 'link2',
        alertLocation: StatusAlertLocation.AFTER_LOGIN,
        startTimeEpochMillis: Date.now(),
      },
    ]);

    component(loginProps);

    expect(await screen.findByText(/Before Login Alert/)).toBeInTheDocument();
    expect(screen.getByText(/Should show/)).toBeInTheDocument();
    expect(screen.queryByText(/After Login Alert/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Should not show/)).not.toBeInTheDocument();
  });

  it('should show alerts with current time between start and end times', async () => {
    const now = Date.now();
    const oneHourAgo = now - MILLIS_PER_HOUR;
    const oneHourLater = now + MILLIS_PER_HOUR;

    statusAlertStub.addAlerts([
      {
        title: 'Current Alert',
        message: 'Should show - currently active',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: oneHourAgo,
        endTimeEpochMillis: oneHourLater,
      },
    ]);

    component(loginProps);
    expect(await screen.findByText(/Current Alert/)).toBeInTheDocument();
    expect(
      screen.getByText(/Should show - currently active/)
    ).toBeInTheDocument();
  });

  it('should not show alerts with start time in the future', async () => {
    const now = Date.now();
    const oneHourLater = now + MILLIS_PER_HOUR;
    const twoHoursLater = now + MILLIS_PER_HOUR * 2;

    statusAlertStub.addAlerts([
      {
        title: 'Future Alert',
        message: 'Should not show - starts in future',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: oneHourLater,
        endTimeEpochMillis: twoHoursLater,
      },
    ]);

    component(loginProps);
    expect(screen.queryByText(/Future Alert/)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Should not show - starts in future/)
    ).not.toBeInTheDocument();
  });

  it('should not show alerts with end time in the past', async () => {
    const now = Date.now();
    const twoHoursAgo = now - 2 * MILLIS_PER_HOUR;
    const oneHourAgo = now - MILLIS_PER_HOUR;

    statusAlertStub.addAlerts([
      {
        title: 'Expired Alert',
        message: 'Should not show - already expired',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: twoHoursAgo,
        endTimeEpochMillis: oneHourAgo,
      },
    ]);

    component(loginProps);
    expect(screen.queryByText(/Expired Alert/)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Should not show - already expired/)
    ).not.toBeInTheDocument();
  });

  it('should show alerts with null start time (always active from beginning)', async () => {
    const now = Date.now();
    const oneHourLater = now + MILLIS_PER_HOUR;

    statusAlertStub.addAlerts([
      {
        title: 'No Start Time Alert',
        message: 'Should show - no start time',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: null,
        endTimeEpochMillis: oneHourLater,
      },
    ]);

    component(loginProps);
    expect(await screen.findByText(/No Start Time Alert/)).toBeInTheDocument();
    expect(screen.getByText(/Should show - no start time/)).toBeInTheDocument();
  });

  it('should show alerts with null end time (never expires)', async () => {
    const now = Date.now();
    const oneHourAgo = now - MILLIS_PER_HOUR;

    statusAlertStub.addAlerts([
      {
        title: 'No End Time Alert',
        message: 'Should show - no end time',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: oneHourAgo,
        endTimeEpochMillis: null,
      },
    ]);

    component(loginProps);
    expect(await screen.findByText(/No End Time Alert/)).toBeInTheDocument();
    expect(screen.getByText(/Should show - no end time/)).toBeInTheDocument();
  });

  it('should show alerts with both null start and end times (always active)', async () => {
    statusAlertStub.addAlerts([
      {
        title: 'Always Active Alert',
        message: 'Should show - always active',
        link: 'link1',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
        startTimeEpochMillis: null,
        endTimeEpochMillis: null,
      },
    ]);

    component(loginProps);
    expect(await screen.findByText(/Always Active Alert/)).toBeInTheDocument();
    expect(screen.getByText(/Should show - always active/)).toBeInTheDocument();
  });
});
