import * as React from 'react';

import {WorkspaceAccessLevel} from 'generated/fetch';
import {MenuItem} from 'app/components/buttons';
import {WorkspaceData} from 'app/utils/workspace-data';
import {reactStyles} from 'app/utils';
import colors, {colorWithWhiteness} from 'app/styles/colors';

const styles = reactStyles({
  icon: {
    background: colorWithWhiteness(colors.primary, 0.48),
    color: colors.white,
    display: 'table-cell',
    height: '46px',
    width: '45px',
    borderBottom: `1px solid ${colorWithWhiteness(colors.primary, 0.4)}`,
    cursor: 'pointer',
    textAlign: 'center',
    verticalAlign: 'middle'
  },
})

interface WorkspaceActionsProps {
  workspaceData: WorkspaceData,
  onDuplicate: Function,
  onEdit: Function,
  onShare: Function,
  onDelete: Function,
}
export const WorkspaceActionsMenu = (props: WorkspaceActionsProps) => {
  const {workspaceData, workspaceData: {accessLevel, adminLocked}, onDuplicate, onEdit, onShare, onDelete } = props;
  const isNotOwner = !workspaceData || accessLevel !== WorkspaceAccessLevel.OWNER;

  const ownerTip = 'Requires owner permission';
  const lockedTip = 'Workspace is locked by admin';

  return <React.Fragment>
    <MenuItem
      icon='copy'
      tooltip={adminLocked && <div data-test-id='workspace-duplicate-disabled-tooltip'>{lockedTip}</div>}
      disabled={adminLocked}
      onClick={() => onDuplicate()}>
      Duplicate
    </MenuItem>
    <MenuItem
      icon='pencil'
      tooltip={isNotOwner && <div data-test-id='workspace-edit-disabled-tooltip'>{ownerTip}</div>}
      disabled={isNotOwner}
      onClick={() => onEdit()}>
      Edit
    </MenuItem>
    <MenuItem
      icon='share'
      tooltip={(isNotOwner && <div data-test-id='workspace-share-disabled-tooltip'>{ownerTip}</div>) ||
        (adminLocked && <div data-test-id='workspace-share-disabled-tooltip'>{lockedTip}</div>)}
      disabled={isNotOwner || adminLocked}
      onClick={() => onShare()}>
      Share
    </MenuItem>
    <MenuItem
      icon='trash'
      tooltip={(isNotOwner && <div data-test-id='workspace-delete-disabled-tooltip'>{ownerTip}</div>) ||
        (adminLocked && <div data-test-id='workspace-delete-disabled-tooltip'>{lockedTip}</div>)}
      disabled={isNotOwner || adminLocked}
      onClick={() => onDelete()}>
      Delete
    </MenuItem>
  </React.Fragment>;
}
