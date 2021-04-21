import {mount} from 'enzyme';
import * as React from 'react';

import {WorkspaceNavBarReact} from 'app/pages/workspace/workspace-nav-bar';
import {currentWorkspaceStore, NavStore, serverConfigStore, urlParamsStore} from 'app/utils/navigation';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {CdrVersionsStubVariables, cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';
import {cdrVersionStore} from "app/utils/stores";

describe('WorkspaceNavBarComponent', () => {

  let props: {};

  const component = () => {
    return mount(<WorkspaceNavBarReact {...props}/>, {attachTo: document.getElementById('root')});
  };

  beforeEach(() => {
    props = {};

    currentWorkspaceStore.next(workspaceDataStub);
    urlParamsStore.next({ns: workspaceDataStub.namespace, wsid: workspaceDataStub.id});
    serverConfigStore.next({
      gsuiteDomain: 'fake-research-aou.org', enableResearchReviewPrompt: true});
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should highlight the active tab', () => {
    props = {tabPath: 'about'};
    const wrapper = component();
    expect(wrapper.find({'data-test-id': 'About', 'aria-selected': true}).exists()).toBeTruthy();
  });

  it('should navigate on tab click', () => {
    const navSpy = jest.fn();
    NavStore.navigate = navSpy;
    const wrapper = component();

    wrapper.find({'data-test-id': 'Data'}).first().simulate('click');
    expect(navSpy).toHaveBeenCalledWith(
      ['/workspaces', workspaceDataStub.namespace, workspaceDataStub.id, 'data']);
  });

  it('should disable Data and Analysis tab if workspace require review research purpose', () => {
    const navSpy = jest.fn();
    NavStore.navigate = navSpy;
    workspaceDataStub.researchPurpose.needsReviewPrompt = true;

    const wrapper = component();

    expect(wrapper.find({'data-test-id': 'Data'}).first().props().disabled).toBeTruthy();
    expect(wrapper.find({'data-test-id': 'Analysis'}).first().props().disabled).toBeTruthy();
    expect(wrapper.find({'data-test-id': 'About'}).first().props().disabled).toBeFalsy();

  });

  it('should display the default CDR Version with no new version flag or upgrade modal visible', () => {
    const wrapper = component();

    expect(wrapper.find({'data-test-id': 'cdr-version'}).first().text())
        .toEqual(CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION);

    expect(wrapper.find({'data-test-id': 'new-version-flag'}).exists()).toBeFalsy();
    expect(wrapper.find({'data-test-id': 'cdr-version-upgrade-modal'}).exists()).toBeFalsy();
  })

  it('should display an alternative CDR Version with a new version flag', () => {
    const altWorkspace = workspaceDataStub;
    altWorkspace.cdrVersionId = CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID;
    currentWorkspaceStore.next(altWorkspace);

    const wrapper = component();

    expect(wrapper.find({'data-test-id': 'cdr-version'}).first().text())
        .toEqual(CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION);

    expect(wrapper.find({'data-test-id': 'new-version-flag'}).exists()).toBeTruthy();
    expect(wrapper.find({'data-test-id': 'cdr-version-upgrade-modal'}).exists()).toBeFalsy();
  })

  it('clicks the new version flag which should pop up the version upgrade modal', () => {
    const altWorkspace = workspaceDataStub;
    altWorkspace.cdrVersionId = CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID;
    currentWorkspaceStore.next(altWorkspace);

    const wrapper = component();

    expect(wrapper.find({'data-test-id': 'cdr-version-upgrade-modal'}).exists()).toBeFalsy();

    const flag = wrapper.find({'data-test-id': 'new-version-flag'}).first();
    flag.simulate('click');

    expect(wrapper.find({'data-test-id': 'cdr-version-upgrade-modal'}).exists()).toBeTruthy();
  })
});
