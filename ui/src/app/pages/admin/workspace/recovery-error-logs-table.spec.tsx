import * as React from 'react';

import {
  WorkspaceAdminApi,
  WorkspaceRecoveryErrorLog,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { RecoveryErrorLogsTable } from './recovery-error-logs-table';

class WorkspaceAdminApiRecoveryLogsStub extends WorkspaceAdminApi {
  public getWorkspaceRecoveryErrorLogsCalls = 0;

  constructor(
    private readonly fetchLogs: (
      namespace: string
    ) => Promise<WorkspaceRecoveryErrorLog[]>
  ) {
    super(undefined);
  }

  getWorkspaceRecoveryErrorLogs(
    namespace: string
  ): Promise<WorkspaceRecoveryErrorLog[]> {
    this.getWorkspaceRecoveryErrorLogsCalls += 1;
    return this.fetchLogs(namespace);
  }
}

describe('RecoveryErrorLogsTable', () => {
  it('renders columns and values for failed workspace recovery logs', async () => {
    const logs: WorkspaceRecoveryErrorLog[] = [
      {
        id: 7,
        workspaceId: 123,
        errorType: 'RECOVERY_FAILED',
        errorMessage: 'Recover failed after transfer timeout',
        createdTime: '2026-10-16T14:00:00.000Z',
      },
    ];
    const api = new WorkspaceAdminApiRecoveryLogsStub(() =>
      Promise.resolve(logs)
    );
    registerApiClient(WorkspaceAdminApi, api);

    render(
      <RecoveryErrorLogsTable
        namespace='aou-rw-test'
        recoveryState={WorkspaceRecoveryStatus.FAILED}
      />
    );

    expect(screen.getByText('Recovery Error Logs')).toBeInTheDocument();
    await screen.findByText('RECOVERY_FAILED');

    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('Workspace ID')).toBeInTheDocument();
    expect(screen.getByText('Error Type')).toBeInTheDocument();
    expect(screen.getByText('Error Message')).toBeInTheDocument();
    expect(screen.getByText('Time')).toBeInTheDocument();
    expect(
      screen.getByText('Recover failed after transfer timeout')
    ).toBeInTheDocument();
  });

  it('fetches logs only when recovery status is FAILED', async () => {
    const api = new WorkspaceAdminApiRecoveryLogsStub(() =>
      Promise.resolve([])
    );
    registerApiClient(WorkspaceAdminApi, api);

    const { rerender } = render(
      <RecoveryErrorLogsTable
        namespace='aou-rw-test'
        recoveryState={WorkspaceRecoveryStatus.REQUESTED}
      />
    );

    expect(api.getWorkspaceRecoveryErrorLogsCalls).toBe(0);

    rerender(
      <RecoveryErrorLogsTable
        namespace='aou-rw-test'
        recoveryState={WorkspaceRecoveryStatus.FAILED}
      />
    );

    expect(screen.getByText('Recovery Error Logs')).toBeInTheDocument();
  });

  it('shows loading, empty, and error states', async () => {
    let releaseRequest: (value: WorkspaceRecoveryErrorLog[]) => void;
    const pending = new Promise<WorkspaceRecoveryErrorLog[]>((resolve) => {
      releaseRequest = resolve;
    });

    const api = new WorkspaceAdminApiRecoveryLogsStub(() => pending);
    registerApiClient(WorkspaceAdminApi, api);

    const { rerender } = render(
      <RecoveryErrorLogsTable
        namespace='aou-rw-test'
        recoveryState={WorkspaceRecoveryStatus.FAILED}
      />
    );

    expect(
      document.querySelector('.p-datatable-loading-overlay')
    ).toBeInTheDocument();

    releaseRequest([]);
    await screen.findByText('No recovery errors found for this workspace.');

    const failingApi = new WorkspaceAdminApiRecoveryLogsStub(() =>
      Promise.reject(new Error('boom'))
    );
    registerApiClient(WorkspaceAdminApi, failingApi);

    rerender(
      <RecoveryErrorLogsTable
        namespace='aou-rw-test-2'
        recoveryState={WorkspaceRecoveryStatus.FAILED}
      />
    );

    await screen.findByText(
      'Failed to load recovery error logs. Please refresh and try again.'
    );
  });

  it('renders long messages and never displays unexpected stackTrace fields', async () => {
    const longMessage = `${'A very long recovery error message '.repeat(
      8
    )}end.`;
    const log: WorkspaceRecoveryErrorLog = {
      id: 9,
      workspaceId: 77,
      errorType: 'TRANSFER_FAILED',
      errorMessage: longMessage,
      createdTime: '2026-10-16T14:00:00.000Z',
    };
    Object.assign(log, {
      stackTrace: 'sensitive stack trace that should never be shown',
    });
    const logs = [log];

    const api = new WorkspaceAdminApiRecoveryLogsStub(() =>
      Promise.resolve(logs)
    );
    registerApiClient(WorkspaceAdminApi, api);

    render(
      <RecoveryErrorLogsTable
        namespace='aou-rw-test'
        recoveryState={WorkspaceRecoveryStatus.FAILED}
      />
    );

    await screen.findByText(longMessage);
    expect(
      screen.queryByText(/sensitive stack trace that should never be shown/i)
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/stackTrace/i)).not.toBeInTheDocument();
  });
});
