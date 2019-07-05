import {mount} from 'enzyme';
import * as React from 'react';

import {WorkspaceList} from './workspace-list';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {Profile} from 'generated';
import {ProfileApi, WorkspacesApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {navigate, serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {
  WorkspacesApiStub,
  workspaceStubs,
  WorkspaceStubVariables
} from 'testing/stubs/workspaces-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

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
    return mount(<WorkspaceList/>);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile as unknown as Profile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
    serverConfigStore.next({useBillingProjectBuffer: false, gsuiteDomain: "abc"});
  });

  it('displays the correct number of workspaces', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
        .map(c => c.text());
    expect(cardNameList).toEqual(workspaceStubs.map(w => w.name));
  });

  it('enables editing workspaces', async () => {
    const workspace = workspaceStubs[0];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="workspace-card-name"]').first().simulate('click');
    expect(navigate).toHaveBeenCalledWith(
      ['workspaces', workspace.namespace, workspace.id, 'data']);
  });

  it('has the correct permissions classes', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="workspace-card"]').first()
      .find('[data-test-id="workspace-access-level"]').text())
      .toBe(WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION);
  });

  // TODO (RW-2625) Exercise a workspace share code path from the workspace list.

  // Note: this spec is not testing the Popup menus on workspace cards due to an issue using
  //    PopupTrigger in the test suite.

});
