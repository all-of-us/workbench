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
  dropdownHeader: {
    fontSize: 12,
    lineHeight: '30px',
    color: colors.primary,
    fontWeight: 600,
    paddingLeft: 12,
    width: 160
  },
})

interface WorkspaceActionsProps {
  workspace: WorkspaceData,
  onDuplicate: Function,
  onEdit: Function,
  onShare: Function,
  onDelete: Function,
}
export const WorkspaceActionsMenu = (props: WorkspaceActionsProps) => {
  const {workspace, workspace: {accessLevel, adminLocked}, onDuplicate, onEdit, onShare, onDelete } = props;
  const isNotOwner = !workspace || accessLevel !== WorkspaceAccessLevel.OWNER;

  const ownerTip = 'Requires owner permission';
  const lockedTip = 'Workspace is locked by admin';

  return <React.Fragment>
    <div style={styles.dropdownHeader}>Workspace Actions</div>
    <MenuItem
      icon='copy'
      tooltip={adminLocked && lockedTip}
      disabled={adminLocked}
      onClick={() => onDuplicate()}>
      Duplicate
    </MenuItem>
    <MenuItem
      icon='pencil'
      tooltip={isNotOwner && ownerTip}
      disabled={isNotOwner}
      onClick={() => onEdit()}>
      Edit
    </MenuItem>
    <MenuItem
      icon='share'
      tooltip={(isNotOwner && ownerTip) || (adminLocked && lockedTip)}
      disabled={isNotOwner || adminLocked}
      onClick={() => onShare()}>
      Share
    </MenuItem>
    <MenuItem
      icon='trash'
      tooltip={(isNotOwner && ownerTip) || (adminLocked && lockedTip)}
      disabled={isNotOwner || adminLocked}
      onClick={() => onDelete()}>
      Delete
    </MenuItem>
  </React.Fragment>;
}
