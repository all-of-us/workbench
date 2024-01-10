import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { NotebooksApi, WorkspacesApi } from 'generated/fetch';

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

const appsFilesTable = (container) =>
  container
    .querySelector('[data-test-id="apps-file-list"]')
    .querySelector('tbody');
const appsFilesTableColumns = (container) =>
  appsFilesTable(container).querySelector('td');

const MENU_COLUMN_NUMBER = 0;
const APPLICATION_COLUMN_NUMBER = 1;
const NAME_COLUMN_NUMBER = 2;
const MODIFIED_DATE_COLUMN_NUMBER = 3;
const MODIFIED_BY_COLUMN_NUMBER = 4;

// There are two header rows, so this is the first row with data.
const FIRST_DATA_ROW_NUMBER = 2;
describe('AppsList', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(NotebooksApi, new NotebooksApiStub());
  });

  it('should render new Analysis tab', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const { container } = render(
      <MemoryRouter>
        <AppFilesList showSpinner={() => {}} hideSpinner={() => {}} />
      </MemoryRouter>
    );

    let firstDataRow;
    const firstNotebook = NotebooksApiStub.stubNotebookList()[0];
    // First Column : Menu icon
    await waitFor(() => {
      console.log(screen.logTestingPlaygroundURL());
      firstDataRow = screen.getAllByRole('row')[FIRST_DATA_ROW_NUMBER];
      expect(
        within(firstDataRow).getByTitle('Notebook Action Menu')
      ).toBeTruthy();
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
});
