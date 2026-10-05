import * as React from 'react';
import { useState, useEffect } from 'react';

import {
  MigrationState,
  Workspace,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';

import { WorkspaceInfoField } from './workspace-info-field';

interface WorkspaceRecoveryErrorLog {
  id: number;
  workspaceId: number;
  errorMessage: string;
  errorType?: string;
  stackTrace?: string;
  createdTime: string;
}

interface Props {
  loadingCollaborators: boolean;
  workspace: Workspace;
  onRecover?: () => void;
}

export const WorkspaceArchiveInfo = ({
  loadingCollaborators,
  workspace,
  onRecover,
}: Props) => {
  const [recoveryErrorLogs, setRecoveryErrorLogs] =
    useState<WorkspaceRecoveryErrorLog[]>([]);
  const [loadingErrorLogs, setLoadingErrorLogs] = useState<boolean>(false);
  const [expandedErrorLogId, setExpandedErrorLogId] = useState<number | null>(null);

  useEffect(() => {
    // Fetch recovery error logs when recovery status is FAILED
    if (workspace.recoveryState === WorkspaceRecoveryStatus.FAILED) {
      setLoadingErrorLogs(true);
      (workspaceAdminApi() as any)
        .getWorkspaceRecoveryErrorLogs(workspace.namespace)
        .then((logs: any) => {
          setRecoveryErrorLogs(logs || []);
        })
        .catch((error: any) => {
          console.error('Failed to fetch recovery error logs:', error);
          setRecoveryErrorLogs([]);
        })
        .finally(() => setLoadingErrorLogs(false));
    }
  }, [workspace.namespace, workspace.recoveryState]);

  const migrated = workspace.migrationState === MigrationState.FINISHED;

  const archiveStatus =
    typeof workspace.recoveryState === 'undefined'
      ? 'Not Archived'
      : 'Archived';

  const recoveryStatus = (() => {
    switch (workspace.recoveryState) {
      case WorkspaceRecoveryStatus.NOT_STARTED:
        return 'Not Requested';

      case WorkspaceRecoveryStatus.REQUESTED:
        return 'Requested by Researcher';

      case WorkspaceRecoveryStatus.RECOVERING:
        return 'Recovery In Progress';

      case WorkspaceRecoveryStatus.RECOVERED:
        return 'Recovery Completed';

      case WorkspaceRecoveryStatus.FAILED:
        return 'Recovery Failed';

      default:
        return 'N/A';
    }
  })();

  return (
    <>
      <h3>Workspace Archive</h3>
      <div className='basic-info' style={{ marginTop: '1.5rem' }}>
        {migrated ? (
          <WorkspaceInfoField labelText='Archive'>
            No archival record – Workspace migrated to Verily
          </WorkspaceInfoField>
        ) : (
          <>
            <WorkspaceInfoField labelText='Archive Status'>
              {archiveStatus}
            </WorkspaceInfoField>

            <WorkspaceInfoField labelText='Recovery Status'>
              {recoveryStatus}
            </WorkspaceInfoField>

            <WorkspaceInfoField labelText='Recovery Action'>
              <Button
                type='primary'
                disabled={
                  loadingCollaborators ||
                  (workspace.recoveryState !==
                    WorkspaceRecoveryStatus.REQUESTED &&
                    workspace.recoveryState !== WorkspaceRecoveryStatus.FAILED)
                }
                onClick={onRecover}
                style={{
                  minWidth: '220px',
                  textTransform: 'uppercase',
                }}
              >
                {(() => {
                  switch (workspace.recoveryState) {
                    case WorkspaceRecoveryStatus.NOT_STARTED:
                      return 'Waiting for Researcher Request';

                    case WorkspaceRecoveryStatus.REQUESTED:
                      return 'Recover Workspace';

                    case WorkspaceRecoveryStatus.RECOVERING:
                      return 'Recovery In Progress';

                    case WorkspaceRecoveryStatus.RECOVERED:
                      return 'Recovery Complete';

                    case WorkspaceRecoveryStatus.FAILED:
                      return 'Retry Recovery';

                    default:
                      return 'Not Archived';
                  }
                })()}
              </Button>
            </WorkspaceInfoField>

            <WorkspaceInfoField labelText='Recovered VWB Workspace ID'>
              {workspace.migratedVwbWorkspaceId || 'N/A'}
            </WorkspaceInfoField>

            {/* Recovery Error Logs Section */}
            {workspace.recoveryState === WorkspaceRecoveryStatus.FAILED &&
              (loadingErrorLogs ? (
                <WorkspaceInfoField labelText='Recovery Error Logs'>
                  <Spinner />
                </WorkspaceInfoField>
              ) : recoveryErrorLogs.length > 0 ? (
                <>
                  <WorkspaceInfoField labelText='Recovery Error Logs'>
                    <div style={{ marginTop: '0.5rem' }}>
                      {recoveryErrorLogs.map((errorLog, index) => (
                        <div
                          key={errorLog.id}
                          style={{
                            border: '1px solid #ddd',
                            borderRadius: '4px',
                            padding: '0.75rem',
                            marginBottom: '0.5rem',
                            backgroundColor: '#fafafa',
                          }}
                        >
                          <div
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'flex-start',
                            }}
                          >
                            <div style={{ flex: 1 }}>
                              <div
                                style={{
                                  fontWeight: 600,
                                  marginBottom: '0.25rem',
                                  color: '#d32f2f',
                                }}
                              >
                                Error {index + 1}:{' '}
                                {errorLog.errorType || 'UNKNOWN'}
                              </div>
                              <div style={{ marginBottom: '0.5rem' }}>
                                <strong>Message:</strong>{' '}
                                {errorLog.errorMessage}
                              </div>
                              <div style={{ fontSize: '0.85rem', color: '#666' }}>
                                <strong>Time:</strong>{' '}
                                {new Date(
                                  errorLog.createdTime
                                ).toLocaleString()}
                              </div>
                            </div>
                            {errorLog.stackTrace && (
                              <Button
                                type='secondary'
                                style={{
                                  height: '1.875rem',
                                  marginLeft: '0.5rem',
                                }}
                                onClick={() =>
                                  setExpandedErrorLogId(
                                    expandedErrorLogId === errorLog.id
                                      ? null
                                      : errorLog.id
                                  )
                                }
                              >
                                {expandedErrorLogId === errorLog.id
                                  ? 'Hide'
                                  : 'Show'}{' '}
                                Stack Trace
                              </Button>
                            )}
                          </div>
                          {expandedErrorLogId === errorLog.id &&
                            errorLog.stackTrace && (
                              <div
                                style={{
                                  marginTop: '0.75rem',
                                  backgroundColor: '#fff',
                                  padding: '0.75rem',
                                  borderRadius: '4px',
                                  fontSize: '0.75rem',
                                  fontFamily: 'monospace',
                                  overflow: 'auto',
                                  maxHeight: '300px',
                                  border: '1px solid #e0e0e0',
                                  whiteSpace: 'pre-wrap',
                                  wordWrap: 'break-word',
                                }}
                              >
                                {errorLog.stackTrace}
                              </div>
                            )}
                        </div>
                      ))}
                    </div>
                  </WorkspaceInfoField>
                </>
              ) : (
                <WorkspaceInfoField labelText='Recovery Error Logs'>
                  No error logs available
                </WorkspaceInfoField>
              ))}
          </>
        )}
      </div>
    </>
  );
};
