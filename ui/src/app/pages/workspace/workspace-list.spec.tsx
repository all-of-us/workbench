import * as React from 'react';

import {
  ProfileApi,
  WorkspaceAccessLevel,
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
    const accessLevelOption = await screen.findByText(accessLevelText);

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
});
