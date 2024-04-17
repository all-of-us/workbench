import '@testing-library/jest-dom';

import * as React from 'react';
import * as fp from 'lodash/fp';

import { AuthDomainApi, Profile, UserAdminApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { AuthDomainApiStub } from 'testing/stubs/auth-domain-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { AdminUserTable } from './admin-user-table';

describe('AdminUserTable', () => {
  let props: { profile: Profile; hideSpinner: () => {}; showSpinner: () => {} };

  const component = () => {
    return render(<AdminUserTable {...props} />);
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
        enableEraCommons: true,
      },
    });
    props = {
      ...props,
      profile: ProfileStubVariables.PROFILE_STUB,
      hideSpinner: () => fp.noop,
      showSpinner: () => fp.noop,
    };
    registerApiClient(UserAdminApi, new UserAdminApiStub());
    registerApiClient(AuthDomainApi, new AuthDomainApiStub());
  });

  it('should render', () => {
    component();
    expect(screen.getByText('User Admin Table')).toBeInTheDocument();
  });
});
