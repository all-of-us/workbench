import '@testing-library/jest-dom';

import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { analysisTabName } from 'app/routing/utils';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { renderModal } from 'testing/react-test-helpers';

import {
  NotebookSizeWarningModal,
  NotebookSizeWarningModalProps,
} from './notebook-size-warning-modal';

function findCloseButton() {
  return screen.getByAltText('Close');
}
function findEditButton() {
  return screen.getByRole('button', {
    name: /edit/i,
  });
}
function findPlaygroundButton() {
  return screen.getByRole('button', {
    name: /run playground mode/i,
  });
}
describe('Notebook Size Warning Modal', () => {
  let user;

  const defaultProps: NotebookSizeWarningModalProps = {
    handleClose: () => {},
    nameSpace: 'mockNamespace',
    workspaceId: 'mockWorkspaceId',
    notebookName: 'mockNotebookName',
  };

  const component = async (
    propOverrides?: Partial<NotebookSizeWarningModalProps>
  ) =>
    renderModal(
      <NotebookSizeWarningModal {...{ ...defaultProps, ...propOverrides }} />
    );
  beforeEach(() => {
    user = userEvent.setup();
  });

  it('should render', async () => {
    await component();
    screen.getByText('Notebook file size bigger than 5MB');
    screen.getByText('Our system monitors network traffic', { exact: false });
  });

  it('should link to correct support article', async () => {
    await component();

    const expectedLink =
      'https://support.researchallofus.org/hc/en-us/articles/10916327500436-How-to-clear-notebook-outputs-without-editing-them';
    expect(
      screen.getByRole('link', {
        name: 'How to clear notebook outputs without editing them',
      })
    ).toHaveAttribute('href', expectedLink);
  });

  it('should have a functional close button', async () => {
    const mockClose = jest.fn();
    await component({ handleClose: mockClose });
    await user.click(findCloseButton());
    expect(mockClose).toHaveBeenCalledTimes(1);
  });

  it('should have a functional edit button', async () => {
    const expectedNavigation = [
      'workspaces',
      defaultProps.nameSpace,
      defaultProps.workspaceId,
      analysisTabName,
      defaultProps.notebookName,
    ];
    await component();
    await user.click(findEditButton());
    expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation, {
      queryParams: { playgroundMode: false },
    });
  });

  it('should have a functional playground button', async () => {
    const expectedNavigation = [
      'workspaces',
      defaultProps.nameSpace,
      defaultProps.workspaceId,
      analysisTabName,
      defaultProps.notebookName,
    ];
    await component();
    await user.click(findPlaygroundButton());
    expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation, {
      queryParams: { playgroundMode: true },
    });
  });

  it('should disable buttons (except close) and show a spinner when notebookName is null', async () => {
    const mockClose = jest.fn();
    await component({ notebookName: null, handleClose: mockClose });
    // Looking for spinner label
    screen.getByLabelText('Please Wait');
    await user.click(findEditButton());
    await user.click(findPlaygroundButton());
    expect(mockNavigate).not.toHaveBeenCalled();
    await user.click(findCloseButton());
    expect(mockClose).toHaveBeenCalledTimes(1);
  });
});
