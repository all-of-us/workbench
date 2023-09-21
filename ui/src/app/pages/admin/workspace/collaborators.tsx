import * as React from 'react';

import { WorkspaceUserAdminView } from 'generated/fetch';

interface Props {
  collaborators: WorkspaceUserAdminView[];
}
export const Collaborators = ({ collaborators }: Props) => (
  <>
    <h3>Collaborators</h3>
    <div className='collaborators' style={{ marginTop: '1.5rem' }}>
      {collaborators.map((workspaceUserAdminView, i) => (
        <div key={i}>
          {`${workspaceUserAdminView.userModel.userName}: ${workspaceUserAdminView.role}`}
        </div>
      ))}
    </div>
  </>
);
