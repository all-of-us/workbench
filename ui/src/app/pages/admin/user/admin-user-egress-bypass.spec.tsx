import '@testing-library/jest-dom';

import * as React from 'react';

import { UserAdminApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { AdminUserEgressBypass } from './admin-user-egress-bypass';

describe('AdminUserEgressBypassSpec', () => {
  const defaultProps = {
    userId: 123,
  };

  const component = () => {
    return render(<AdminUserEgressBypass {...defaultProps} />);
  };

  beforeEach(() => {
    registerApiClient(UserAdminApi, new UserAdminApiStub());
  });

  it('should render', async () => {
    component();
    expect(screen.getByText('Enable Large File Downloads')).toBeInTheDocument();
  });
});
