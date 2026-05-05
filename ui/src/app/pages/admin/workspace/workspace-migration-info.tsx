import * as React from 'react';

import { Workspace } from 'generated/fetch';
import { MigrationState } from 'generated/fetch';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  workspace: Workspace;
}

export const WorkspaceMigrationInfo = ({ workspace }: Props) => {
  return (
    <>
      <h3>Workspace Migration</h3>

      <div className='basic-info' style={{ marginTop: '1.5rem' }}>
        <WorkspaceInfoField labelText='Migration Status'>
          {workspace.migrationState || MigrationState.NOT_STARTED}
        </WorkspaceInfoField>

        <WorkspaceInfoField labelText='VWB Workspace ID'>
          {workspace.migratedVwbWorkspaceId || 'N/A'}
        </WorkspaceInfoField>
      </div>
    </>
  );
};
