import {currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceNavBarReact} from 'app/views/workspace-nav-bar/component';
import {mount} from 'enzyme';
import {WorkspaceAccessLevel} from 'generated';
import * as React from 'react';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

describe('WorkspaceNavBarComponent', () => {

  let props: {};
  const workspace = {
    ...WorkspacesServiceStub.stubWorkspace(),
    accessLevel: WorkspaceAccessLevel.OWNER,
  };

  const component = () => {
    return mount(<WorkspaceNavBarReact {...props}/>, {attachTo: document.getElementById('root')});
  };

  beforeEach(() => {
    props = {};

    currentWorkspaceStore.next(workspace);
    urlParamsStore.next({ns: workspace.namespace, wsid: workspace.id});
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should highlight the active tab', () => {
    props = {tabPath: ''};
    const wrapper = component();
    expect(wrapper.find({'data-test-id': 'About', 'aria-selected': true}).exists()).toBeTruthy();
  });

  it('should navigate on tab click', () => {
    const navSpy = jest.fn();
    NavStore.navigate = navSpy;
    const wrapper = component();

    wrapper.find({'data-test-id': 'Cohorts'}).first().simulate('click');
    expect(navSpy).toHaveBeenCalledWith(
      ['/workspaces', workspace.namespace, workspace.id, 'cohorts']);
  });

  it('should call delete method when clicked', () => {
    const deleteSpy = jest.fn();
    props = {deleteFunction: deleteSpy};
    const wrapper = component();

    wrapper.find({'data-test-id': 'workspace-menu-button'}).first().simulate('click');
    wrapper.find({'data-test-id': 'trash'}).first().simulate('click');
    expect(deleteSpy).toHaveBeenCalled();
  });

  it('should call share method when clicked', () => {
    const shareSpy = jest.fn();
    props = {shareFunction: shareSpy};
    const wrapper = component();

    wrapper.find({'data-test-id': 'workspace-menu-button'}).first().simulate('click');
    wrapper.find({'data-test-id': 'share'}).first().simulate('click');
    expect(shareSpy).toHaveBeenCalled();
  });

});
