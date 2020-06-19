// Mock the navigate function but not userProfileStore
import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {Profile, ProfileApi, WorkspacesApi, WorkspaceAccessLevel} from 'generated/fetch';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {workspaceStubs, WorkspacesApiStub, userRolesStub} from 'testing/stubs/workspaces-api-stub';
import {serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {WorkspaceCard} from './workspace-card';

jest.mock('app/utils/navigation', () => ({
  ...(require.requireActual('app/utils/navigation')),
  navigate: jest.fn()
}));

describe('WorkspaceCard', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = (accessLevel: WorkspaceAccessLevel) => {
    return mount(
      <WorkspaceCard
       accessLevel={accessLevel}
       reload={reload}
       userEmail={'engle.r.fish@broadinstitute.org'}
       workspace={workspaceStubs[0]}
      />,
      {attachTo: document.getElementById('root')}
    );
  };

  beforeEach(() => {
    profileApi = new ProfileApiStub();
    registerApiClient(ProfileApi, profileApi);
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async() => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
    serverConfigStore.next({gsuiteDomain: 'abc'});
  });

  it('fetches user roles before opening the share dialog', async() => {
    const wrapper = component(WorkspaceAccessLevel.OWNER);
    await waitOneTickAndUpdate(wrapper);
    const userRolesSpy = jest.spyOn(workspacesApi(), 'getFirecloudWorkspaceUserRoles');

    expect(wrapper.exists('[data-test-id="workspace-share-modal"]')).toBeFalsy();

    // Click the snowman menu
    wrapper.find('[data-test-id="workspace-card-menu"]').first().simulate('click');

    // Disabled tooltip should not show on hover.
    const shareEl = wrapper.find('[data-test-id="Share-menu-item"]').first();
    shareEl.simulate('mouseenter');
    expect(wrapper.exists('[data-test-id="workspace-share-disabled-tooltip"]')).toBeFalsy();

    // Click should open the share modal.
    shareEl.simulate('click');
    await waitOneTickAndUpdate(wrapper);

    // We should have called the userRoles API before opening the share modal.
    expect(userRolesSpy).toHaveBeenCalledTimes(1);

    // The share modal should have been opened w/ the correct props.
    const shareModal = wrapper.find('[data-test-id="workspace-share-modal"]').first();
    expect(shareModal.prop('userRoles')).toEqual(userRolesStub);

  });

  it('disables sharing for non-owners', async() => {
    const wrapper = component(WorkspaceAccessLevel.WRITER);
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.exists('[data-test-id="workspace-share-modal"]')).toBeFalsy();

    // Click the snowman menu.
    wrapper.find('[data-test-id="workspace-card-menu"]').first().simulate('click');
    const shareEl = wrapper.find('[data-test-id="Share-menu-item"]').first();

    // Hover should show the disabled tooltip.
    shareEl.simulate('mouseenter');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="workspace-share-disabled-tooltip"]')).toBeTruthy();

    // The share modal should not open on click.
    shareEl.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="workspace-share-modal"]')).toBeFalsy();
  });

});
