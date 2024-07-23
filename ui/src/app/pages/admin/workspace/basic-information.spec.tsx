import '@testing-library/jest-dom';

import * as React from 'react';

import {
  FeaturedWorkspaceCategory,
  WorkspaceActiveStatus,
  WorkspaceAdminApi,
} from 'generated/fetch';

import { WorkspaceAdminApiStub } from '../../../../testing/stubs/workspace-admin-api-stub';
import { screen } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderWithRouter,
} from 'testing/react-test-helpers';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { BasicInformation } from './basic-information';

describe('BasicInformation', () => {
  const activeStatus = WorkspaceActiveStatus.ACTIVE;
  let user: UserEvent;
  let workspace;
  const component = () => {
    return renderWithRouter(
      <BasicInformation
        {...{ workspace, activeStatus }}
        reload={async () => console.log('X')}
      />
    );
  };

  beforeEach(() => {
    workspace = workspaceStubs[0];
    user = userEvent.setup();
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
      },
    });
    registerApiClient(WorkspaceAdminApi, new WorkspaceAdminApiStub());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders', async () => {
    component();
    expect(
      await screen.findByRole('heading', { name: /basic information/i })
    ).toBeInTheDocument();
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
    component();
    await user.click(await screen.findByText('Community'));
    await user.click(await screen.findByText('Demo Projects'));
    await user.click(screen.getByRole('button', { name: 'Publish' }));
    expect(await screen.findByText('Demo Projects')).toBeInTheDocument();
  });
});
