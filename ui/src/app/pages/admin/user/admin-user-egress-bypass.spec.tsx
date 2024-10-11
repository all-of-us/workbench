import '@testing-library/jest-dom';

import * as React from 'react';

import { UserAdminApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  registerApiClient,
  userAdminApi,
} from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectTooltip,
  expectTooltipAbsence,
} from 'testing/react-test-helpers';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import {
  AdminUserEgressBypass,
  AdminUserEgressBypassProps,
  MAX_BYPASS_DESCRIPTION,
  MIN_BYPASS_DESCRIPTION,
} from './admin-user-egress-bypass';

const getCreateWindowSpy = () =>
  jest.spyOn(userAdminApi(), 'createEgressBypassWindow');

const getBypassReasonText = () =>
  screen.getByRole('textbox', {
    name: /Enter description for large file download request./i,
  });

const getBypassButton = () =>
  screen.getByRole('button', {
    name: 'Temporarily Enable Large File Downloads',
  });

describe(AdminUserEgressBypass.name, () => {
  const defaultProps: AdminUserEgressBypassProps = {
    targetUserId: ProfileStubVariables.PROFILE_STUB.userId,
  };

  const component = (overrideProps?: Partial<AdminUserEgressBypassProps>) =>
    render(
      <AdminUserEgressBypass {...{ ...defaultProps, ...overrideProps }} />
    );

  beforeEach(() => {
    registerApiClient(UserAdminApi, new UserAdminApiStub());
  });

  it('should allow egress bypass', async () => {
    const user = userEvent.setup();
    const createWindowSpy = getCreateWindowSpy();

    component();

    const byPassDescription = 'test bypass reason';

    await user.click(getBypassReasonText());
    await user.paste(byPassDescription);

    const bypassButton = getBypassButton();

    await expectTooltipAbsence(
      bypassButton,
      /Required to enable large file downloads/i,
      user
    );
    expectButtonElementEnabled(bypassButton);

    await user.click(bypassButton);

    expect(createWindowSpy).toHaveBeenCalledWith(
      ProfileStubVariables.PROFILE_STUB.userId,
      {
        startTime: expect.any(Number),
        byPassDescription,
      }
    );
  });

  it('should disallow egress bypass with a too short reason', async () => {
    const user = userEvent.setup();

    component();

    const tooShort = 'a'.repeat(MIN_BYPASS_DESCRIPTION - 1);

    await user.click(getBypassReasonText());
    await user.paste(tooShort);

    const bypassButton = getBypassButton();
    await expectTooltip(bypassButton, /Request Reason/i, user);
    expectButtonElementDisabled(bypassButton);
  });

  it('should disallow egress bypass with a too long reason', async () => {
    const user = userEvent.setup();

    component();

    const tooShort = 'a'.repeat(MAX_BYPASS_DESCRIPTION + 1);

    await user.click(getBypassReasonText());
    await user.paste(tooShort);

    const bypassButton = getBypassButton();
    await expectTooltip(bypassButton, /Request Reason/i, user);
    expectButtonElementDisabled(bypassButton);
  });
});
