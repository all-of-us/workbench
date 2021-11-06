import {profileStore, serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {AccessModule} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {MemoryRouter} from 'react-router-dom';
import defaultServerConfig from 'testing/default-server-config';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

import {LoginGovIAL2NotificationMaybe} from "./login-gov-ial2-notification";

describe('LoginGovIAL2Notification', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();
  const profile = ProfileStubVariables.PROFILE_STUB;

  beforeEach(() => {
    serverConfigStore.set({config: defaultServerConfig});
    profileStore.set({profile, load, reload, updateCache});
  });

  const component = () => {
    return mount(<MemoryRouter><LoginGovIAL2NotificationMaybe/></MemoryRouter>);
  };

  it('should show notification when login.gov feature flag enabled', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: true
      }});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').exists()).toBeTruthy();
  });

  it('should not show notification when login.gov feature flag disabled', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: false
      }});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').exists()).toBeFalsy();
  });

  it('should not show notification when login.gov complete', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: true
      }});
    const newProfile = fp.set('accessModules', {modules: [
        {moduleName: AccessModule.RASLINKLOGINGOV, completionEpochMillis: Date.now()},
      ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});

    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').exists()).toBeFalsy();
  });

  it('should not show notification when login.gov bypassed', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: true
      }});
    const newProfile = fp.set('accessModules', {modules: [
        {moduleName: AccessModule.RASLINKLOGINGOV, bypassEpochMillis: Date.now()},
      ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});

    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').exists()).toBeFalsy();
  });

});
