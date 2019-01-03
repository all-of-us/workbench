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
    expect(wrapper.find('.panel-title').text()).toBe(panels[0].title);
    wrapper.find('#next').simulate('click');
    expect(wrapper.find('.panel-title').text()).toBe(panels[1].title);
  });

  it('should go to the panel we select from the breadcrumbs', () => {
    const wrapper = component();
    const panelNum = 2;
    wrapper.find('#breadcrumb' + panelNum).simulate('click');
    expect(wrapper.find('.panel-title').text()).toBe(panels[panelNum].title);
  });

  it('should go to the previous slide when we click previous', () => {
    const wrapper = component();
    expect(wrapper.find('.panel-title').text()).toBe(panels[0].title);
    wrapper.find('#next').simulate('click');
    expect(wrapper.find('.panel-title').text()).toBe(panels[1].title);
    wrapper.find('#previous').simulate('click');
    expect(wrapper.find('.panel-title').text()).toBe(panels[0].title);
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
    expect(wrapper.find('.full-image-wrapper').length).toBe(0);
    wrapper.find('#expand-icon').simulate('click');
    expect(wrapper.find('.full-image-wrapper').length).toBe(1);
    wrapper.find('#shrink-icon').simulate('click');
    expect(wrapper.find('.full-image-wrapper').length).toBe(0);
  });

});

