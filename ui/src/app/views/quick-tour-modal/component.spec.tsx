import {QuickTourReact} from './component';

import * as React from 'react';
import { shallow } from 'enzyme';


describe('QuickTourModalComponent', () => {

  let props: {
    learning: boolean,
    closeFunction: Function
  };

  const component = () => {
    return shallow(<QuickTourReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      learning: true,
      closeFunction: () => {}
    }
  });

  it('should render, should have a next and close button', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    expect(wrapper.exists('#close')).toBeTruthy();
    expect(wrapper.exists('#next')).toBeTruthy();
  });

  it('should not show the previous button when we are on the first slide', () => {
    const wrapper = component();
    expect(wrapper.exists('#previous')).toBeFalsy();
  });

  it('should go to the next slide when we click next', () => {
    const wrapper = component();
    const firstSlide = wrapper.state().selected;
    wrapper.find('#next').simulate('click');
    expect(wrapper.state().selected).toBe(firstSlide + 1);
  });

  it('should go to the previous slide when we click previous', () => {
    const wrapper = component();
    const firstSlide = wrapper.state().selected;
    wrapper.find('#next').simulate('click');
    wrapper.find('#previous').simulate('click');
    expect(wrapper.state().selected).toBe(firstSlide);
  });

  it('should not show the next button when we are on the last slide', () => {
    const wrapper = component();
    let i = 0;
    do {
      wrapper.find('#next').simulate('click');
      i++;
    }
    while (i < 4);
    expect(wrapper.exists('#close')).toBeFalsy();
    expect(wrapper.find('#next').text()).toBe('Close');
  });

  it('should expand the image when we click the expand icon', () => {
    const wrapper = component();
    expect(wrapper.state().fullImage).toBeFalsy();
    // TODO
  });

  it('should retract the image when we click the retract icon', () => {

  });

});

