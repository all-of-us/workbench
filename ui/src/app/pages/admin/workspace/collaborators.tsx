import * as React from 'react';

import { WorkspaceAccessLevel, WorkspaceUserAdminView } from 'generated/fetch';

interface Props {
  creator: string;
  collaborators: WorkspaceUserAdminView[];
}
export const Collaborators = ({ creator, collaborators }: Props) => {
  const owners = collaborators.filter(
    (c) => c.role === WorkspaceAccessLevel.OWNER
  );
  const writers = collaborators.filter(
    (c) => c.role === WorkspaceAccessLevel.WRITER
  );
  const readers = collaborators.filter(
    (c) => c.role === WorkspaceAccessLevel.READER
  );

  const collabList = (users: WorkspaceUserAdminView[]) => {
    return users?.length > 0
      ? users.map((c) => (
          <div style={{ marginLeft: '1rem' }} key={c.userModel.userName}>
            {c.userModel.userName}
          </div>
        ))
      : 'None';
  };

  return (
    <>
      <h3>Collaborators</h3>
      <div className='collaborators' style={{ marginTop: '1.5rem' }}>
        <div>Creator and Owner: {creator}</div>
        <div>
          Other Owners:{' '}
          {collabList(owners?.filter((c) => c.userModel.userName !== creator))}
        </div>
        <div>Writers: {collabList(writers)}</div>
        <div>Readers: {collabList(readers)}</div>
      </div>
    </>
  );
};
