import { shallow } from 'enzyme';
import * as React from 'react';

import {panels, QuickTourReact, QuickTourReactProps, QuickTourReactState} from './component';


describe('QuickTourModalComponent', () => {

  let props: QuickTourReactProps;
  const lastPanel = panels.length - 1;

  const component = () => {
    return shallow<QuickTourReact, QuickTourReactProps, QuickTourReactState>
    (<QuickTourReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      learning: true,
      closeFunction: () => {}
    };
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

  it('should go to the panel we select from the breadcrumbs', () => {
    const wrapper = component();
    const panelNum = 2;
    wrapper.find('#breadcrumb' + panelNum).simulate('click');
    expect(wrapper.state().selected).toBe(panelNum);
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
    wrapper.find('#breadcrumb' + lastPanel).simulate('click');
    expect(wrapper.exists('#close')).toBeFalsy();
    expect(wrapper.find('#next').text()).toBe('Close');
  });

  it('should expand and retract the image when the resize icon is clicked', () => {
    const wrapper = component();
    // You cannot expand the image on the first page of the quick tour
    wrapper.find('#next').simulate('click');
    expect(wrapper.state().fullImage).toBeFalsy();
    wrapper.find('#expand-icon').simulate('click');
    expect(wrapper.state().fullImage).toBeTruthy();
    wrapper.find('#shrink-icon').simulate('click');
    expect(wrapper.state().fullImage).toBeFalsy();
  });

});

