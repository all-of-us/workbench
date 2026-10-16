import * as React from 'react';

import {
  Workspace,
  WorkspaceAdminApi,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { renderWithRouter } from 'testing/react-test-helpers';

import { RecoveryErrorLogsTable } from './recovery-error-logs-table';

class WorkspaceAdminApiStub extends WorkspaceAdminApi {
  constructor() {
    super(undefined);
  }

  getWorkspaceRecoveryErrorLogs() {
    return Promise.resolve([
      {
        id: 1,
        workspaceId: 1,
        errorMessage: 'Archive metadata not found',
        errorType: 'ARCHIVE_METADATA_NOT_FOUND',
        createdTime: '2026-10-15T14:56:37Z',
        // Simulate historical records that may still include stackTrace in storage.
        stackTrace: 'java.lang.RuntimeException: old stack',
      } as any,
    ]);
  }
}

describe('RecoveryErrorLogsTable', () => {
  beforeEach(() => {
    registerApiClient(WorkspaceAdminApi, new WorkspaceAdminApiStub());
  });

  it('renders essential recovery error fields and does not render stack trace details', async () => {
    const workspace = {
      namespace: 'aou-rw-local1-f09171af',
      recoveryState: WorkspaceRecoveryStatus.FAILED,
    } as Workspace;

    renderWithRouter(<RecoveryErrorLogsTable workspace={workspace} />);

    await waitFor(() => {
      expect(
        screen.getByText('ARCHIVE_METADATA_NOT_FOUND')
      ).toBeInTheDocument();
    });

    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('Workspace ID')).toBeInTheDocument();
    expect(screen.getByText('Error Message')).toBeInTheDocument();
    expect(screen.getByText('Error Type')).toBeInTheDocument();
    expect(screen.getByText('Time')).toBeInTheDocument();
    expect(screen.getByText('Archive metadata not found')).toBeInTheDocument();

    expect(screen.queryByText('Stack Trace:')).not.toBeInTheDocument();
    expect(
      screen.queryByText(/java.lang.RuntimeException: old stack/i)
    ).not.toBeInTheDocument();
  });
});
