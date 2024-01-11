import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import {
  NotebooksApi,
  UserAppEnvironment,
  WorkspacesApi,
} from 'generated/fetch';

import { renderModal } from '../../../testing/react-test-helpers';
import { RuntimeApiStub } from '../../../testing/stubs/runtime-api-stub';
import { ExpandedApp } from '../../components/apps-panel/expanded-app';
import { UIAppType } from '../../components/apps-panel/utils';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { analysisTabPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { displayDateWithoutHours } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

// There are two header rows, so this is the first row with data.
const FIRST_DATA_ROW_NUMBER = 2;

const component = async () =>
  renderModal(
    <MemoryRouter>
      <AppFilesList showSpinner={() => {}} hideSpinner={() => {}} />
    </MemoryRouter>
  );
describe('AppsList', () => {
  let notebooksApiStub: NotebooksApiStub;
  let user;
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    notebooksApiStub = new NotebooksApiStub();
    registerApiClient(NotebooksApi, notebooksApiStub);
    user = userEvent.setup();
  });

  it('should render new Analysis tab', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    await component();
    let firstDataRow;
    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];
    // First Column : Menu icon
    await waitFor(() => {
      firstDataRow = screen.getAllByRole('row')[FIRST_DATA_ROW_NUMBER];
      within(firstDataRow).getByTitle('Notebook Action Menu');
    });

    // Second Column displays the type of Application: In this case Jupyter
    within(firstDataRow).getByAltText('Jupyter');

    // // Fourth column of table displays file name with extension
    const expectedLink = `${analysisTabPath(
      workspaceDataStub.namespace,
      workspaceDataStub.id
    )}/preview/mockFile.ipynb`;
    expect(
      within(firstDataRow).getByRole('link', { name: firstNotebook.name })
    ).toHaveAttribute('href', expectedLink);

    // // Fifth column of notebook table displays last modified time
    within(firstDataRow).getByText(
      displayDateWithoutHours(firstNotebook.lastModifiedTime)
    );

    // Sixth column of notebook table displays last modified by
    within(firstDataRow).getByText(firstNotebook.lastModifiedBy);
  });

  it('should not render modal if notebook is greater than 5MB', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    await component();

    let firstDataRow;
    notebooksApiStub.notebookList[0].sizeInBytes = 5 * 1024 * 1024 + 1;

    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];
    let notebookLink;
    await waitFor(() => {
      firstDataRow = screen.getAllByRole('row')[FIRST_DATA_ROW_NUMBER];
      notebookLink = within(firstDataRow).getByRole('link', {
        name: firstNotebook.name,
      });
    });

    await user.click(notebookLink);

    await waitFor(() => {
      screen.getByText('Notebook file size bigger than 5mb');
    });
  });

  it('should not render modal if notebook is less or equal to 5MB', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    await component();

    let firstDataRow;
    notebooksApiStub.notebookList[0].sizeInBytes = 5 * 1024 * 1024;

    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];
    let notebookLink;
    await waitFor(() => {
      firstDataRow = screen.getAllByRole('row')[FIRST_DATA_ROW_NUMBER];
      notebookLink = within(firstDataRow).getByRole('link', {
        name: firstNotebook.name,
      });
    });

    await user.click(notebookLink);

    const modalTitle = screen.queryByText('Notebook file size bigger than 5mb');
    expect(modalTitle).toBeNull();
  });
});
