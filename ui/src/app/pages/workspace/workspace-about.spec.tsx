import {mount} from 'enzyme';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {WorkspaceAbout} from './workspace-about';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {ClusterApi, Profile, ProfileApi, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {cdrVersionStore, currentWorkspaceStore, serverConfigSworktore, userProfileStore} from 'app/utils/navigation';
import {userRolesStub, workspaceStubs} from 'testing/stubs/workspaces-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {CdrVersionsStubVariables, cdrVersionListResponse} from 'testing/stubs/cdr-versions-api-stub';

describe('WorkspaceAbout', () => {
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;
  const reload = jest.fn();
  const updateCache = jest.fn();

  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };

  const component = () => {
    return mount(<WorkspaceAbout/>);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ClusterApi, new ClusterApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache});
    currentWorkspaceStore.next(workspace);
    serverConfigStore.next({
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    });
    cdrVersionStore.next(cdrVersionListResponse);
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should display research purpose', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="researchPurpose"]'))
      .toBeTruthy();
  });

  it('should display workspace collaborators', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    userRolesStub.forEach((role, i) => {
      const userRoleText = wrapper.find('[data-test-id="workspaceUser-' + i + '"]')
        .text();
      expect(userRoleText).toContain(role.email);
      expect(userRoleText).toContain(role.role);
    });
  });

  it('should allow the user to open the share modal', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="workspaceShareButton"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="workspaceShareModal"]'))
      .toBeTruthy();
  });

  it('should display cdr version', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="cdrVersion"]').text())
      .toContain(CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION);
  });

  it('should display workspace metadata', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="dataAccessLevel"]').text())
      .toContain(fp.capitalize(workspace.dataAccessLevel.toString()));
    expect(wrapper.find('[data-test-id="creationDate"]').text())
      .toContain(new Date(workspace.creationTime).toDateString());
    expect(wrapper.find('[data-test-id="lastUpdated"]').text())
      .toContain(new Date(workspace.lastModifiedTime).toDateString());
  });
});
