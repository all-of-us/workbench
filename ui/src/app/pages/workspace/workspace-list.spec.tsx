import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import {
  registerApiClient
} from 'app/services/swagger-fetch-clients';
import {Profile, ProfileApi, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {buildWorkspaceStub, workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces';
import RSelect from 'react-select';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceList} from './workspace-list';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {mockNavigate} from 'setupTests';

describe('WorkspaceList', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();
  let workspacesApiStub: WorkspacesApiStub;

  const props = {
    hideSpinner: () => {},
    showSpinner: () => {}
  };

  const component = () => {
    return mount(<WorkspaceList {...props}/>, {attachTo: document.getElementById('root')});
  };

  async function pickAccessLevel(wrapper: ReactWrapper, label: string) {
    wrapper
      .find(RSelect)
      .instance().setState({menuIsOpen: true});
    await waitOneTickAndUpdate(wrapper);

    wrapper
      .find(RSelect)
      .find({type: 'option'})
      .findWhere(e => e.text() === label)
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
  }

  function getCardNames(wrapper: ReactWrapper) {
    return wrapper.find('[data-test-id="workspace-card-name"]').map(c => c.text());
  }

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async() => {
      const newProfile = await profileApi.getMe();
      profileStore.set({profile: newProfile, load, reload, updateCache});
    });

    profileStore.set({profile, load, reload, updateCache});
    serverConfigStore.set({config: {gsuiteDomain: 'abc', enableResearchReviewPrompt: true}});
  });

  it('displays the correct number of workspaces', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(getCardNames(wrapper)).toEqual(workspaceStubs.map(w => w.name));
  });

  it('navigates when clicking on the workspace name', async() => {
    const workspace = workspaceStubs[0];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="workspace-card-name"]').first().simulate('click');
    expect(mockNavigate).toHaveBeenCalledWith(
      ['workspaces', workspace.namespace, workspace.id, 'data']);
  });

  it('has the correct permissions classes', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="workspace-card"]').first()
      .find('[data-test-id="workspace-access-level"]').text())
      .toBe(WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION);
  });

  it('should show Research Purpose Review Modal if workspace require review', async() => {
    const workspace = workspaceStubs[0];
    workspace.researchPurpose.needsReviewPrompt = true;
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="workspace-card-name"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="workspace-review-modal"]').length).toBeGreaterThan(0);
  });

  it('filters workspace list', async() => {
    const workspaceRead = buildWorkspaceStub('read');
    const workspaceWrite = buildWorkspaceStub('write');
    const workspaceOwn = buildWorkspaceStub('own');
    workspacesApiStub.workspaces = [workspaceRead, workspaceWrite, workspaceOwn];
    workspacesApiStub.workspaceAccess = new Map([
      [workspaceRead.id, WorkspaceAccessLevel.READER],
      [workspaceWrite.id, WorkspaceAccessLevel.WRITER],
      [workspaceOwn.id, WorkspaceAccessLevel.OWNER],
    ]);

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(getCardNames(wrapper).length).toEqual(3);

    await pickAccessLevel(wrapper, 'Reader');
    expect(getCardNames(wrapper)).toEqual([workspaceRead.name]);

    await pickAccessLevel(wrapper, 'Owner');
    expect(getCardNames(wrapper)).toEqual([workspaceOwn.name]);

    await pickAccessLevel(wrapper, 'Writer');
    expect(getCardNames(wrapper)).toEqual([workspaceWrite.name]);

    await pickAccessLevel(wrapper, 'All');
    expect(getCardNames(wrapper).length).toEqual(3);
  });
});
