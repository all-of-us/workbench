import * as React from 'react';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { MenuItem } from 'app/components/buttons';
import { WorkspaceData } from 'app/utils/workspace-data';

export interface WorkspaceActionsMenuProps {
  workspaceData: WorkspaceData;
  onDuplicate: Function;
  onEdit: Function;
  onShare: Function;
  onDelete: Function;
}
export const WorkspaceActionsMenu = (props: WorkspaceActionsMenuProps) => {
  const {
    workspaceData,
    workspaceData: { accessLevel, adminLocked },
    onDuplicate,
    onEdit,
    onShare,
    onDelete,
  } = props;
  const isNotOwner =
    !workspaceData || accessLevel !== WorkspaceAccessLevel.OWNER;

  const ownerTip = (action: string) => (
    <div data-test-id={`workspace-${action}-disabled-tooltip`}>
      Requires owner permission
    </div>
  );
  const lockedTip = (action: string) => (
    <div data-test-id={`workspace-${action}-disabled-tooltip`}>
      Workspace is locked by admin
    </div>
  );

  return (
    <React.Fragment>
      <MenuItem
        icon='copy'
        tooltip={adminLocked && lockedTip('duplicate')}
        disabled={adminLocked}
        onClick={() => onDuplicate()}
      >
        Duplicate
      </MenuItem>
      <MenuItem
        icon='pencil'
        tooltip={isNotOwner && ownerTip('edit')}
        disabled={isNotOwner}
        onClick={() => onEdit()}
      >
        Edit
      </MenuItem>
      <MenuItem
        icon='share'
        tooltip={
          (isNotOwner && ownerTip('share')) ||
          (adminLocked && lockedTip('share'))
        }
        disabled={isNotOwner || adminLocked}
        onClick={() => onShare()}
      >
        Share
      </MenuItem>
      <MenuItem
        aria-label='Delete'
        icon='trash'
        tooltip={
          (isNotOwner && ownerTip('delete')) ||
          (adminLocked && lockedTip('delete'))
        }
        disabled={isNotOwner || adminLocked}
        onClick={() => onDelete()}
      >
        Delete
      </MenuItem>
    </React.Fragment>
  );
};
