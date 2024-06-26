import * as React from 'react';

import {
  FeaturedWorkspacesConfigApi,
  ProfileApi,
  WorkspacesApi,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderWithRouter,
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import { FeaturedWorkspacesConfigApiStub } from 'testing/stubs/featured-workspaces-config-api-stub';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';
import { buildWorkspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { WorkspaceLibrary } from './workspace-library';

describe('WorkspaceLibrary', () => {
  let publishedWorkspaceStubs = [];
  let PHENOTYPE_LIBRARY_WORKSPACES;
  let TUTORIAL_WORKSPACE;
  let user;

  const suffixes = [' Phenotype Library', ' Tutorial Workspace'];

  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
  };

  const component = () => {
    return renderWithRouter(<WorkspaceLibrary {...props} />);
  };

  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });

    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(
      FeaturedWorkspacesConfigApi,
      new FeaturedWorkspacesConfigApiStub()
    );
    publishedWorkspaceStubs = buildWorkspaceStubs(suffixes).map((w) => ({
      ...w,
      published: true,
    }));

    serverConfigStore.set({
      config: defaultServerConfig,
    });

    PHENOTYPE_LIBRARY_WORKSPACES = publishedWorkspaceStubs[0];
    TUTORIAL_WORKSPACE = publishedWorkspaceStubs[1];
    user = userEvent.setup();
  });

  it('renders', async () => {
    component();
    expect(
      await screen.findByText('Researcher Workbench Workspace Library')
    ).toBeInTheDocument();
  });

  it('should display phenotype library workspaces', async () => {
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    component();
    await user.click(await screen.findByText('Phenotype Library'));

    const cardNameList = screen
      .getAllByTestId('workspace-card-name')
      .map((c) => c.textContent);
    expect(cardNameList).toEqual([PHENOTYPE_LIBRARY_WORKSPACES.name]);
  });

  it('should display tutorial workspaces', async () => {
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    component();
    await user.click(
      await screen.findByRole('button', {
        name: 'Tutorial Workspaces',
      })
    );

    const cardNameList = screen
      .getAllByTestId('workspace-card-name')
      .map((c) => c.textContent);
    expect(cardNameList).toEqual([TUTORIAL_WORKSPACE.name]);
  });

  it('should not display unpublished workspaces', async () => {
    component();
    await waitForNoSpinner();
    const cardNameList = screen
      .queryAllByTestId('workspace-card-name')
      .map((c) => c.textContent);
    expect(cardNameList.length).toBe(0);
  });

  it('should have tutorial workspaces as default tab', async () => {
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    component();
    await waitForNoSpinner();
    const cardNameList = screen
      .queryAllByTestId('workspace-card-name')
      .map((c) => c.textContent);
    expect(cardNameList).toEqual([TUTORIAL_WORKSPACE.name]);
  });

  it('controlled tier workspace is not clickable for non-ct user', async () => {
    PHENOTYPE_LIBRARY_WORKSPACES.accessTierShortName =
      AccessTierShortNames.Controlled;
    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );

    component();
    await waitForNoSpinner();

    await user.click(await screen.findByText('Phenotype Library'));

    const cardNameList = screen
      .queryAllByTestId('workspace-card-name')
      .map((c) => c.textContent);
    expect(cardNameList.length).toEqual(1);

    expectButtonElementDisabled(
      screen.getByText(PHENOTYPE_LIBRARY_WORKSPACES.name)
    );
  });

  it('controlled tier workspace is clickable for ct user', async () => {
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profileStore.get().profile,
        accessTierShortNames: [
          AccessTierShortNames.Registered,
          AccessTierShortNames.Controlled,
        ],
      },
    });

    PHENOTYPE_LIBRARY_WORKSPACES.accessTierShortName =
      AccessTierShortNames.Controlled;

    registerApiClient(
      WorkspacesApi,
      new WorkspacesApiStub(publishedWorkspaceStubs)
    );
    component();
    await user.click(await screen.findByText('Phenotype Library'));

    expectButtonElementEnabled(
      screen.getByText(PHENOTYPE_LIBRARY_WORKSPACES.name)
    );
  });
});
