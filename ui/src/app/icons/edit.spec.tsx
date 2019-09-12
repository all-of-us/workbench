import {mount} from 'enzyme';
import * as React from 'react';

import {
  EditComponentProps,
  EditComponentReact
} from './edit';

// Note that the description is slightly different from the component for easier test filtering.
describe('EditIconComponent', () => {

  let props: EditComponentProps;

  const component = () => {
    return mount(<EditComponentReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      disabled: false,
      enableHoverEffect: true,
      style: {}
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should change style if disabled', () => {
    const wrapper = component();
    const style = wrapper.find('svg').props().style;
    props.disabled = true;
    const disabledWrapper = component();
    const disabledStyle = disabledWrapper.find('svg').props().style;
    expect(style).not.toEqual(disabledStyle);
  });

  it('should change style on mouse over', () => {
    const wrapper = component();
    const style = wrapper.find('svg').props().style;
    wrapper.simulate('mouseover');
    const hoverStyle = wrapper.find('svg').props().style;
    expect(style).not.toEqual(hoverStyle);
  });

  it('should change style on mouse out', () => {
    const wrapper = component();
    wrapper.simulate('mouseover');
    const hoverStyle = wrapper.find('svg').props().style;
    wrapper.simulate('mouseleave');
    const mouseOutStyle = wrapper.find('svg').props().style;
    expect(mouseOutStyle).not.toEqual(hoverStyle);
  });

});
