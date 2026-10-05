import '@testing-library/jest-dom';

import * as React from 'react';
import * as fp from 'lodash/fp';

import { Profile, UserAdminApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { renderWithRouter } from 'testing/react-test-helpers';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { AdminUserTable } from './admin-user-table';

describe('AdminUserTable', () => {
  let props: { profile: Profile; hideSpinner: () => {}; showSpinner: () => {} };

  const component = () => {
    return renderWithRouter(<AdminUserTable {...props} />);
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
      },
    });
    props = {
      ...props,
      profile: ProfileStubVariables.PROFILE_STUB,
      hideSpinner: () => fp.noop,
      showSpinner: () => fp.noop,
    };
    registerApiClient(UserAdminApi, new UserAdminApiStub());
  });

  it('should render', () => {
    component();
    expect(screen.getByText('User Admin Table')).toBeInTheDocument();
  });
});
