import {mount} from 'enzyme';
import * as React from 'react';

import {AdminUser} from './admin-user';
import {AuthDomainApi, Profile, ProfileApi} from 'generated/fetch';
import {serverConfigStore} from 'app/utils/navigation';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {AuthDomainApiStub} from 'testing/stubs/auth-domain-api-stub';


describe('AdminUser', () => {
  let props: {profile: Profile};

  const component = () => {
    return mount(<AdminUser {...props}/>);
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
    };
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(AuthDomainApi, new AuthDomainApiStub());
  });


  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
