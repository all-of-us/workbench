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

  const ownerTip = (action: string) => <div data-test-id={`workspace-${action}-disabled-tooltip`}>Requires owner permission</div>;
  const lockedTip = (action: string) => <div data-test-id={`workspace-${action}-disabled-tooltip`}>Workspace is locked by admin</div>;

  return <React.Fragment>
    <MenuItem
      icon='copy'
      tooltip={adminLocked && lockedTip('duplicate')}
      disabled={adminLocked}
      onClick={() => onDuplicate()}>
      Duplicate
    </MenuItem>
    <MenuItem
      icon='pencil'
      tooltip={isNotOwner && ownerTip('edit')}
      disabled={isNotOwner}
      onClick={() => onEdit()}>
      Edit
    </MenuItem>
    <MenuItem
      icon='share'
      tooltip={(isNotOwner && ownerTip('share')) || (adminLocked && lockedTip('share'))}
      disabled={isNotOwner || adminLocked}
      onClick={() => onShare()}>
      Share
    </MenuItem>
    <MenuItem
      icon='trash'
      tooltip={(isNotOwner && ownerTip('delete')) || (adminLocked && lockedTip('delete'))}
      disabled={isNotOwner || adminLocked}
      onClick={() => onDelete()}>
      Delete
    </MenuItem>
  </React.Fragment>;
}
