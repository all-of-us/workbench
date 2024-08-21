import * as React from 'react';

import { WorkspaceAccessLevel, WorkspaceUserAdminView } from 'generated/fetch';

import { reactStyles } from 'app/utils';

const styles = reactStyles({
  group: {
    marginBottom: '1rem',
  },
  item: {
    marginLeft: '1rem',
  },
});

const collabList = (users: WorkspaceUserAdminView[]) => {
  return users?.length > 0
    ? users.map((c) => (
        <div style={styles.item} key={c.userModel.userName}>
          {c.userModel.userName}
        </div>
      ))
    : 'None';
};

interface Props {
  creator: string;
  collaborators: WorkspaceUserAdminView[];
}
export const Collaborators = ({ creator, collaborators }: Props) => {
  return (
    <>
      <h3>Collaborators</h3>
      <div className='collaborators' style={{ marginTop: '1.5rem' }}>
        <div style={styles.group}>Creator and Owner: {creator}</div>
        <div style={styles.group}>
          Other Owners:{' '}
          {collabList(
            collaborators
              ?.filter((c) => c.role === WorkspaceAccessLevel.OWNER)
              ?.filter((c) => c.userModel.userName !== creator)
          )}
        </div>
        <div style={styles.group}>
          Writers:{' '}
          {collabList(
            collaborators?.filter((c) => c.role === WorkspaceAccessLevel.WRITER)
          )}
        </div>
        <div style={styles.group}>
          Readers:{' '}
          {collabList(
            collaborators?.filter((c) => c.role === WorkspaceAccessLevel.READER)
          )}
        </div>
      </div>
    </>
  );
};
