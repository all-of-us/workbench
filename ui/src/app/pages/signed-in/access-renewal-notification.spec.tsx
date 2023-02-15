import * as React from 'react';
import { mount } from 'enzyme';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import {
  AccessRenewalNotificationMaybe,
  AccessRenewalNotificationProps,
} from './access-renewal-notification';

const profile = ProfileStubVariables.PROFILE_STUB;
const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

describe('Access Renewal Notification', () => {
  const component = (props: AccessRenewalNotificationProps) => {
    return mount(<AccessRenewalNotificationMaybe {...props} />);
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    profileStore.set({ profile, load, reload, updateCache });
  });

  it('Should not render when renewal is not needed (no modules)', async () => {
    const wrapper = component({ accessTier: AccessTierShortNames.Registered });
    expect(wrapper.exists()).toBeTruthy();

    const banner = wrapper.find({
      'data-test-id': 'access-renewal-notification',
    });
    expect(banner.exists()).toBeFalsy();
  });
});
