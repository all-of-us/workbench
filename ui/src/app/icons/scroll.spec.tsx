import * as React from 'react';
import { mount } from 'enzyme';

import { Scroll } from './scroll';

describe('ScrollComponent', () => {
  it('should render', () => {
    const wrapper = mount(<Scroll dir='left' shade='light' />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
