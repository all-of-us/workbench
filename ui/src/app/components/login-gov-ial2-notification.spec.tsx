import {mount} from 'enzyme';
import * as React from 'react';
import * as fp from 'lodash/fp';

import {LoginGovIAL2NotificationMaybe} from "./login-gov-ial2-notification";
import {profileStore, serverConfigStore} from 'app/utils/stores';
import defaultServerConfig from 'testing/default-server-config';
import {AccessModule} from 'generated/fetch';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

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
    return mount(<LoginGovIAL2NotificationMaybe/>);
  };

  it('should show notification when login.gov feature flag enabled', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: true
      }});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').length).toBe(2);
  });

  it('should not notification when login.gov feature flag disabled', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: false
      }});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').length).toBe(0);
  });

  it('should not notification when login.gov complete', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: true
      }});
    const newProfile = fp.set('accessModules', {modules: [
        {moduleName: AccessModule.RASLINKLOGINGOV, completionEpochMillis: Date.now()},
      ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});

    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').length).toBe(0);
  });

  it('should not notification when login.gov bypassed', () => {
    serverConfigStore.set({config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: true
      }});
    const newProfile = fp.set('accessModules', {modules: [
        {moduleName: AccessModule.RASLINKLOGINGOV, bypassEpochMillis: Date.now()},
      ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});

    const wrapper = component();
    expect(wrapper.find('[data-test-id="ial2-notification"]').length).toBe(0);
  });

});