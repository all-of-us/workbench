import * as React from 'react';
import { mount } from 'enzyme';

import * as Authentication from 'app/utils/authentication';
import * as ProfilePicture from 'app/utils/profile-picture';
import { notificationStore } from 'app/utils/stores';

import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { SideNav, SideNavItem, SideNavProps } from './side-nav';

describe('SideNav', () => {
  const props: SideNavProps = {
    profile: ProfileStubVariables.PROFILE_STUB,
    onToggleSideNav: () => {},
  };

  const spy = jest.spyOn(ProfilePicture, 'getProfilePictureSrc');
  spy.mockReturnValue('lol.png');

  const component = () => mount(<SideNav {...props} />);

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show an error when signout fails', () => {
    const signOutSpy = jest.spyOn(Authentication, 'signOut');
    signOutSpy.mockImplementation(() => {
      throw new Error();
    });
    const wrapper = mount(
      <SideNav
        {...props}
        profile={{
          ...ProfileStubVariables.PROFILE_STUB,
          givenName: 'TestGivenName',
          familyName: 'TestFamilyName',
        }}
      />
    );
    wrapper
      .find('[data-test-id="TestGivenNameTestFamilyName-menu-item"]')
      .first()
      .simulate('click');

    expect(notificationStore.get()).toBeNull();
    wrapper.find('[data-test-id="SignOut-menu-item"]').simulate('click');
    expect(notificationStore.get()).toBeTruthy();
  });

  it('disables options when user not registered', () => {
    const wrapper = mount(
      <SideNav
        {...props}
        profile={{
          ...ProfileStubVariables.PROFILE_STUB,
          accessTierShortNames: [],
          givenName: 'Tester',
          familyName: 'MacTesterson',
        }}
      />
    );
    wrapper
      .find('[data-test-id="TesterMacTesterson-menu-item"]')
      .first()
      .simulate('click');
    // These are our expected items to be disabled when you are not registered
    let disabledItemText = [
      'Your Workspaces',
      'Featured Workspaces',
      'User Support Hub',
    ];
    const sideNavItems = wrapper.find(SideNavItem);
    let disabledItems = sideNavItems.filterWhere(
      (sideNavItem) => sideNavItem.props().disabled
    );
    sideNavItems.forEach((sideNavItem) => {
      const disabled = sideNavItem.props().disabled;
      const sideNavItemText = sideNavItem.text();
      if (disabledItemText.includes(sideNavItemText)) {
        disabledItems = disabledItems.filterWhere(
          (disabledItem) => disabledItem.text() !== sideNavItem.text()
        );
        disabledItemText = disabledItemText.filter(
          (textItem) => textItem !== sideNavItemText
        );
        expect(disabled).toBeTruthy();
      }
    });
    // Ensure all expected items to be found.
    expect(disabledItemText.length).toBe(0);
    // Ensure there are no other disabled items that we do not expect.
    expect(disabledItems.length).toBe(0);
  });
});
