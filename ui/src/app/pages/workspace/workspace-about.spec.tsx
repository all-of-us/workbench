import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { mount } from 'enzyme';

import {
  Authority,
  ProfileApi,
  RuntimeApi,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderWithRouter,
  waitForNoSpinner,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { userRolesStub, workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceAbout } from './workspace-about';
import { SpecificPopulationItems } from './workspace-edit-text';

describe('WorkspaceAbout', () => {
  const profile = ProfileStubVariables.PROFILE_STUB;
  let profileApi: ProfileApiStub;
  let user;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const workspace = {
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.OWNER,
  };

  const component = () => {
    return mount(
      <MemoryRouter>
        <WorkspaceAbout hideSpinner={() => {}} showSpinner={() => {}} />
      </MemoryRouter>
    );
  };

  const componentAlt = () => {
    return renderWithRouter(
      <WorkspaceAbout hideSpinner={() => {}} showSpinner={() => {}} />
    );
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(RuntimeApi, new RuntimeApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });

    profileStore.set({ profile, load, reload, updateCache });
    currentWorkspaceStore.next(workspace);
    serverConfigStore.set({
      config: {
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
        enableEraCommons: true,
      },
    });
    cdrVersionStore.set(cdrVersionTiersResponse);
    user = userEvent.setup();
  });

  it('should render', async () => {
    componentAlt();
    await waitForNoSpinner();
    screen.getByText(/primary purpose of project/i);
  });

  it('should display research purpose', async () => {
    componentAlt();
    await waitForNoSpinner();
    expect(screen.getByText('Research Purpose')).toBeInTheDocument();
    // Research Purpose: Drug, Population and Ethics
    expect(screen.getAllByTestId('primaryResearchPurpose').length).toBe(3);
    // Primary Purpose: Education
    expect(screen.getByTestId('primaryPurpose')).toBeInTheDocument();
  });

  it('should display workspace collaborators', async () => {
    componentAlt();
    await waitForNoSpinner();
    userRolesStub.forEach((role, i) => {
      const userRoleText = screen.getByTestId('workspaceUser-' + i).textContent;
      expect(userRoleText).toContain(role.email);
      expect(userRoleText).toContain(role.role.toString());
    });
  });

  it('should allow the user to open the share modal', async () => {
    componentAlt();
    await waitForNoSpinner();
    await user.click(
      screen.getByRole('button', {
        name: /share/i,
      })
    );
    screen.getByText('Share defaultWorkspace');
  });

  test.each([
    ['enable the share button if workspace is not adminLocked', false],
    ['disable the share button if workspace is adminLocked', true],
  ])('Should %s', async (testName, adminLocked) => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      adminLocked,
    });
    componentAlt();
    await waitForNoSpinner();
    const shareButton = screen.getByRole('button', {
      name: /share/i,
    });

    adminLocked
      ? expectButtonElementDisabled(shareButton)
      : expectButtonElementEnabled(shareButton);
  });

  it('should display cdr version', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="cdrVersion"]').text()).toContain(
      CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION
    );
  });

  it('should display workspace metadata', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="accessTierShortName"]').text()
    ).toContain(fp.capitalize(workspace.accessTierShortName));
    expect(wrapper.find('[data-test-id="creationDate"]').text()).toContain(
      new Date(workspace.creationTime).toDateString()
    );
    expect(wrapper.find('[data-test-id="lastUpdated"]').text()).toContain(
      new Date(workspace.lastModifiedTime).toDateString()
    );
  });

  it('should not manipulate SpecificPopulationItems object on page load', async () => {
    const raceSubCategoriesBeforePageload =
      SpecificPopulationItems[0].subCategory.length;
    component();
    const raceSubCategoriesAfterPageLoad =
      SpecificPopulationItems[0].subCategory.length;
    expect(raceSubCategoriesBeforePageload).toBe(
      raceSubCategoriesAfterPageLoad
    );
  });

  it('should not display Publish/Unpublish buttons without appropriate Authority', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="publish-button"]')).toBeFalsy();
    expect(wrapper.exists('[data-test-id="unpublish-button"]')).toBeFalsy();
  });

  it('should display Publish/Unpublish buttons with FEATUREDWORKSPACEADMIN Authority', async () => {
    const profileWithAuth = {
      ...ProfileStubVariables.PROFILE_STUB,
      authorities: [Authority.FEATURED_WORKSPACE_ADMIN],
    };
    profileStore.set({ profile: profileWithAuth, load, reload, updateCache });

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="publish-button"]')).toBeTruthy();
    expect(wrapper.exists('[data-test-id="unpublish-button"]')).toBeTruthy();
  });

  it('should display Publish/Unpublish buttons with DEVELOPER Authority', async () => {
    const profileWithAuth = {
      ...ProfileStubVariables.PROFILE_STUB,
      authorities: [Authority.DEVELOPER],
    };
    profileStore.set({ profile: profileWithAuth, load, reload, updateCache });

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="publish-button"]')).toBeTruthy();
    expect(wrapper.exists('[data-test-id="unpublish-button"]')).toBeTruthy();
  });

  it('Publish/Unpublish button styling depends on state - unpublished', async () => {
    const profileWithAuth = {
      ...ProfileStubVariables.PROFILE_STUB,
      authorities: [Authority.DEVELOPER],
    };
    profileStore.set({ profile: profileWithAuth, load, reload, updateCache });

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const publishButton = wrapper.find('[data-test-id="publish-button"]');
    expect(publishButton.exists()).toBeTruthy();
    expect(publishButton.prop('disabled')).toBeFalsy();
    expect(publishButton.prop('type')).toEqual('primary');

    const unpublishButton = wrapper.find('[data-test-id="unpublish-button"]');
    expect(unpublishButton.exists()).toBeTruthy();
    expect(unpublishButton.prop('disabled')).toBeTruthy();
    expect(unpublishButton.prop('type')).toEqual('secondary');
  });

  it('Publish/Unpublish button styling depends on state - published', async () => {
    const profileWithAuth = {
      ...ProfileStubVariables.PROFILE_STUB,
      authorities: [Authority.DEVELOPER],
    };
    profileStore.set({ profile: profileWithAuth, load, reload, updateCache });
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      published: true,
    });

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    const publishButton = wrapper.find('[data-test-id="publish-button"]');
    expect(publishButton.exists()).toBeTruthy();
    expect(publishButton.prop('disabled')).toBeTruthy();
    expect(publishButton.prop('type')).toEqual('secondary');

    const unpublishButton = wrapper.find('[data-test-id="unpublish-button"]');
    expect(unpublishButton.exists()).toBeTruthy();
    expect(unpublishButton.prop('disabled')).toBeFalsy();
    expect(unpublishButton.prop('type')).toEqual('primary');
  });

  it('Should display locked workspace message if adminLocked is true', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      adminLocked: true,
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="lock-workspace-msg"]')).toBeTruthy();
  });

  it('Should not display locked workspace message if adminLocked is false', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      adminLocked: false,
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists('[data-test-id="lock-workspace-msg"]')).toBeFalsy();
  });

  it('Should enable billing report url if user is workspace owner.', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper
        .find({ 'data-test-id': 'workspace-billing-report' })
        .first()
        .props().disabled
    ).toBeFalsy();
  });

  it('Should disable billing report url if user is not workspace owner.', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper
        .find({ 'data-test-id': 'workspace-billing-report' })
        .first()
        .props().disabled
    ).toBeTruthy();
  });

  it('should display Google project id', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="googleProject"]').text()).toContain(
      workspace.googleProject
    );
  });
  it('should display bucket name', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="bucketName"]').text()).toContain(
      workspace.googleBucketName
    );
  });
  it('should display workspace namespace', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="workspaceNamespace"]').text()
    ).toContain(workspace.namespace);
  });
});
