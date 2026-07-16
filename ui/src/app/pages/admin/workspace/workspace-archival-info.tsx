import * as React from 'react';

import {
  MigrationState,
  Workspace,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspace: Workspace;
  onRecover?: () => void;
}

export const WorkspaceArchiveInfo = ({ workspace, onRecover }: Props) => {
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
                  workspace.recoveryState !== WorkspaceRecoveryStatus.REQUESTED
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
                      return 'Recovery Failed';

                    default:
                      return 'Not Archived';
                  }
                })()}
              </Button>
            </WorkspaceInfoField>

            <WorkspaceInfoField labelText='Recovered VWB Workspace ID'>
              {workspace.migratedVwbWorkspaceId || 'N/A'}
            </WorkspaceInfoField>
          </>
        )}
      </div>
    </>
  );
};
