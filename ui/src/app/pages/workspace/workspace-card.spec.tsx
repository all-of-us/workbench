// Mock the navigate function but not userProfileStore
import {mount} from 'enzyme';
import {Profile, ProfileApi, WorkspacesApi} from 'generated/fetch';
import * as React from 'react';
import {ProfileApiStub, ProfileStubVariables} from 'src/testing/stubs/profile-api-stub';
import {WorkspaceAccessLevel} from 'src/generated/fetch';
import {registerApiClient, workspacesApi} from 'src/app/services/swagger-fetch-clients';
import {workspaceStubs, WorkspacesApiStub, userRolesStub} from 'src/testing/stubs/workspaces-api-stub';
import {serverConfigStore, userProfileStore} from 'src/app/utils/navigation';
import {waitOneTickAndUpdate} from 'src/testing/react-test-helpers';
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

  const component = () => {
    return mount(
      <WorkspaceCard
       accessLevel={WorkspaceAccessLevel.OWNER}
       reload={reload}
       userEmail={'engle.r.fish@broadinstitute.org'}
       workspace={workspaceStubs[0]}
      />,
      {attachTo: document.getElementById('root')}
    );
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