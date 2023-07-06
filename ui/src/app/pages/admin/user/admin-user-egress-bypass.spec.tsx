import * as React from 'react';
import { mount } from 'enzyme';

import { UserAdminApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { AdminUserEgressByPass } from './admin-user-egress-bypass';

describe('AdminUserEgressBypassSpec', () => {
  const defaultProps = {
    userId: 123,
  };

  const component = () => {
    return mount(<AdminUserEgressByPass {...defaultProps} />);
  };

  beforeEach(() => {
    registerApiClient(UserAdminApi, new UserAdminApiStub());
  });

  it('should render', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });
});
