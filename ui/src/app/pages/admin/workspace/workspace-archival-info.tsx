import * as React from 'react';

import {
  MigrationState,
  Workspace,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspace: Workspace;
}

export const WorkspaceArchiveInfo = ({ workspace }: Props) => {
  const migrated = workspace.migrationState === MigrationState.FINISHED;

  return (
    <>
      <h3>Workspace Archive</h3>
      {migrated ? (
        <div className='basic-info' style={{ marginTop: '1.5rem' }}>
          <WorkspaceInfoField labelText='Archive'>
            No archival record – Workspace migrated to Verily
          </WorkspaceInfoField>
        </div>
      ) : (
        <div className='basic-info' style={{ marginTop: '1.5rem' }}>
          <WorkspaceInfoField labelText='Archive Status'>
            {workspace.recoveryState || WorkspaceRecoveryStatus.NOT_STARTED}
          </WorkspaceInfoField>
          <WorkspaceInfoField labelText='Recovered VWB Workspace ID'>
            {workspace.migratedVwbWorkspaceId || 'N/A'}
          </WorkspaceInfoField>
        </div>
      )}
    </>
  );
};
