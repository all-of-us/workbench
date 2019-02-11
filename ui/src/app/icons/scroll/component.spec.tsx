import { mount } from 'enzyme';
import * as React from 'react';

import {Scroll} from './component';

describe('ScrollComponent', () => {
  it('should render', () => {
    const wrapper = mount(<Scroll dir='left' shade='light'/>)
    expect(wrapper.exists()).toBeTruthy();
  });
});
