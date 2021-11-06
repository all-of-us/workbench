import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {AuthDomainApi, Profile, ProfileApi} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {AuthDomainApiStub} from 'testing/stubs/auth-domain-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';

import {AdminUsers} from './admin-users';


describe('AdminUsers', () => {
  let props: {profile: Profile, hideSpinner: () => {}, showSpinner: () => {}};

  const component = () => {
    return mount(<AdminUsers {...props}/>);
  };

  beforeEach(() => {
    serverConfigStore.set({config: {
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    }});
    props = {
      ...props,
      profile: ProfileStubVariables.PROFILE_STUB,
      hideSpinner: () => fp.noop,
      showSpinner: () => fp.noop
    };
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(AuthDomainApi, new AuthDomainApiStub());
  });


  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
