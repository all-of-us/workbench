import {shallow} from 'enzyme';
import * as React from 'react';

import {
  WorkspaceNavBarReact, WorkspaceNavBarReactProps
} from 'app/views/workspace-nav-bar/component';

xdescribe('QuickTourModalComponent', () => {

  let props: WorkspaceNavBarReactProps;

  const component = () => {
    return shallow(<WorkspaceNavBarReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      shareFunction: () => {},
      deleteFunction: () => {},
      workspace: {},
      tabPath: ''
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    });

  it('should highlight the active tab', () => {
    const wrapper = component();
    expect(wrapper.exists('[data-test-id="previous"]')).toBeFalsy();
  });

  it('should navigate on tab click', () => {
    const wrapper = component();
    // expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[0].title);
    wrapper.find('[data-test-id="next"]').simulate('click');
    // expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[1].title);
  });

  it('should update on workspace navigate', () => {
    const wrapper = component();
    const panelNum = 2;
    wrapper.find('[data-test-id="breadcrumb' + panelNum + '"]').simulate('click');
    // expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[panelNum].title);
  });

  it('should close menu on action', () => {
    const wrapper = component();
    // expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[0].title);
    wrapper.find('[data-test-id="next"]').simulate('click');
    // expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[1].title);
    wrapper.find('[data-test-id="previous"]').simulate('click');
    // expect(wrapper.find('[data-test-id="panel-title"]').text()).toBe(panels[0].title);
  });

});

