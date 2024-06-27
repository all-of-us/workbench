import '@testing-library/jest-dom';

import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { WorkspaceActiveStatus } from '../../../../generated/fetch';
import { render, screen } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { AdminWorkspaceSearch } from './admin-workspace-search';
import { BasicInformation } from './basic-information';

describe('BasicInformation', () => {
  const activeStatus = WorkspaceActiveStatus.ACTIVE;
  let user: UserEvent;
  let workspace;
  const component = () => {
    return render(<BasicInformation {...{ workspace, activeStatus }} />);
  };

  beforeEach(() => {
    workspace = workspaceStubs[0];
    user = userEvent.setup();
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
    expect(
      screen.getByPlaceholderText('Select a category...')
    ).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /publish/i })
    );
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });
  it('should show published workspace', async () => {
    workspace.category = 'Community';
    component();
    expect(await screen.findByText('Select a category...')).toBeInTheDocument();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /change category/i })
    );
    expectButtonElementEnabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });

  it('should publish workspace', async () => {
    component();
    await user.click(await screen.findByText('Select a category...'));
    await user.click(await screen.findByText('Community'));
    await user.click(screen.getByRole('button', { name: /publish/i }));
    await waitForNoSpinner();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /change category/i })
    );
    expectButtonElementEnabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });

  it('should unpublish workspace', async () => {
    component();
    await user.click(screen.getByRole('button', { name: /unpublish/i }));
    await waitForNoSpinner();
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /publish/i })
    );
    expectButtonElementDisabled(
      screen.getByRole('button', { name: /unpublish/i })
    );
  });
});
