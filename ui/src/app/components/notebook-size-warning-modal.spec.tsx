import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mockNavigate } from 'setupTests';

import {
  NotebooksApi,
  UserAppEnvironment,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ExpandedApp } from 'app/components/apps-panel/expanded-app';
import { UIAppType } from 'app/components/apps-panel/utils';
import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { analysisTabName, analysisTabPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { displayDateWithoutHours } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { renderModal } from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { NotebookSizeWarningModal } from './notebook-size-warning-modal';

// There are two header rows, so this is the first row with data.
const FIRST_DATA_ROW_NUMBER = 2;
const mockNamespace = 'mockNamespace';
const mockWorkspaceId = 'mockNamespace';
const mockNotebookName = 'mockNamespace';
const expectedNavigation = [
  'workspaces',
  mockNamespace,
  mockWorkspaceId,
  analysisTabName,
  mockNotebookName,
];

const component = async (handleClose) =>
  renderModal(
    <NotebookSizeWarningModal
      {...{ handleClose }}
      nameSpace={mockNamespace}
      workspaceId={mockWorkspaceId}
      notebookName={mockNotebookName}
    />
  );

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
  let mockClose;
  beforeEach(() => {
    user = userEvent.setup();
    mockClose = jest.fn();
  });

  it('should render', async () => {
    await component(mockClose);
    screen.getByText('Notebook file size bigger than 5mb');
    screen.getByText('Opening this notebook', { exact: false });
  });

  it('should link to correct support article', async () => {
    await component(mockClose);

    const expectedLink =
      'https://support.researchallofus.org/hc/en-us/articles/10916327500436-How-to-clear-notebook-outputs-without-editing-them';
    expect(
      screen.getByRole('link', {
        name: 'How to clear notebook outputs without editing them',
      })
    ).toHaveAttribute('href', expectedLink);
  });

  it('should have a functional close button', async () => {
    await component(mockClose);
    await user.click(findCloseButton());
    expect(mockClose).toHaveBeenCalledTimes(1);
  });

  it('should have a functional edit button', async () => {
    await component(mockClose);
    await user.click(findEditButton());
    expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation, {
      playgroundMode: false,
    });
  });

  it('should have a functional playground button', async () => {
    await component(mockClose);
    await user.click(findPlaygroundButton());
    expect(mockNavigate).toHaveBeenCalledWith(expectedNavigation, {
      playgroundMode: true,
    });
  });
});
