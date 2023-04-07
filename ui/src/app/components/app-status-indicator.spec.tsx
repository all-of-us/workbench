import * as React from 'react';
import { mount } from 'enzyme';

import { AppStatus } from 'generated/fetch';

import { AppStatusIndicator } from './app-status-indicator';
import {
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  SuspendedIcon,
  UpdatingIcon,
} from './status-icon';

describe('App Status Indicator', () => {
  test.each([
    [AppStatus.STARTING, UpdatingIcon],
    [AppStatus.STOPPED, StoppedIcon],
    [AppStatus.RUNNING, RunningIcon],
    [AppStatus.STOPPING, StoppingIcon],
    [AppStatus.ERROR, ErrorIcon],
  ])(
    'App Status indicator renders correct indicator when a user app is in %s state',
    (appStatus, icon) => {
      const wrapper = mount(
        <AppStatusIndicator {...{ appStatus }} userSuspended={false} />
      );
      expect(wrapper.exists()).toBeTruthy();
      const statusIcon = wrapper.find(icon);
      expect(statusIcon.exists()).toBeTruthy();
    }
  );

  it('Verify that a user app with an undefined status does not have a status indicator', () => {
    const wrapper = mount(
      <AppStatusIndicator userSuspended={false} appStatus={undefined} />
    );
    expect(wrapper.exists()).toBeTruthy();
    const iconContainer = wrapper.find(
      'div[data-test-id="app-status-icon-container"]'
    );
    expect(iconContainer.exists()).toBeTruthy();
    expect(iconContainer.children().length).toEqual(0);
  });

  it('Verify that a user app where the user is suspended displays the correct indicator', () => {
    const wrapper = mount(
      <AppStatusIndicator userSuspended appStatus={undefined} />
    );
    expect(wrapper.exists()).toBeTruthy();
    const statusIcon = wrapper.find(SuspendedIcon);
    expect(statusIcon.exists()).toBeTruthy();
  });
});
