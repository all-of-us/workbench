import {mount} from 'enzyme';
import * as React from 'react';

import {
  registerApiClient, workspacesApi,
} from 'app/services/swagger-fetch-clients';
import {navigate} from 'app/utils/navigation';
import {Profile, ProfileApi, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceList} from './workspace-list';
import {profileStore, serverConfigStore} from "app/utils/stores";

// Mock the navigate function but not userProfileStore
jest.mock('app/utils/navigation', () => ({
  ...(jest.requireActual('app/utils/navigation')),
  navigate: jest.fn()
}));

describe('WorkspaceList', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
    spinnerVisible: false
  };

  const component = () => {
    return mount(<WorkspaceList {...props}/>, {attachTo: document.getElementById('root')});
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

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
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
        .map(c => c.text());
    expect(cardNameList).toEqual(workspaceStubs.map(w => w.name));
  });

  it('navigates when clicking on the workspace name', async() => {
    const workspace = workspaceStubs[0];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="workspace-card-name"]').first().simulate('click');
    expect(navigate).toHaveBeenCalledWith(
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
});
