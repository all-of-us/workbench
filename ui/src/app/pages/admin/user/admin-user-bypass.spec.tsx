import * as React from 'react';
import { mount } from 'enzyme';

import { Button } from 'app/components/buttons';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { AdminUserBypass } from './admin-user-bypass';

describe('AdminUserBypassSpec', () => {
  const defaultProps = {
    user: ProfileStubVariables.ADMIN_TABLE_USER_STUB,
  };

  const component = () => {
    return mount(<AdminUserBypass {...defaultProps} />);
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  it('should render bypass toggles for access modules', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableEraCommons: true,
        enableRasLoginGovLinking: true,
        enableComplianceTraining: true,
      },
    });

    const wrapper = component();
    wrapper.find(Button).simulate('click');

    expect(
      [
        'rt-compliance-training-toggle',
        'ct-compliance-training-toggle',
        'ducc-toggle',
        'era-commons-toggle',
        'two-factor-auth-toggle',
        'identity-toggle',
        'profile-confirmation-toggle',
        'publication-confirmation-toggle',
      ]
        .filter((id) => !wrapper.find(`[data-test-id="${id}"]`).exists())
        .map((id) => console.log(`Failed to find data-test-id: ${id}`)).length
    ).toBe(0); // count of not found data-test-ids
  });

  it('should only show bypass toggles for access modules used in this environment', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableEraCommons: false,
        enableRasLoginGovLinking: false,
        enableComplianceTraining: false,
      },
    });

    const wrapper = component();
    wrapper.find(Button).simulate('click');

    expect(
      [
        'ducc-toggle',
        'two-factor-auth-toggle',
        'profile-confirmation-toggle',
        'publication-confirmation-toggle',
      ]
        .filter((id) => !wrapper.find(`[data-test-id="${id}"]`).exists())
        .map((id) => console.log(`Failed to find data-test-id: ${id}`)).length
    ).toBe(0); // count of not found data-test-ids

    expect(
      [
        'rt-compliance-training-toggle',
        'ct-compliance-training-toggle',
        'era-commons-toggle',
        'identity-toggle',
      ]
        .filter((id) => wrapper.find(`[data-test-id="${id}"]`).exists())
        .map((id) => console.log(`Failed to find data-test-id: ${id}`)).length
    ).toBe(0); // count of found data-test-ids
  });
});
