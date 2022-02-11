import * as React from 'react';
import { MemoryRouter } from 'react-router';
import * as fp from 'lodash/fp';
import { mount } from 'enzyme';

import { AuthDomainApi, Profile, UserAdminApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import { AuthDomainApiStub } from 'testing/stubs/auth-domain-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { AdminUsers } from './admin-users';

describe('AdminUsers', () => {
  let props: { profile: Profile; hideSpinner: () => {}; showSpinner: () => {} };

  const component = () => {
    return mount(
      <MemoryRouter>
        <AdminUsers {...props} />
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: {
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
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
