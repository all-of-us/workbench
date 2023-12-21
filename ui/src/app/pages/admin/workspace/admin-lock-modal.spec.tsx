import '@testing-library/jest-dom';

import * as React from 'react';

import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as swaggerClients from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

import { AdminLockModal } from './admin-lock-modal';

let workspaceNamespace,
  onLock,
  onCancel,
  mockWorkspaceAdminApi,
  setAdminLockedState;

beforeEach(() => {
  workspaceNamespace = WorkspaceStubVariables.DEFAULT_WORKSPACE_NS;

  onLock = jest.fn();
  onCancel = jest.fn();
  setAdminLockedState = jest.fn(() => Promise.resolve());

  mockWorkspaceAdminApi = jest.spyOn(swaggerClients, 'workspaceAdminApi');
  mockWorkspaceAdminApi.mockImplementation(() => ({ setAdminLockedState }));
});

afterEach(() => {
  jest.clearAllMocks();
});

describe('AdminLockModal', () => {
  it('disallows too-short and too-long text descriptions', async () => {
    const user = userEvent.setup();
    const { container } = renderModal(
      <AdminLockModal {...{ workspaceNamespace, onLock, onCancel }} />
    );
    expect(container).toBeInTheDocument();

    const cancelButton = screen.getByText('CANCEL');
    expectButtonElementEnabled(cancelButton);

    const lockButton = screen.getByText('LOCK WORKSPACE');
    expectButtonElementDisabled(lockButton);

    const lockReasonTextArea = screen.getByLabelText(
      'Enter reason for researchers on why workspace access is locked'
    );
    expect(lockReasonTextArea).toBeInTheDocument();

    await user.type(lockReasonTextArea, 'too short');
    expectButtonElementDisabled(lockButton);

    await user.type(lockReasonTextArea, 'more than ten letters');
    expectButtonElementEnabled(lockButton);

    const tooMuchText = 'x'.repeat(5000);
    await user.click(lockReasonTextArea);
    await user.paste(tooMuchText);
    expectButtonElementDisabled(lockButton);
  });

  it('locks a workspace', async () => {
    const requestReason = 'a very good reason for locking this workspace';
    const requestDate = '2023-08-23';
    const requestDateInMillis = new Date(requestDate).valueOf();
    const user = userEvent.setup();
    const { container } = renderModal(
      <AdminLockModal {...{ workspaceNamespace, onLock, onCancel }} />
    );
    expect(container).toBeInTheDocument();

    const lockButton = screen.getByText('LOCK WORKSPACE');
    expectButtonElementDisabled(lockButton);

    const lockReasonTextArea = screen.getByLabelText(
      'Enter reason for researchers on why workspace access is locked'
    );
    expect(lockReasonTextArea).toBeInTheDocument();

    await user.type(lockReasonTextArea, requestReason);

    const requestDatePicker = screen.getByPlaceholderText('YYYY-MM-DD');
    // selects existing text
    await user.tripleClick(requestDatePicker);
    // overwrites existing text.  typing won't work here because it is aggressively parsed.
    await user.paste(requestDate);

    expectButtonElementEnabled(lockButton);

    await act(async () => {
      await user.click(lockButton);
      await waitFor(() => {
        expect(setAdminLockedState).toHaveBeenCalledWith(workspaceNamespace, {
          requestReason,
          requestDateInMillis,
        });
      });
    });
  });
});
