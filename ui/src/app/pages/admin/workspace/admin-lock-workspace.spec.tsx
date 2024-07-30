import '@testing-library/jest-dom';

import * as React from 'react';

import { FeaturedWorkspaceCategory, Workspace } from 'generated/fetch';

import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as swaggerClients from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  renderModal,
} from 'testing/react-test-helpers';
import { buildWorkspaceStub } from 'testing/stubs/workspaces';

import { AdminLockWorkspace } from './admin-lock-workspace';

let workspace, reload, mockWorkspaceAdminApi, setAdminUnlockedState;

beforeEach(() => {
  workspace = buildWorkspaceStub();

  reload = jest.fn();
  setAdminUnlockedState = jest.fn(() => Promise.resolve());

  mockWorkspaceAdminApi = jest.spyOn(swaggerClients, 'workspaceAdminApi');
  mockWorkspaceAdminApi.mockImplementation(() => ({ setAdminUnlockedState }));
});

afterEach(() => {
  jest.clearAllMocks();
});

const renderAdminLockWorkspaceModal = (ws: Workspace) =>
  renderModal(<AdminLockWorkspace reload={reload} workspace={ws} />);

describe('AdminLockWorkspace', () => {
  it('opens a modal for locking an unlocked workspace', async () => {
    const user = userEvent.setup();
    const { container } = renderAdminLockWorkspaceModal({
      ...workspace,
      adminLocked: false,
    });

    expect(container).toBeInTheDocument();

    const lockButton = screen.getByText('LOCK WORKSPACE');
    expect(lockButton).toBeInTheDocument();

    expect(screen.queryByText('UNLOCK WORKSPACE')).not.toBeInTheDocument();

    await user.click(lockButton);

    await waitFor(() => {
      // lock reason is inside modal
      const lockReasonTextArea = screen.getByLabelText(
        'Enter reason for researchers on why workspace access is locked'
      );
      expect(lockReasonTextArea).toBeInTheDocument();
    });
  });

  it('unlocks a locked workspace', async () => {
    const user = userEvent.setup();
    const { container } = renderAdminLockWorkspaceModal({
      ...workspace,
      adminLocked: true,
    });
    expect(container).toBeInTheDocument();

    const unlockButton = screen.getByText('UNLOCK WORKSPACE');
    expect(unlockButton).toBeInTheDocument();

    expect(screen.queryByText('LOCK WORKSPACE')).not.toBeInTheDocument();

    await act(async () => {
      await user.click(unlockButton);

      await waitFor(() => {
        expect(setAdminUnlockedState).toHaveBeenCalledTimes(1);
      });
      await waitFor(() => {
        expect(reload).toHaveBeenCalledTimes(1);
      });
    });
  });

  it('should disable lock button if its a published workspace', async () => {
    const user = userEvent.setup();
    const { container } = renderAdminLockWorkspaceModal({
      ...workspace,
      featuredCategory: FeaturedWorkspaceCategory.COMMUNITY,
    });
    expect(container).toBeInTheDocument();
    expect(screen.queryByText('UNLOCK WORKSPACE')).not.toBeInTheDocument();
    const lockButton = screen.queryByRole('button', {
      name: /lock workspace/i,
    });
    expect(lockButton).toBeInTheDocument();
    expectButtonElementDisabled(lockButton);
    await user.hover(lockButton);
    expect(
      screen.getByText('Cannot lock published workspace')
    ).toBeInTheDocument();
  });
});
