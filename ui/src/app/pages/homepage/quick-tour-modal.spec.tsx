import {shallow} from 'enzyme';
import * as React from 'react';

import {panels, QuickTourReact, QuickTourReactProps, QuickTourReactState} from './quick-tour-modal';

describe('QuickTourModalComponent', () => {

  let props: QuickTourReactProps;
  const lastPanel = panels.length - 1;

  const component = () => {
    return shallow<QuickTourReact, QuickTourReactProps, QuickTourReactState>
    (<QuickTourReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      closeFunction: () => {}
    };
  });

  it('should render, should have a next and closeFunction button', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    expect(wrapper.exists('[data-test-id="closeFunction"]')).toBeTruthy();
    expect(wrapper.exists('[data-test-id="next"]')).toBeTruthy();
  });

  it('should not show the previous button when we are on the first slide', () => {
    const wrapper = component();
    expect(wrapper.exists('[data-test-id="previous"]')).toBeFalsy();
  });

  it('should go to the next slide when we click next', () => {
    const wrapper = component();
    expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[0].title);
    wrapper.find('[data-test-id="next"]').simulate('click');
    expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[1].title);
  });

  it('should go to the panel we select from the breadcrumbs', () => {
    const wrapper = component();
    const panelNum = 2;
    wrapper.find('[data-test-id="breadcrumb' + panelNum + '"]').simulate('click');
    expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[panelNum].title);
  });

  it('should go to the previous slide when we click previous', () => {
    const wrapper = component();
    expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[0].title);
    wrapper.find('[data-test-id="next"]').simulate('click');
    expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[1].title);
    wrapper.find('[data-test-id="previous"]').simulate('click');
    expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[0].title);
  });

  it('should not show the next button when we are on the last slide', () => {
    const wrapper = component();
    wrapper.find('[data-test-id="breadcrumb' + lastPanel + '"]').simulate('click');
    expect(wrapper.exists('[data-test-id="closeFunction"]')).toBeFalsy();
    expect(wrapper.find('[data-test-id="next"]').childAt(0).text()).toBe('Close');
  });

  it('should expand and retract the image when the resize icon is clicked', () => {
    const wrapper = component();
    // You cannot expand the image on the first page of the quick tour
    wrapper.find('[data-test-id="next"]').simulate('click');
    expect(wrapper.find('[data-test-id="full-image-wrapper"]').length).toBe(0);
    wrapper.find('[data-test-id="expand-icon"]').simulate('click');
    expect(wrapper.find('[data-test-id="full-image-wrapper"]').length).toBe(1);
    wrapper.find('[data-test-id="shrink-icon"]').simulate('click');
    expect(wrapper.find('[data-test-id="full-image-wrapper"]').length).toBe(0);
  });

});

