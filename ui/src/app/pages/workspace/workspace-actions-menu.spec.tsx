import '@testing-library/jest-dom';

import * as React from 'react';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/setup/setup';
import {
  WorkspaceActionsMenu,
  WorkspaceActionsMenuProps,
} from 'app/pages/workspace/workspace-actions-menu';

import { workspaceDataStub } from 'testing/stubs/workspaces';

describe(WorkspaceActionsMenu.name, () => {
  const createWorkspaceActionsProps = (): WorkspaceActionsMenuProps => {
    return {
      workspaceData: { ...workspaceDataStub },
      onDuplicate: () => {},
      onEdit: () => {},
      onShare: () => {},
      onDelete: () => {},
    };
  };

  const setup = (): UserEvent => {
    return userEvent.setup();
  };

  const validateButtonEnabled = async (
    user: UserEvent,
    button: HTMLElement,
    relevantCallbackProp: Function
  ) => {
    // This is a workaround. The accessible way to do this is to either use <button> elements or set `aria-disabled` on interactive divs. Both are wide-reaching refactors.
    await user.click(button);
    expect(relevantCallbackProp).toHaveBeenCalled();
  };

  const validateButtonDisabled = async (
    user: UserEvent,
    button: HTMLElement,
    relevantCallbackProp: Function
  ) => {
    // This is a workaround. See validateButtonEnabled.
    await user.click(button);
    expect(relevantCallbackProp).not.toHaveBeenCalled();
  };

  it('enables all actions for owners', async () => {
    const user = setup();

    const props = createWorkspaceActionsProps();
    props.workspaceData.accessLevel = WorkspaceAccessLevel.OWNER;
    props.onDuplicate = jest.fn();
    props.onEdit = jest.fn();
    props.onShare = jest.fn();
    props.onDelete = jest.fn();

    render(<WorkspaceActionsMenu {...props} />);

    const duplicateButton = screen.getByText('Duplicate');
    const editButton = screen.getByText('Edit');
    const shareButton = screen.getByText('Share');
    const deleteButton = screen.getByText('Delete');

    await validateButtonEnabled(user, duplicateButton, props.onDuplicate);
    await validateButtonEnabled(user, editButton, props.onEdit);
    await validateButtonEnabled(user, shareButton, props.onShare);
    await validateButtonEnabled(user, deleteButton, props.onDelete);
  });

  it('allows duplicating for non-owners', async () => {
    const user = setup();

    const props = createWorkspaceActionsProps();
    props.workspaceData.accessLevel = WorkspaceAccessLevel.READER;
    props.onDuplicate = jest.fn();

    render(<WorkspaceActionsMenu {...props} />);

    const duplicateButton = screen.getByText('Duplicate');

    await validateButtonEnabled(user, duplicateButton, props.onDuplicate);
  });

  it('disables editing for non-owners', async () => {
    const user = setup();

    const props = createWorkspaceActionsProps();
    props.workspaceData.accessLevel = WorkspaceAccessLevel.READER;
    props.onEdit = jest.fn();

    render(<WorkspaceActionsMenu {...props} />);

    const editButton = screen.getByText('Edit');

    await validateButtonDisabled(user, editButton, props.onEdit);

    await user.hover(editButton);
    screen.getByText('Requires owner permission');
  });

  it('disables sharing for non-owners', async () => {
    const user = setup();

    const props = createWorkspaceActionsProps();
    props.workspaceData.accessLevel = WorkspaceAccessLevel.READER;
    props.onShare = jest.fn();

    render(<WorkspaceActionsMenu {...props} />);

    const shareButton = screen.getByText('Share');

    await validateButtonDisabled(user, shareButton, props.onShare);

    await user.hover(shareButton);
    screen.getByText('Requires owner permission');
  });

  it('disables deleting for non-owners', async () => {
    const user = setup();

    const props = createWorkspaceActionsProps();
    props.workspaceData.accessLevel = WorkspaceAccessLevel.READER;
    props.onDelete = jest.fn();

    render(<WorkspaceActionsMenu {...props} />);

    const deleteButton = screen.getByText('Delete');

    await validateButtonDisabled(user, deleteButton, props.onDelete);

    await user.hover(deleteButton);
    screen.getByText('Requires owner permission');
  });
});
