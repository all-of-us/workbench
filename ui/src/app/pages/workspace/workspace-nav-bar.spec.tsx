import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';
import { mount } from 'enzyme';
import { mockNavigate } from 'setupTests';

import { WorkspaceNavBar } from 'app/pages/workspace/workspace-nav-bar';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';
import { NOTEBOOKS_TAB_NAME } from 'app/utils/user-apps-utils';

import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('WorkspaceNavBar', () => {
  let props: {};

  const component = () => {
    return mount(
      <MemoryRouter
        initialEntries={[
          `/${workspaceDataStub.namespace}/${workspaceDataStub.id}`,
        ]}
      >
        <Route path='/:ns/:wsid'>
          <WorkspaceNavBar {...props} />
        </Route>
      </MemoryRouter>,
      { attachTo: document.getElementById('root') }
    );
  };

  beforeEach(() => {
    props = {};

    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({
      config: {
        gsuiteDomain: 'fake-research-aou.org',
        enableResearchReviewPrompt: true,
      },
    });
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('should render the Data tab by default', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    expect(
      wrapper.find({ 'data-test-id': 'Data', 'aria-selected': true }).exists()
    ).toBeTruthy();
  });

  it('should highlight the active tab', () => {
    props = { tabPath: 'about' };
    const wrapper = component();
    expect(
      wrapper.find({ 'data-test-id': 'About', 'aria-selected': true }).exists()
    ).toBeTruthy();
  });

  it('should navigate on tab click', () => {
    const wrapper = component();

    wrapper.find({ 'data-test-id': 'Analysis' }).first().simulate('click');
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      NOTEBOOKS_TAB_NAME,
    ]);

    wrapper.find({ 'data-test-id': 'Data' }).first().simulate('click');
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'data',
    ]);

    wrapper.find({ 'data-test-id': 'About' }).first().simulate('click');
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'about',
    ]);
  });

  const setNeedsReviewPrompt = (needsReviewPrompt: boolean) => {
    const researchPurpose = {
      ...workspaceDataStub.researchPurpose,
      needsReviewPrompt,
    };
    currentWorkspaceStore.next({ ...workspaceDataStub, researchPurpose });
  };

  const setAdminLocked = (adminLocked: boolean) => {
    currentWorkspaceStore.next({ ...workspaceDataStub, adminLocked });
  };

  it('should not navigate on tab click if tab is disabled because it needs review', () => {
    // disables Data and Analysis tabs - see restrictTab()
    setNeedsReviewPrompt(true);

    const wrapper = component();

    wrapper.find({ 'data-test-id': 'Data' }).first().simulate('click');
    expect(mockNavigate).not.toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'data',
    ]);

    wrapper.find({ 'data-test-id': 'Analysis' }).first().simulate('click');
    expect(mockNavigate).not.toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      NOTEBOOKS_TAB_NAME,
    ]);
  });

  it('should not navigate on tab click if tab is disabled because it is admin-locked', () => {
    setAdminLocked(true);

    const wrapper = component();

    wrapper.find({ 'data-test-id': 'Data' }).first().simulate('click');
    expect(mockNavigate).not.toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      'data',
    ]);

    wrapper.find({ 'data-test-id': 'Analysis' }).first().simulate('click');
    expect(mockNavigate).not.toHaveBeenCalledWith([
      'workspaces',
      workspaceDataStub.namespace,
      workspaceDataStub.id,
      NOTEBOOKS_TAB_NAME,
    ]);
  });

  it('should disable Data and Analysis tab if workspace require review research purpose', () => {
    setNeedsReviewPrompt(true);

    const wrapper = component();

    expect(
      wrapper.find({ 'data-test-id': 'Data' }).first().props().disabled
    ).toBeTruthy();
    expect(
      wrapper.find({ 'data-test-id': 'Analysis' }).first().props().disabled
    ).toBeTruthy();
    expect(
      wrapper.find({ 'data-test-id': 'About' }).first().props().disabled
    ).toBeFalsy();
  });

  it('should not disable Data and Analysis tab if workspace does not require review research purpose', () => {
    setNeedsReviewPrompt(false);

    const wrapper = component();

    expect(
      wrapper.find({ 'data-test-id': 'Data' }).first().props().disabled
    ).toBeFalsy();
    expect(
      wrapper.find({ 'data-test-id': 'Analysis' }).first().props().disabled
    ).toBeFalsy();
    expect(
      wrapper.find({ 'data-test-id': 'About' }).first().props().disabled
    ).toBeFalsy();
  });

  it('should disable Data and Analysis tab if the workspace is admin-locked', () => {
    setAdminLocked(true);

    const wrapper = component();

    expect(
      wrapper.find({ 'data-test-id': 'Data' }).first().props().disabled
    ).toBeTruthy();
    expect(
      wrapper.find({ 'data-test-id': 'Analysis' }).first().props().disabled
    ).toBeTruthy();
    expect(
      wrapper.find({ 'data-test-id': 'About' }).first().props().disabled
    ).toBeFalsy();
  });

  it('should display the default CDR Version with no new version flag or upgrade modal visible', () => {
    const wrapper = component();

    expect(
      wrapper.find({ 'data-test-id': 'cdr-version' }).first().text()
    ).toEqual(CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION);

    expect(
      wrapper.find({ 'data-test-id': 'new-version-flag' }).exists()
    ).toBeFalsy();
    expect(
      wrapper.find({ 'data-test-id': 'cdr-version-upgrade-modal' }).exists()
    ).toBeFalsy();
  });

  it('should display an alternative CDR Version with a new version flag', () => {
    const altWorkspace = workspaceDataStub;
    altWorkspace.cdrVersionId =
      CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID;
    currentWorkspaceStore.next(altWorkspace);

    const wrapper = component();

    expect(
      wrapper.find({ 'data-test-id': 'cdr-version' }).first().text()
    ).toEqual(CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION);

    expect(
      wrapper.find({ 'data-test-id': 'new-version-flag' }).exists()
    ).toBeTruthy();
    expect(
      wrapper.find({ 'data-test-id': 'cdr-version-upgrade-modal' }).exists()
    ).toBeFalsy();
  });

  it('clicks the new version flag which should pop up the version upgrade modal', () => {
    const altWorkspace = workspaceDataStub;
    altWorkspace.cdrVersionId =
      CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID;
    currentWorkspaceStore.next(altWorkspace);

    const wrapper = component();

    expect(
      wrapper.find({ 'data-test-id': 'cdr-version-upgrade-modal' }).exists()
    ).toBeFalsy();

    const flag = wrapper.find({ 'data-test-id': 'new-version-flag' }).first();
    flag.simulate('click');

    expect(
      wrapper.find({ 'data-test-id': 'cdr-version-upgrade-modal' }).exists()
    ).toBeTruthy();
  });
});
