import {mount} from 'enzyme';
import * as React from 'react';

import {
  registerApiClient, workspacesApi,
} from 'app/services/swagger-fetch-clients';
import {navigate, serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {Profile, ProfileApi, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {
  userRolesStub,
  WorkspacesApiStub,
  workspaceStubs,
  WorkspaceStubVariables
} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceList} from './workspace-list';

// Mock the navigate function but not userProfileStore
jest.mock('app/utils/navigation', () => ({
  ...(require.requireActual('app/utils/navigation')),
  navigate: jest.fn()
}));

describe('WorkspaceList', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = () => {
    return mount(<WorkspaceList/>, {attachTo: document.getElementById('root')});
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async() => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
    serverConfigStore.next({gsuiteDomain: 'abc'});
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

  it('fetches user roles before opening the share dialog', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const userRolesSpy = jest.spyOn(workspacesApi(), 'getFirecloudWorkspaceUserRoles');

    // Click the snowman menu
    wrapper.find('[data-test-id="workspace-card-menu"]').first().simulate('click');
    // Click the share menu item
    wrapper.find('[data-test-id="Share-menu-item"]').first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);

    // We should have called the userRoles API before opening the share modal.
    expect(userRolesSpy).toHaveBeenCalledTimes(1);

    // The share modal should have been opened w/ the correct props.
    const shareModal = wrapper.find('[data-test-id="workspace-share-modal"]').first();
    expect(shareModal.prop('userRoles')).toEqual(userRolesStub);
  });

});
