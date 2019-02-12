import {userProfileStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import * as React from 'react';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfilePage} from './component';


describe('ProfilePageComponent', () => {

  const profile = ProfileStubVariables.PROFILE_STUB;

  const component = () => {
    return mount(<ProfilePage/>);
  };

  beforeEach(() => {
    userProfileStore.next({profile, reload: () => {}});
  });

  it('should render the profile', () => {
    const wrapper = component();
    expect(wrapper.find('input').first().props().value).toMatch(profile.givenName);
  });

});
