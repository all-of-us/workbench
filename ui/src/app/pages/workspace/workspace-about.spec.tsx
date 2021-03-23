import {mount} from 'enzyme';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {WorkspaceAbout} from './workspace-about';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {Authority, RuntimeApi, Profile, ProfileApi, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {cdrVersionStore, currentWorkspaceStore, serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {userRolesStub, workspaceStubs} from 'testing/stubs/workspaces';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {CdrVersionsStubVariables, cdrVersionListResponse} from 'testing/stubs/cdr-versions-api-stub';
import {SpecificPopulationItems} from './workspace-edit-text';

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
    registerApiClient(RuntimeApi, new RuntimeApiStub());

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
    // Research Purpose: Drug, Population and Ethics
    expect(wrapper.find('[data-test-id="primaryResearchPurpose"]').length).toBe(3);
    // Primary Purpose: Education
    expect(wrapper.find('[data-test-id="primaryPurpose"]').length).toBe(1);
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
    expect(wrapper.find('[data-test-id="accessTierShortName"]').text())
      .toContain(fp.capitalize(workspace.accessTierShortName));
    expect(wrapper.find('[data-test-id="creationDate"]').text())
      .toContain(new Date(workspace.creationTime).toDateString());
    expect(wrapper.find('[data-test-id="lastUpdated"]').text())
      .toContain(new Date(workspace.lastModifiedTime).toDateString());
  });

  it('should not manipulate SpecificPopulationItems object on page load', async () => {
    const raceSubCategoriesBeforePageload = SpecificPopulationItems[0].subCategory.length;
    const wrapper = component();
    const raceSubCategoriesAfterPageLoad = SpecificPopulationItems[0].subCategory.length;
    expect(raceSubCategoriesBeforePageload).toBe(raceSubCategoriesAfterPageLoad);
  });

  it('should not display Publish/Unpublish buttons without appropriate Authority', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="publish-button"]')).toBeFalsy()
    expect(wrapper.exists('[data-test-id="unpublish-button"]')).toBeFalsy()
  });

  it('should display Publish/Unpublish buttons with FEATUREDWORKSPACEADMIN Authority', async () => {
    const profileWithAuth = {...ProfileStubVariables.PROFILE_STUB, authorities: [Authority.FEATUREDWORKSPACEADMIN]};
    userProfileStore.next({profile: profileWithAuth, reload, updateCache});

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="publish-button"]')).toBeTruthy()
    expect(wrapper.exists('[data-test-id="unpublish-button"]')).toBeTruthy()
  });

  it('should display Publish/Unpublish buttons with DEVELOPER Authority', async () => {
    const profileWithAuth = {...ProfileStubVariables.PROFILE_STUB, authorities: [Authority.DEVELOPER]};
    userProfileStore.next({profile: profileWithAuth, reload, updateCache});

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="publish-button"]')).toBeTruthy()
    expect(wrapper.exists('[data-test-id="unpublish-button"]')).toBeTruthy()
  });

});
