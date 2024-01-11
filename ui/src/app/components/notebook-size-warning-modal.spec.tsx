import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

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
import { analysisTabPath } from 'app/routing/utils';
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

const component = async () =>
  renderModal(
    <NotebookSizeWarningModal
      handleClose={() => {}}
      handleEdit={() => {}}
      handlePlayground={() => {}}
    />
  );

function findCloseButton() {
  return screen.getByAltText('Close');
}
function findEditButton() {
  return screen.getByRole('button', {
    name: /run playground mode/i,
  });
}
function findPlaygroundButton() {
  return screen.getByRole('button', {
    name: /run playground mode/i,
  });
}
describe('AppsList', () => {
  let notebooksApiStub: NotebooksApiStub;
  let user;
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    notebooksApiStub = new NotebooksApiStub();
    registerApiClient(NotebooksApi, notebooksApiStub);
    user = userEvent.setup();
  });

  it('should notebook size warning modal', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    await component();
    screen.getByText('Notebook file size bigger than 5mb');
    screen.getByText('Opening this notebook', { exact: false });
    findCloseButton();
    findEditButton();
    findPlaygroundButton();
  });
});
