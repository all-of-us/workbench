import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  Workspace,
  WorkspaceRecoveryErrorLog,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';

interface Props {
  workspace: Workspace;
}

export const RecoveryErrorLogsTable = ({ workspace }: Props) => {
  const [logs, setLogs] = useState<WorkspaceRecoveryErrorLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState<string | null>(null);

  useEffect(() => {
    // Only fetch logs if recovery status is FAILED
    if (workspace.recoveryState !== WorkspaceRecoveryStatus.FAILED) {
      setLogs([]);
      setFetchError(null);
      return;
    }

    setLoading(true);
    setFetchError(null);
    setLogs([]);

    workspaceAdminApi()
      .getWorkspaceRecoveryErrorLogs(workspace.namespace)
      .then((result) => {
        setLogs(result || []);
      })
      .catch((error) => {
        console.error('Failed to fetch recovery error logs:', error);
        setFetchError('Failed to load recovery error logs');
        setLogs([]);
      })
      .finally(() => setLoading(false));
  }, [workspace.namespace, workspace.recoveryState]);

  return (
    <div style={{ marginTop: '2rem' }}>
      <h2>Recovery Error Logs</h2>

      {loading && <Spinner />}

      {fetchError && (
        <div
          style={{
            color: '#d32f2f',
            padding: '1rem',
            backgroundColor: '#ffebee',
            borderRadius: '4px',
            border: '1px solid #ef5350',
            marginBottom: '1rem',
          }}
        >
          {fetchError}
        </div>
      )}

      {!loading && !fetchError && logs.length === 0 && (
        <div style={{ padding: '1rem', color: '#666' }}>
          No error logs available
        </div>
      )}

      {!loading && !fetchError && logs.length > 0 && (
        <DataTable
          value={logs}
          scrollable
          paginator
          rows={10}
          rowsPerPageOptions={[5, 10, 20]}
        >
          <Column field='id' header='ID' style={{ minWidth: '90px' }} />
          <Column
            field='workspaceId'
            header='Workspace ID'
            style={{ minWidth: '140px' }}
          />
          <Column
            field='errorType'
            header='Error Type'
            style={{
              color: '#d32f2f',
              fontWeight: 600,
              minWidth: '150px',
            }}
          />
          <Column
            field='errorMessage'
            header='Error Message'
            style={{ minWidth: '400px' }}
            body={(log: WorkspaceRecoveryErrorLog) => (
              <div
                style={{
                  whiteSpace: 'normal',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  maxWidth: '500px',
                }}
              >
                {log.errorMessage}
              </div>
            )}
          />
          <Column
            field='createdTime'
            header='Time'
            style={{ minWidth: '200px' }}
            body={(log: WorkspaceRecoveryErrorLog) =>
              log.createdTime
                ? new Date(log.createdTime).toLocaleString()
                : 'N/A'
            }
          />
        </DataTable>
      )}
    </div>
  );
};
