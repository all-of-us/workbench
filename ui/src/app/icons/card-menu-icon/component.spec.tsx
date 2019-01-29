import {mount} from 'enzyme';
import * as React from 'react';

import {
  CardMenuIconComponentReact
} from './component';

// Note that the description is slightly different from the component for easier test filtering.
describe('CardMenuIconComponent', () => {
  const component = () => {
    return mount(<CardMenuIconComponentReact />);
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});
