import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  FeaturedWorkspaceCategory,
  ProfileApi,
  RuntimeApi,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { nowPlusDays } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { SpecificPopulationItems } from 'app/utils/research-purpose-text';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectTooltip,
  expectTooltipAbsence,
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
import {
  userRolesStub,
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceAbout } from './workspace-about';

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
        enableInitialCreditsExpiration: true,
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
    screen.getByText('Share ' + WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME);
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

  it('renders Community Workspace section', () => {
    component();
    expect(screen.getByText('Community Workspace')).toBeInTheDocument();
  });

  it('Publish button is enabled for workspace owner when the workspace is not published previously', () => {
    component();
    const publishButton = screen.getByText('Publish');
    expectButtonElementEnabled(publishButton);
  });

  it('should disable publish button for non workspace owner', async () => {
    const nonOwnerWorkspace = {
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.WRITER,
    };
    currentWorkspaceStore.next(nonOwnerWorkspace);
    component();
    const publishButton = screen.getByText('Publish');
    expectButtonElementDisabled(publishButton);

    await user.hover(publishButton);
    screen.getByText('Only workspace owners can publish workspaces');
  });

  it('should disable publish button for locked workspace', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      adminLocked: true,
    });
    component();
    const publishButton = screen.getByText('Publish');
    expectButtonElementDisabled(publishButton);

    await user.hover(publishButton);
    screen.getByText('Locked workspace cannot be published');
  });

  it('should disable publish button for workspace owner if workspace is already published', async () => {
    const publishedWorkspace = {
      ...workspaceStubs[0],
      featuredCategory: FeaturedWorkspaceCategory.COMMUNITY,
      accessLevel: WorkspaceAccessLevel.OWNER,
    };
    currentWorkspaceStore.next(publishedWorkspace);
    component();
    const publishButton = screen.getByText('Publish');
    expectButtonElementDisabled(publishButton);

    await user.hover(publishButton);
    screen.getByText('Contact Support to unpublish');
  });

  it('should set showPublishConsentModal to true if publish button is clicked ', async () => {
    component();
    await user.click(screen.getByText('Publish'));
    expect(
      screen.getByText('Publish As a Community Workspace')
    ).toBeInTheDocument();
  });

  it('should see initial credit expiration date', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      billingAccountName: 'billingAccounts/free',
      initialCredits: {
        exhausted: false,
        expired: false,
        expirationEpochMillis: new Date('1998-03-17T15:30:00').getTime(),
      },
    });
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        freeTierBillingAccountId: 'free',
      },
    });
    component();
    expect(
      screen.getByText(/workspace initial credit expiration/i)
    ).toBeInTheDocument();
    screen.getByText(/tue mar 17 1998/i);
  });

  it('should not see initial credit expiration date when enableInitialCreditsExpiration is false', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      billingAccountName: 'billingAccounts/free',
      initialCredits: {
        exhausted: false,
        expired: false,
        expirationEpochMillis: new Date('1998-03-17T15:30:00').getTime(),
      },
    });
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        freeTierBillingAccountId: 'free',
        enableInitialCreditsExpiration: false,
      },
    });
    component();
    expect(
      screen.queryByText(/workspace initial credit expiration/i)
    ).not.toBeInTheDocument();
  });

  it('eligble creator should be able to request extension', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      creatorUser: {
        userName: profile.username,
        givenName: profile.givenName,
        familyName: profile.familyName,
      },
      billingAccountName: 'billingAccounts/free',
      initialCredits: {
        exhausted: false,
        expired: true,
        expirationEpochMillis: nowPlusDays(-1),
      },
    });
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        freeTierBillingAccountId: 'free',
      },
    });
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profile,
        eligibleForInitialCreditsExtension: true,
      },
    });
    component();
    const requestExtensionButton = screen.getByRole('button', {
      name: /request extension/i,
    });
    expect(requestExtensionButton).toBeInTheDocument();

    await expectTooltipAbsence(
      requestExtensionButton,
      'Contact your workspace creator, ' +
        `${workspace?.creatorUser?.givenName} ${workspace?.creatorUser?.familyName}, to extend initial credits.`,
      user
    );
    await user.click(requestExtensionButton);
    expect(
      await screen.findByText(/request credit expiration date extension/i)
    ).toBeInTheDocument();
  });

  it('ineligble creator should not be able to request extension', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      creatorUser: {
        userName: profile.username,
        givenName: profile.givenName,
        familyName: profile.familyName,
      },
      billingAccountName: 'billingAccounts/free',
      initialCredits: {
        exhausted: false,
        expired: true,
        expirationEpochMillis: nowPlusDays(-1),
      },
    });
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        freeTierBillingAccountId: 'free',
      },
    });
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profile,
        eligibleForInitialCreditsExtension: false,
      },
    });
    component();
    const requestExtensionButton = screen.getByRole('button', {
      name: /request extension/i,
    });
    expect(requestExtensionButton).toBeInTheDocument();

    await expectTooltip(
      requestExtensionButton,
      'You are not eligible to extend your initial credits.',
      user
    );
    expectButtonElementDisabled(requestExtensionButton);
  });

  it('non-creator should not be able to request extension', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      creatorUser: {
        userName: 'not-the-user@aou-rwb.com',
        givenName: 'Bob',
        familyName: 'Pop',
      },
      billingAccountName: 'billingAccounts/free',
      initialCredits: {
        exhausted: false,
        expired: true,
        expirationEpochMillis: nowPlusDays(-1),
      },
    });
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        freeTierBillingAccountId: 'free',
      },
    });
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profile,
        eligibleForInitialCreditsExtension: true,
      },
    });
    component();
    const requestExtensionButton = screen.getByRole('button', {
      name: /request extension/i,
    });
    expect(requestExtensionButton).toBeInTheDocument();

    const currentWorkspace = currentWorkspaceStore.getValue();

    await expectTooltip(
      requestExtensionButton,
      'Contact your workspace creator, ' +
        `${currentWorkspace?.creatorUser?.givenName} ${currentWorkspace?.creatorUser?.familyName}, ` +
        "to update this workspace's billing details.",
      user
    );
    expectButtonElementDisabled(requestExtensionButton);
  });

  it('should not see initial credit expiration section when bypassed', async () => {
    currentWorkspaceStore.next({
      ...currentWorkspaceStore.getValue(),
      creatorUser: {
        userName: 'not-the-user@aou-rwb.com',
        givenName: 'Bob',
        familyName: 'Pop',
      },
      billingAccountName: 'billingAccounts/free',
      initialCredits: {
        exhausted: false,
        expired: true,
        expirationEpochMillis: nowPlusDays(-1),
        expirationBypassed: true,
      },
    });
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        freeTierBillingAccountId: 'free',
      },
    });
    component();
    expect(
      screen.queryByText(/workspace initial credit expiration/i)
    ).not.toBeInTheDocument();
  });
});
