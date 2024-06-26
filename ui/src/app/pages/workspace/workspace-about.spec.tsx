import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Authority,
  ProfileApi,
  RuntimeApi,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
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
  expectPrimaryButton,
  expectSecondaryButton,
  renderWithRouter,
  waitForNoSpinner,
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
    component();
    await waitForNoSpinner();
    screen.getByText(/primary purpose of project/i);
  });

  it('should display research purpose', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByText('Research Purpose')).toBeInTheDocument();
    // Research Purpose: Drug, Population and Ethics
    expect(screen.getAllByTestId('primaryResearchPurpose').length).toBe(3);
    // Primary Purpose: Education
    expect(screen.getByTestId('primaryPurpose')).toBeInTheDocument();
  });

  it('should display workspace collaborators', async () => {
    component();
    await waitForNoSpinner();
    userRolesStub.forEach((role, i) => {
      const userRoleText = screen.getByTestId('workspaceUser-' + i).textContent;
      expect(userRoleText).toContain(role.email);
      expect(userRoleText).toContain(role.role.toString());
    });
  });

  it('should allow the user to open the share modal', async () => {
    component();
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
    component();
    await waitForNoSpinner();
    const shareButton = screen.getByRole('button', {
      name: /share/i,
    });

    adminLocked
      ? expectButtonElementDisabled(shareButton)
      : expectButtonElementEnabled(shareButton);
  });

  it('should display cdr version', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByTestId('cdrVersion').textContent).toContain(
      CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION
    );
  });

  it('should display workspace metadata', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByTestId('accessTierShortName').textContent).toContain(
      fp.capitalize(workspace.accessTierShortName)
    );
    expect(screen.getByTestId('creationDate').textContent).toContain(
      new Date(workspace.creationTime).toDateString()
    );
    expect(screen.getByTestId('lastUpdated').textContent).toContain(
      new Date(workspace.lastModifiedTime).toDateString()
    );
  });

  it('should not manipulate SpecificPopulationItems object on page load', async () => {
    const raceSubCategoriesBeforePageload =
      SpecificPopulationItems[0].subCategory.length;
    component();
    await waitForNoSpinner();
    const raceSubCategoriesAfterPageLoad =
      SpecificPopulationItems[0].subCategory.length;
    expect(raceSubCategoriesBeforePageload).toBe(
      raceSubCategoriesAfterPageLoad
    );
  });

  it('should not display Publish/Unpublish buttons without appropriate Authority', async () => {
    component();
    await waitForNoSpinner();
    expect(
      screen.queryByRole('button', {
        name: 'Publish',
      })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', {
        name: 'Unpublish',
      })
    ).not.toBeInTheDocument();
  });

  test.each([Authority.FEATURED_WORKSPACE_ADMIN, Authority.DEVELOPER])(
    `should display Publish/Unpublish buttons with %s Authority`,
    async (authority) => {
      const profileWithAuth = {
        ...ProfileStubVariables.PROFILE_STUB,
        authorities: [authority],
      };
      profileStore.set({ profile: profileWithAuth, load, reload, updateCache });

      component();
      await waitForNoSpinner();
      expect(
        screen.getByRole('button', {
          name: 'Publish',
        })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', {
          name: 'Unpublish',
        })
      ).toBeInTheDocument();
    }
  );

  it('Publish/Unpublish button styling depends on state - unpublished', async () => {
    const profileWithAuth = {
      ...ProfileStubVariables.PROFILE_STUB,
      authorities: [Authority.DEVELOPER],
    };
    profileStore.set({ profile: profileWithAuth, load, reload, updateCache });

    component();
    await waitForNoSpinner();

    const publishButton = screen.getByRole('button', {
      name: 'Publish',
    });
    expectButtonElementEnabled(publishButton);
    expectPrimaryButton(publishButton);

    const unpublishButton = screen.getByRole('button', {
      name: 'Unpublish',
    });
    expectButtonElementDisabled(unpublishButton);
    expectSecondaryButton(unpublishButton);
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

    component();
    await waitForNoSpinner();

    const publishButton = screen.getByRole('button', {
      name: 'Publish',
    });
    expectButtonElementDisabled(publishButton);
    expectSecondaryButton(publishButton);

    const unpublishButton = screen.getByRole('button', {
      name: 'Unpublish',
    });
    expectButtonElementEnabled(unpublishButton);
    expectPrimaryButton(unpublishButton);
  });

  it('Should display locked workspace message if adminLocked is true', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      adminLocked: true,
    });
    component();
    await waitForNoSpinner();
    expect(screen.getByTestId('lock-workspace-msg')).toBeInTheDocument();
  });

  it('Should not display locked workspace message if adminLocked is false', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      adminLocked: false,
    });
    component();
    await waitForNoSpinner();
    expect(screen.queryByTestId('lock-workspace-msg')).not.toBeInTheDocument();
  });

  it('Should enable billing report url if user is workspace owner.', async () => {
    component();
    await waitForNoSpinner();
    const billingReportButton = screen.getByText('View detailed spend report');
    expectButtonElementEnabled(billingReportButton);
  });

  it('Should disable billing report url if user is not workspace owner.', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      accessLevel: WorkspaceAccessLevel.WRITER,
    });
    component();
    await waitForNoSpinner();
    const billingReportButton = screen.getByText('View detailed spend report');
    expectButtonElementDisabled(billingReportButton);
  });

  it('should display Google project id', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByTestId('googleProject').textContent).toContain(
      workspace.googleProject
    );
  });

  it('should display bucket name', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByTestId('bucketName').textContent).toContain(
      workspace.googleBucketName
    );
  });
  it('should display workspace namespace', async () => {
    component();
    await waitForNoSpinner();
    expect(screen.getByTestId('workspaceNamespace').textContent).toContain(
      workspace.namespace
    );
  });
});
