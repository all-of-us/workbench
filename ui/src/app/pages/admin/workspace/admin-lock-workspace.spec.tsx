import '@testing-library/jest-dom';

import * as React from 'react';

import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as swaggerClients from 'app/services/swagger-fetch-clients';

import { renderModal } from 'testing/react-test-helpers';
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

describe('AdminLockWorkspace', () => {
  it('opens a modal for locking an unlocked workspace', async () => {
    const user = userEvent.setup();
    const { container } = renderModal(
      <AdminLockWorkspace
        {...{ reload }}
        workspace={{ ...workspace, adminLocked: false }}
      />
    );
    expect(container).toBeInTheDocument();

    const lockButton = screen.getByText('LOCK WORKSPACE');
    expect(lockButton).toBeInTheDocument();

    expect(screen.queryByText('UNLOCK WORKSPACE')).not.toBeInTheDocument();

    await act(async () => {
      await user.click(lockButton);

      await waitFor(() => {
        // lock reason is inside modal
        const lockReasonTextArea = screen.getByLabelText(
          'Enter reason for researchers on why workspace access is locked'
        );
        expect(lockReasonTextArea).toBeInTheDocument();
      });
    });
  });

  it('unlocks a locked workspace', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AdminLockWorkspace
        {...{ reload }}
        workspace={{ ...workspace, adminLocked: true }}
      />
    );
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
});
