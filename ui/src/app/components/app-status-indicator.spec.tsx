import '@testing-library/jest-dom';

import * as React from 'react';

import { AppStatus } from 'generated/fetch';

import { render, within } from '@testing-library/react';

import { AppStatusIndicator } from './app-status-indicator';

describe('App Status Indicator', () => {
  test.each([
    [AppStatus.STARTING, 'is updating'],
    [AppStatus.STOPPED, 'has stopped'],
    [AppStatus.RUNNING, 'is running'],
    [AppStatus.STOPPING, 'is stopping'],
    [AppStatus.ERROR, 'has encountered an error'],
  ])(
    'App Status indicator renders correct indicator when a user app is in %s state',
    (appStatus, iconMeaning) => {
      const { getByTestId } = render(
        <AppStatusIndicator {...{ appStatus }} userSuspended={false} />
      );
      const iconContainer = getByTestId('app-status-icon-container');
      const statusIcon = within(iconContainer).getByTitle(
        `Icon indicating item ${iconMeaning}`
      );
      expect(statusIcon).toBeInTheDocument();
    }
  );

  it('Verify that a user app with an undefined status does not have a status indicator', () => {
    const { getByTestId } = render(
      <AppStatusIndicator userSuspended={false} appStatus={undefined} />
    );
    const iconContainer = getByTestId('app-status-icon-container');
    expect(iconContainer.children.length).toEqual(0);
  });

  it('Verify that a user app where the user is suspended displays the correct indicator', () => {
    const { getByTestId } = render(
      <AppStatusIndicator userSuspended appStatus={undefined} />
    );
    const iconContainer = getByTestId('app-status-icon-container');
    within(iconContainer).getByTitle(`Icon indicating item is suspended`);
  });
});
