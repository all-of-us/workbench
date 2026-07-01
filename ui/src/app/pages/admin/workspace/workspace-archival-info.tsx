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

  const canRecover =
    !migrated &&
    workspace.recoveryState !== WorkspaceRecoveryStatus.NOT_STARTED;

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
              {workspace.recoveryState || WorkspaceRecoveryStatus.NOT_STARTED}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Recovered VWB Workspace ID'>
              {workspace.migratedVwbWorkspaceId || 'N/A'}
            </WorkspaceInfoField>
          </>
        )}

        {canRecover && onRecover && (
          <div style={{ marginTop: '1rem' }}>
            <Button
              style={{ border: '2px solid', marginBottom: '0.5rem' }}
              type='secondary'
              onClick={onRecover}
            >
              Recover Workspace
            </Button>
          </div>
        )}
      </div>
    </>
  );
};
