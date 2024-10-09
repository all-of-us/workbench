import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { AdminUserBypass } from './admin-user-bypass';

describe('AdminUserBypassSpec', () => {
  const defaultProps = {
    user: ProfileStubVariables.ADMIN_TABLE_USER_STUB,
  };

  const component = () => {
    return render(<AdminUserBypass {...defaultProps} />);
  };

  let user;
  beforeEach(() => {
    user = userEvent.setup();
    serverConfigStore.set({
      config: defaultServerConfig,
    });
  });

  it('should render bypass toggles for access modules', async () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableRasLoginGovLinking: true,
        enableComplianceTraining: true,
      },
    });

    component();

    const bypassButton = await screen.findByRole('button', { name: /bypass/i });
    await user.click(bypassButton);

    expect(
      [
        'rt-compliance-training-toggle',
        'ct-compliance-training-toggle',
        'ducc-toggle',
        'two-factor-auth-toggle',
        'identity-toggle',
        'profile-confirmation-toggle',
        'publication-confirmation-toggle',
      ]
        .filter((id) => !screen.queryByTestId(id))
        .map((id) => console.log(`Failed to find data-test-id: ${id}`)).length
    ).toBe(0); // count of not found data-test-ids
  });

  it('should only show bypass toggles for access modules used in this environment', async () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableRasLoginGovLinking: false,
        enableComplianceTraining: false,
      },
    });

    component();
    const bypassButton = await screen.findByRole('button', { name: /bypass/i });
    await user.click(bypassButton);

    expect(
      [
        'ducc-toggle',
        'two-factor-auth-toggle',
        'profile-confirmation-toggle',
        'publication-confirmation-toggle',
      ]
        .filter((id) => !screen.queryByTestId(id))
        .map((id) => console.log(`Failed to find data-test-id: ${id}`)).length
    ).toBe(0); // count of not found data-test-ids

    expect(
      [
        'rt-compliance-training-toggle',
        'ct-compliance-training-toggle',
        'era-commons-toggle',
        'identity-toggle',
      ]
        .filter((id) => screen.queryByTestId(id))
        .map((id) => console.log(`Failed to find data-test-id: ${id}`)).length
    ).toBe(0); // count of found data-test-ids
  });
});
