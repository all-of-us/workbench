import { MemoryRouter, Route } from 'react-router-dom';

import { AppStatus, WorkspaceAccessLevel } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/setup/setup';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import * as swaggerClients from 'app/services/swagger-fetch-clients';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import {
  currentWorkspaceStore,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { userAppsStore } from 'app/utils/stores';
import { analysisTabName } from 'app/utils/user-apps-utils';

import { createListAppsRStudioResponse } from 'testing/stubs/apps-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { InteractiveNotebook } from './interactive-notebook';

let mockAppsApi;
let mockNotebooksApi;

beforeEach(async () => {
  currentWorkspaceStore.next({
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.WRITER,
  });

  jest.mock('app/services/swagger-fetch-clients');
  mockAppsApi = jest.spyOn(swaggerClients, 'appsApi');
  mockNotebooksApi = jest.spyOn(swaggerClients, 'notebooksApi');
});

const setup = (mockAppOverrides, mockNotebookOverrides): UserEvent => {
  mockAppsApi.mockImplementation(() => ({
    ...mockAppOverrides,
  }));
  mockNotebooksApi.mockImplementation(() => ({
    ...mockNotebookOverrides,
  }));
  return userEvent.setup();
};

const renderInteractiveNotebook = (pathParameters) =>
  render(
    <MemoryRouter
      initialEntries={[
        `/workspaces/sampleNameSpace/sampleWorkspace/${analysisTabName}/preview/example.Rmd`,
      ]}
    >
      <Route path={`/workspaces/:ns/:wsid/${analysisTabName}/preview/:nbName`}>
        <InteractiveNotebook hideSpinner={() => {}} match={pathParameters} />
      </Route>
    </MemoryRouter>
  );

test('Edit Rmd file with running RStudio', async () => {
  const user = setup(
    { localizeApp: () => Promise.resolve({}) },
    {
      getNotebookLockingMetadata: () => Promise.resolve({}),
    }
  );
  const rStudioApp = createListAppsRStudioResponse({
    status: AppStatus.RUNNING,
  });
  userAppsStore.set({
    userApps: [rStudioApp],
  });
  const pathParameters = { params: { nbName: 'test.Rmd' } };
  const spyWindowOpen = jest.spyOn(window, 'open');
  spyWindowOpen.mockImplementation(jest.fn(() => window));
  renderInteractiveNotebook(pathParameters);
  const editButton = screen.getByTitle('Edit');
  await user.click(editButton);
  await waitFor(() => {
    expect(spyWindowOpen).toHaveBeenCalledTimes(1);
    expect(spyWindowOpen).toHaveBeenCalledWith(
      rStudioApp.proxyUrls[GKE_APP_PROXY_PATH_SUFFIX],
      '_blank'
    );
  });
});

test('Should open the RStudio configuration panel when you click edit on an Rmd file without a running RStudio instance.', async () => {
  const user = setup(
    { localizeApp: () => Promise.resolve({}) },
    {
      getNotebookLockingMetadata: () => Promise.resolve({}),
    }
  );

  userAppsStore.set({
    userApps: [],
  });
  const pathParameters = { params: { nbName: 'test.Rmd' } };
  const spyWindowOpen = jest.spyOn(window, 'open');
  spyWindowOpen.mockImplementation(jest.fn(() => window));
  renderInteractiveNotebook(pathParameters);
  const editButton = screen.getByTitle('Edit');
  await user.click(editButton);
  expect(spyWindowOpen).toHaveBeenCalledTimes(0);
  expect(setSidebarActiveIconStore.value).toEqual(rstudioConfigIconId);
});
