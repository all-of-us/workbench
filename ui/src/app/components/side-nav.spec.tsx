import {mount} from 'enzyme';
import * as React from 'react';

import {SideNav, SideNavItem, SideNavProps} from './side-nav';
import {ProfileStubVariables} from "../../testing/stubs/profile-api-stub";
import {DataAccessLevel} from "../../generated/fetch";


describe('SideNav', () => {
  const existingNames = [];
  const props: SideNavProps = {
    profile: ProfileStubVariables.PROFILE_STUB,
    bannerAdminActive: false,
    workspaceAdminActive: false,
    homeActive: false,
    libraryActive: false,
    onToggleSideNav: () => {},
    profileActive: false,
    userAdminActive: false,
    workspacesActive: false,
  };
  const component = () => mount(<SideNav {...props}/>);
  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('disables options when user not registered', () => {
    const wrapper = mount(<SideNav {...props} profile={{...ProfileStubVariables.PROFILE_STUB,
      dataAccessLevel: DataAccessLevel.Unregistered}}/>);
    const disabledItems = ['Your Workspaces', 'Featured Workspaces', 'User Support'];
    wrapper.find(SideNavItem).forEach(sideNavItem => {
      if (disabledItems.includes(sideNavItem.text())) {
        expect(sideNavItem.props().disabled).toBeTruthy();
      }
    });
  });

});
