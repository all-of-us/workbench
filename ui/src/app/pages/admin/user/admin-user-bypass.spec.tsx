import * as React from 'react';
import { mount } from 'enzyme';

import { AdminTableUser } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { AdminUserBypass } from './admin-user-bypass';

describe('AdminUserBypassSpec', () => {
  let props: { user: AdminTableUser };

  const component = () => {
    return mount(<AdminUserBypass {...props} />);
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: {
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
        enableEraCommons: true,
        enableRasLoginGovLinking: true,
      },
    });
    props = {
      user: ProfileStubVariables.ADMIN_TABLE_USER_STUB,
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
  // Note: this spec is not testing the Popup on the admin bypass button due to an issue using
  //    PopupTrigger in the test suite.
});
