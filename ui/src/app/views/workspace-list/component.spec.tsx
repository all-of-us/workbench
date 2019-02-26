import {mount} from 'enzyme';
import * as React from 'react';

import {WorkspaceList} from './component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {WorkspacesApi, ProfileApi} from "generated/fetch/api";
import {Profile} from 'generated';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {userProfileStore} from 'app/utils/navigation';
import {WorkspacesApiStub} from "testing/stubs/workspaces-api-stub";


describe('WorkspaceList', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const reload = jest.fn();

  const component = () => {
    return mount(<WorkspaceList
    />);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    });

    userProfileStore.next({profile, reload});
  });

  it('displays the correct number of workspaces', () => {
    const wrapper = component();
  });

  it('enables editing workspaces', () => {

  });

  it('enables deleting workspaces', () => {

  });

  it('shows an error modal if there is an error deleting a workspace', () => {

  });

  it('enables sharing workspaces', () => {

  });

  it('enables duplicating workspaces', () => {

  });

});
