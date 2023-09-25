import * as React from 'react';

import { WorkspaceUserAdminView } from 'generated/fetch';

interface Props {
  collaborators: WorkspaceUserAdminView[];
}
export const Collaborators = ({ collaborators }: Props) => (
  <>
    <h3>Collaborators</h3>
    <div className='collaborators' style={{ marginTop: '1.5rem' }}>
      {collaborators.map((c, i) => (
        <div key={i}>{`${c.userModel.userName}: ${c.role}`}</div>
      ))}
    </div>
  </>
);
