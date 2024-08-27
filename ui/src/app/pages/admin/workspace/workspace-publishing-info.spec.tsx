import '@testing-library/jest-dom';

import * as React from 'react';

import { FeaturedWorkspaceCategory, WorkspaceAdminApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import {
  registerApiClient,
  workspaceAdminApi,
} from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  expectTooltip,
  renderWithRouter,
} from 'testing/react-test-helpers';
import { WorkspaceAdminApiStub } from 'testing/stubs/workspace-admin-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { WorkspacePublishingInfo } from './workspace-publishing-info';

describe(WorkspacePublishingInfo.name, () => {
  let user: UserEvent;
  let workspace;
  const component = () => {
    return renderWithRouter(
      <WorkspacePublishingInfo
        {...{ workspace }}
        reload={async () => Promise.resolve()}
      />
    );
  };

  beforeEach(() => {
    workspace = JSON.parse(JSON.stringify(workspaceStubs[0]));
    user = userEvent.setup();
    serverConfigStore.set({
      config: defaultServerConfig,
    });
    registerApiClient(WorkspaceAdminApi, new WorkspaceAdminApiStub());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders', async () => {
    component();
    expect(await screen.findByText(/Workspace Published/)).toBeInTheDocument();
  });

  it('should show unpublished workspace', async () => {
    component();

    expect(screen.getByText('Select a category...')).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: 'Publish' })
    );
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });

  it('should show published workspace', async () => {
    workspace.featuredCategory = FeaturedWorkspaceCategory.COMMUNITY;
    component();
    expect(await screen.findByText('Community')).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: 'Publish' })
    );
    expectButtonElementEnabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });

  it('should change category of published workspace', async () => {
    workspace.featuredCategory = FeaturedWorkspaceCategory.COMMUNITY;
    jest
      .spyOn(workspaceAdminApi(), 'publishWorkspaceViaDB')
      .mockImplementation((): Promise<any> => Promise.resolve());
    component();
    await user.click(await screen.findByText('Community'));
    await user.click(await screen.findByText('Demo Projects'));
    await user.click(screen.getByRole('button', { name: 'Publish' }));
    expect(await screen.findByText('Demo Projects')).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: 'Publish' })
    );
  });

  it('should unpublish a workspace', async () => {
    workspace.featuredCategory = FeaturedWorkspaceCategory.COMMUNITY;
    jest
      .spyOn(workspaceAdminApi(), 'unpublishWorkspaceViaDB')
      .mockImplementation((): Promise<any> => Promise.resolve());
    component();
    await user.click(await screen.findByRole('button', { name: /unpublish/i }));
    expect(screen.getByText('Select a category...')).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });

  it('should disable publishing when workspace is locked', async () => {
    workspace.featuredCategory = undefined;
    workspace.adminLocked = true;
    component();
    const publishButton = await screen.findByRole('button', {
      name: 'Publish',
    });
    expectButtonElementDisabled(publishButton);
    await expectTooltip(
      publishButton,
      'This workspace is locked and cannot be published.',
      user
    );
  });

  it('should show appropriate tooltip when workspace is not published', async () => {
    workspace.featuredCategory = null;
    component();
    const publishButton = await screen.findByRole('button', {
      name: 'Publish',
    });

    await expectTooltip(
      publishButton,
      'Please select a category to publish the workspace.',
      user
    );
  });
});
