import {mount} from 'enzyme';
import * as React from 'react';

import {SideNav, SideNavItem} from './side-nav';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

describe('SideNav', () => {
  const props = {
    profile: ProfileStubVariables.PROFILE_STUB,
    onToggleSideNav: () => {},
  };
  const component = () => mount(<SideNav {...props}/>);
  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('disables options when user not registered', () => {
    const wrapper = mount(<SideNav {...props} profile={{...ProfileStubVariables.PROFILE_STUB, accessTierShortNames: []}}/>);
    wrapper.find('[data-test-id="user-options"]').first().simulate('click');
    // These are our expected items to be disabled when you are not registered
    let disabledItemText = ['Your Workspaces', 'Featured Workspaces', 'User Support Hub'];
    const sideNavItems = wrapper.find(SideNavItem);
    let disabledItems = sideNavItems.filterWhere(sideNavItem => sideNavItem.props().disabled);
    sideNavItems.forEach(sideNavItem => {
      const disabled = sideNavItem.props().disabled;
      const sideNavItemText = sideNavItem.text();
      if (disabledItemText.includes(sideNavItemText)) {
        disabledItems = disabledItems.filterWhere(disabledItem => disabledItem.text() !== sideNavItem.text());
        disabledItemText = disabledItemText.filter(textItem => textItem !== sideNavItemText);
        expect(disabled).toBeTruthy();
      }
    });
    // Ensure all expected items to be found.
    expect(disabledItemText.length).toBe(0);
    // Ensure there are no other disabled items that we do not expect.
    expect(disabledItems.length).toBe(0);
  });

});
