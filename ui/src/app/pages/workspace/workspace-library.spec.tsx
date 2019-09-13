import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {userProfileStore} from 'app/utils/navigation';
import {FeaturedWorkspacesConfigApi, Profile, ProfileApi, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {FeaturedWorkspacesConfigApiStub} from 'testing/stubs/featured-workspaces-config-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {WorkspacesApiStub, buildWorkspaceStubs} from 'testing/stubs/workspaces-api-stub';
import {WorkspaceLibrary} from './workspace-library';

// Mock the navigate function but not userProfileStore
jest.mock('app/utils/navigation', () => ({
  ...(require.requireActual('app/utils/navigation')),
  navigate: jest.fn()
}));

describe('WorkspaceLibrary', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  const suffixes = ["", "_1"];
  let profileApi: ProfileApiStub;
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = () => {
    return mount(<WorkspaceLibrary
      enablePublishedWorkspaces={true}
    />);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(FeaturedWorkspacesConfigApi, new FeaturedWorkspacesConfigApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
  });

  it('renders', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should display featured workspaces', async () => {
    const publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true
    }));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([publishedWorkspaceStubs[0].name]);
  });

  it('should not display unpublished workspaces', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList.length).toBe(0);
  });

  it('should display published workspaces', async () => {
    const publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map(w => ({
      ...w,
      published: true,
    }));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub(publishedWorkspaceStubs));
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="Published Workspaces"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const cardNameList = wrapper.find('[data-test-id="workspace-card-name"]')
      .map(c => c.text());
    expect(cardNameList).toEqual([publishedWorkspaceStubs[1].name]);
  });

});
