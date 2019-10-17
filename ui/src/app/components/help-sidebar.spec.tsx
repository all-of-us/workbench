import * as React from 'react';

import {mount} from 'enzyme';
import {HelpSidebar} from './help-sidebar';

describe('SidebarContent', () => {
  it('should render', () => {
    const wrapper = mount(<HelpSidebar location='data' />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
