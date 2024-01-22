import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { NotebooksApi, WorkspacesApi } from 'generated/fetch';

import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { displayDateWithoutHours } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

// There are two header rows, so this is the first row with data.
const FIRST_DATA_ROW_NUMBER = 2;

const component = async () =>
  render(
    <MemoryRouter>
      <AppFilesList showSpinner={() => {}} hideSpinner={() => {}} />
    </MemoryRouter>
  );
describe('AppsList', () => {
  let workspacesApiStub: WorkspacesApiStub;
  let notebooksApiStub: NotebooksApiStub;
  let user;
  beforeEach(() => {
    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);
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
    });

    within(firstDataRow).getByTitle('Notebook Action Menu');

    // Second Column displays the type of Application: In this case Jupyter
    within(firstDataRow).getByAltText('Jupyter');

    // Fourth column of table displays file name with extension
    within(firstDataRow).getByText(firstNotebook.name);

    // Fifth column of notebook table displays last modified time
    within(firstDataRow).getByText(
      displayDateWithoutHours(firstNotebook.lastModifiedTime)
    );

    // Sixth column of notebook table displays last modified by
    within(firstDataRow).getByText(firstNotebook.lastModifiedBy);
  });

  it('should render modal if notebook is greater than 5MB', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    await component();

    let firstDataRow;
    notebooksApiStub.notebookList[0].sizeInBytes = 5 * 1024 * 1024 + 1;

    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];

    await waitFor(() => {
      firstDataRow = screen.getAllByRole('row')[FIRST_DATA_ROW_NUMBER];
    });

    const notebookLink = within(firstDataRow).getByText(firstNotebook.name);

    await user.click(notebookLink);

    await waitFor(() => {
      screen.getByText('Notebook file size bigger than 5MB');
    });

    const modalCloseButton = screen.getByAltText('Close');
    await user.click(modalCloseButton);

    await waitFor(() => {
      expect(
        screen.queryByText('Notebook file size bigger than 5MB')
      ).not.toBeInTheDocument();
    });
  });

  it('should navigate to preview if notebook is less or equal to 5MB', async () => {
    currentWorkspaceStore.next(workspaceDataStub);
    await component();

    let firstDataRow;
    notebooksApiStub.notebookList[0].sizeInBytes = 5 * 1024 * 1024 - 1;

    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];
    await waitFor(() => {
      firstDataRow = screen.getAllByRole('row')[FIRST_DATA_ROW_NUMBER];
    });
    const notebookLink = within(firstDataRow).getByRole('link', {
      name: firstNotebook.name,
    });
    await user.click(notebookLink);

    expect(notebookLink).toHaveAttribute(
      'href',
      `/workspaces/${workspaceDataStub.namespace}/${workspaceDataStub.id}/analysis/preview/${firstNotebook.name}`
    );
  });

  it('should render WaitingForFiles when a transfer is not complete', async () => {
    workspacesApiStub.notebookTransferComplete = () => Promise.resolve(false);
    currentWorkspaceStore.next(workspaceDataStub);
    await component();

    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];
    await waitFor(() =>
      expect(screen.queryByText(firstNotebook.name)).not.toBeInTheDocument()
    );

    await waitFor(() =>
      expect(
        screen.queryByText(/Copying 1 or more notebooks from another workspace/)
      ).toBeInTheDocument()
    );
  });

  it("should not render WaitingForFiles when we don't know whether the transfer is complete", async () => {
    workspacesApiStub.notebookTransferComplete = () =>
      Promise.resolve(undefined);
    currentWorkspaceStore.next(workspaceDataStub);
    await component();

    const firstNotebook = (await notebooksApiStub.getNoteBookList())[0];
    await waitFor(() =>
      // we also do not display the notebook list
      expect(screen.queryByText(firstNotebook.name)).not.toBeInTheDocument()
    );

    await waitFor(() =>
      expect(
        screen.queryByText(/Copying 1 or more notebooks from another workspace/)
      ).not.toBeInTheDocument()
    );
  });
});
