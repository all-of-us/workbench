import * as React from 'react';

import {
  MigrationState,
  ProfileApi,
  WorkspaceAccessLevel,
  WorkspaceRecoveryStatus,
  WorkspacesApi,
} from 'generated/fetch';

import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import { renderWithRouter, waitForNoSpinner } from 'testing/react-test-helpers';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import {
  buildWorkspaceStub,
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceList } from './workspace-list';

describe('WorkspaceList', () => {
  const profile = ProfileStubVariables.PROFILE_STUB;
  let profileApi: ProfileApiStub;
  let user;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let workspacesApiStub: WorkspacesApiStub;

  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
  };

  const component = () => {
    return renderWithRouter(<WorkspaceList {...props} />);
  };

  async function pickAccessLevel(
    dropdown: HTMLElement,
    accessLevelText: string
  ) {
    await user.click(dropdown);

    const listbox = await screen.findByRole('listbox');

    const accessLevelOption = within(listbox).getByText(accessLevelText);

    await user.click(accessLevelOption);
  }

  function getCardNames() {
    return screen
      .getAllByTestId('workspace-card-name')
      .map((c) => c.textContent);
  }

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });

    profileStore.set({ profile, load, reload, updateCache });
    serverConfigStore.set({
      config: { gsuiteDomain: 'abc' },
    });

    user = userEvent.setup();
  });

  it('displays the correct number of workspaces', async () => {
    component();
    await waitForNoSpinner();
    expect(getCardNames()).toEqual(workspaceStubs.map((w) => w.name));
  });

  it('has the correct permissions classes', async () => {
    component();
    await waitForNoSpinner();

    const firstWorkspace = screen.getAllByTestId('workspace-card')[0];
    const accessLevel = within(firstWorkspace).getByTestId(
      'workspace-access-level'
    );

    expect(accessLevel.textContent).toBe(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION
    );
  });

  it('filters workspace list', async () => {
    const workspaceRead = buildWorkspaceStub('read');
    const workspaceWrite = buildWorkspaceStub('write');
    const workspaceOwn = buildWorkspaceStub('own');
    workspacesApiStub.workspaces = [
      workspaceRead,
      workspaceWrite,
      workspaceOwn,
    ];
    workspacesApiStub.workspaceAccess = new Map([
      [workspaceRead.terraName, WorkspaceAccessLevel.READER],
      [workspaceWrite.terraName, WorkspaceAccessLevel.WRITER],
      [workspaceOwn.terraName, WorkspaceAccessLevel.OWNER],
    ]);

    component();
    await waitForNoSpinner();
    expect(getCardNames().length).toEqual(3);

    const accessLevelDropdown = screen.getByLabelText(
      'Access level filter selector'
    );

    await pickAccessLevel(accessLevelDropdown, 'Reader');
    expect(getCardNames()).toEqual([workspaceRead.name]);

    await pickAccessLevel(accessLevelDropdown, 'Owner');
    expect(getCardNames()).toEqual([workspaceOwn.name]);

    await pickAccessLevel(accessLevelDropdown, 'Writer');
    expect(getCardNames()).toEqual([workspaceWrite.name]);

    await pickAccessLevel(accessLevelDropdown, 'All');
    expect(getCardNames().length).toEqual(3);
  });

  it('shows legacy section and delete action for migrated owners', async () => {
    const migratedWorkspace = {
      ...buildWorkspaceStub('migrated-owner'),
      migrationState: MigrationState.FINISHED,
      creatorUser: { userName: 'someone-else@fake-research-aou.org' },
    };
    workspacesApiStub.workspaces = [migratedWorkspace];
    workspacesApiStub.workspaceAccess = new Map([
      [migratedWorkspace.terraName, WorkspaceAccessLevel.OWNER],
    ]);

    component();
    await waitForNoSpinner();

    expect(screen.getByText('Legacy Workspaces')).toBeInTheDocument();
    expect(screen.getByTestId('delete-migrated-workspace')).toBeInTheDocument();
  });

  it('filters legacy list by migrated and archival flow status', async () => {
    const migratedWorkspace = {
      ...buildWorkspaceStub('migrated'),
      migrationState: MigrationState.FINISHED,
    };
    const archivalWorkspace = {
      ...buildWorkspaceStub('archival'),
      recoveryState: WorkspaceRecoveryStatus.NOT_STARTED,
    };

    workspacesApiStub.workspaces = [migratedWorkspace, archivalWorkspace];
    workspacesApiStub.workspaceAccess = new Map([
      [migratedWorkspace.terraName, WorkspaceAccessLevel.OWNER],
      [archivalWorkspace.terraName, WorkspaceAccessLevel.OWNER],
    ]);

    component();
    await waitForNoSpinner();

    expect(screen.getAllByTestId('workspace-card-name')).toHaveLength(2);

    await user.click(screen.getByRole('button', { name: 'Migrated' }));
    expect(screen.getAllByTestId('workspace-card-name')).toHaveLength(1);
    expect(screen.getByText(migratedWorkspace.name)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Archival Flow' }));
    expect(screen.getAllByTestId('workspace-card-name')).toHaveLength(1);
    expect(screen.getByText(archivalWorkspace.name)).toBeInTheDocument();
  });

  it('shows migrated delete action for creators and hides it for other non-owners', async () => {
    const creatorMigratedWorkspace = {
      ...buildWorkspaceStub('migrated-creator'),
      migrationState: MigrationState.FINISHED,
      creatorUser: { userName: profile.username },
    };
    const nonCreatorMigratedWorkspace = {
      ...buildWorkspaceStub('migrated-reader'),
      migrationState: MigrationState.FINISHED,
      creatorUser: { userName: 'another-user@fake-research-aou.org' },
    };
    workspacesApiStub.workspaces = [
      creatorMigratedWorkspace,
      nonCreatorMigratedWorkspace,
    ];
    workspacesApiStub.workspaceAccess = new Map([
      [creatorMigratedWorkspace.terraName, WorkspaceAccessLevel.READER],
      [nonCreatorMigratedWorkspace.terraName, WorkspaceAccessLevel.READER],
    ]);

    component();
    await waitForNoSpinner();

    // Exactly one card is deletable: reader + creator.
    expect(screen.getAllByTestId('delete-migrated-workspace')).toHaveLength(1);
  });
  it('shows delete action for recovered workspaces', async () => {
    const recoveredWorkspace = {
      ...buildWorkspaceStub('recovered'),
      recoveryState: WorkspaceRecoveryStatus.RECOVERED,
      creatorUser: { userName: profile.username },
    };
    workspacesApiStub.workspaces = [recoveredWorkspace];
    workspacesApiStub.workspaceAccess = new Map([
      [recoveredWorkspace.terraName, WorkspaceAccessLevel.OWNER],
    ]);

    component();
    await waitForNoSpinner();

    expect(screen.getByText('Legacy Workspaces')).toBeInTheDocument();
    expect(screen.getByTestId('delete-migrated-workspace')).toBeInTheDocument();
  });
});
