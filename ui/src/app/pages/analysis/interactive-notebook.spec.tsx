import { MemoryRouter, Route } from 'react-router-dom';

import { AppStatus, WorkspaceAccessLevel } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { fireEvent, render, waitFor } from '@testing-library/react';
import * as swaggerClients from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { userAppsStore } from 'app/utils/stores';

import { createListAppsRStudioResponse } from 'testing/stubs/apps-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { InteractiveNotebook } from './interactive-notebook';

let mockNotebooksApi;

beforeEach(async () => {
  currentWorkspaceStore.next({
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.WRITER,
  });

  jest.mock('app/services/swagger-fetch-clients');
  mockNotebooksApi = jest.spyOn(swaggerClients, 'notebooksApi');
});

const setup = (mockOverrides) => {
  mockNotebooksApi.mockImplementation(() => ({
    ...mockOverrides,
  }));
};

test('Edit Rmd file', async () => {
  setup({
    getNotebookLockingMetadata: () => Promise.resolve({}),
  });
  const rStudioApp = createListAppsRStudioResponse({
    status: AppStatus.RUNNING,
  });
  userAppsStore.set({
    userApps: [rStudioApp],
  });
  const pathParameters = { params: { nbName: 'test.Rmd' } };
  const spyWindowOpen = jest.spyOn(window, 'open');
  spyWindowOpen.mockImplementation(jest.fn(() => window));
  render(
    <MemoryRouter
      initialEntries={[
        '/workspaces/sampleNameSpace/sampleWorkspace/notebooks/preview/example.Rmd',
      ]}
    >
      <Route path='/workspaces/:ns/:wsid/notebooks/preview/:nbName'>
        <InteractiveNotebook hideSpinner={() => {}} match={pathParameters} />
      </Route>
    </MemoryRouter>
  );
  const editButton = screen.getByTitle('Edit');
  fireEvent.click(editButton);
  await waitFor(() => {
    expect(spyWindowOpen).toHaveBeenCalledTimes(1);
    expect(spyWindowOpen).toHaveBeenCalledWith(
      rStudioApp.proxyUrls['rstudio-service'],
      '_blank'
    );
  });
});
