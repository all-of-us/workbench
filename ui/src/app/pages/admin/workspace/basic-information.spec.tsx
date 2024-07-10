import '@testing-library/jest-dom';

import * as React from 'react';

import { WorkspaceActiveStatus } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderWithRouter,
} from 'testing/react-test-helpers';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { AdminWorkspaceSearch } from './admin-workspace-search';
import { BasicInformation } from './basic-information';

describe('BasicInformation', () => {
  const activeStatus = WorkspaceActiveStatus.ACTIVE;
  let user: UserEvent;
  let workspace;
  const component = () => {
    return renderWithRouter(
      <BasicInformation {...{ workspace, activeStatus }} />
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
    workspace.featuredCategory = 'Community';
    component();
    expect(await screen.findByText('Select a category...')).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /change category/i })
    );
    expectButtonElementEnabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });
  it('should change category of published workspace', async () => {
    workspace.featuredCategory = 'Community';
    component();
    await user.click(await screen.findByText('Select a category...'));
    await user.click(await screen.findByText('Demo Projects'));
    expectButtonElementEnabled(
      screen.getByRole('button', { name: /change category/i })
    );
    expectButtonElementEnabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });
});
