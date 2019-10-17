import {mount} from 'enzyme';
import * as React from 'react';
import {AdminUserBypass} from './admin-user-bypass';
import {Profile} from 'generated/fetch';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {serverConfigStore} from 'app/utils/navigation';

describe('AdminUserBypassSpec', () => {
  let props: {profile: Profile};

  const component = () => {
    return mount(<AdminUserBypass {...props}/>);
  };

  beforeEach(() => {
    serverConfigStore.next({
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    });
    props = {
      profile: ProfileStubVariables.PROFILE_STUB
    }
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
  // Note: this spec is not testing the Popup on the admin bypass button due to an issue using
  //    PopupTrigger in the test suite.
});
