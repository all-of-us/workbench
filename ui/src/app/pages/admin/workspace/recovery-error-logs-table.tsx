import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { Workspace, WorkspaceRecoveryStatus } from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';

interface WorkspaceRecoveryErrorLog {
  id: number;
  workspaceId: number;
  errorMessage: string;
  errorType?: string;
  stackTrace?: string;
  createdTime: string;
}

interface Props {
  workspace: Workspace;
}

export const RecoveryErrorLogsTable = ({ workspace }: Props) => {
  const [logs, setLogs] = useState<WorkspaceRecoveryErrorLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [expandedRows, setExpandedRows] = useState<{ [key: number]: boolean }>(
    {}
  );

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

    (workspaceAdminApi() as any)
      .getWorkspaceRecoveryErrorLogs(workspace.namespace)
      .then((result: any) => {
        setLogs(result || []);
      })
      .catch((error: any) => {
        console.error('Failed to fetch recovery error logs:', error);
        setFetchError('Failed to load recovery error logs');
        setLogs([]);
      })
      .finally(() => setLoading(false));
  }, [workspace.namespace, workspace.recoveryState]);

  // Only render if recovery failed
  if (workspace.recoveryState !== WorkspaceRecoveryStatus.FAILED) {
    return null;
  }

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
          expandedRows={expandedRows}
          onRowToggle={(e) => setExpandedRows(e.data)}
          rowExpansionTemplate={(log: WorkspaceRecoveryErrorLog) => (
            <div style={{ padding: '1rem', backgroundColor: '#f5f5f5' }}>
              <h4>Error Details for {log.errorType}</h4>
              {log.stackTrace && (
                <div>
                  <h5>Stack Trace:</h5>
                  <pre
                    style={{
                      backgroundColor: '#fff',
                      padding: '1rem',
                      borderRadius: '4px',
                      overflow: 'auto',
                      maxHeight: '400px',
                      border: '1px solid #ddd',
                      fontSize: '0.85rem',
                      fontFamily: 'monospace',
                    }}
                  >
                    {log.stackTrace}
                  </pre>
                </div>
              )}
            </div>
          )}
          scrollable
          responsiveLayout='scroll'
          paginator
          rows={10}
          rowsPerPageOptions={[5, 10, 20]}
        >
          <Column expander style={{ width: '3em' }} />
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
            body={(log: WorkspaceRecoveryErrorLog) => {
              return new Date(log.createdTime).toLocaleString();
            }}
          />
        </DataTable>
      )}
    </div>
  );
};
