import { MemoryRouter, Route } from 'react-router-dom';
import { mockNavigate } from 'setupTests';

import {
  AppStatus,
  AppType,
  UserAppEnvironment,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/setup/setup';
import {
  helpSidebarConfigIdForUIApp,
  UIAppType,
} from 'app/components/apps-panel/utils';
import {
  rstudioConfigIconId,
  sasConfigIconId,
} from 'app/components/help-sidebar-icons';
import {
  analysisTabName,
  analysisTabPath,
  appDisplayPath,
} from 'app/routing/utils';
import * as swaggerClients from 'app/services/swagger-fetch-clients';
import {
  currentWorkspaceStore,
  sidebarActiveIconStore,
} from 'app/utils/navigation';
import {
  MatchParams,
  serverConfigStore,
  userAppsStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  createListAppsRStudioResponse,
  createListAppsSASResponse,
} from 'testing/stubs/apps-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

import { InteractiveNotebook } from './interactive-notebook';

let mockAppsApi;
let mockNotebooksApi;

beforeEach(async () => {
  serverConfigStore.set({ config: defaultServerConfig });
  currentWorkspaceStore.next({
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.WRITER,
  });

  jest.mock('app/services/swagger-fetch-clients');
  mockAppsApi = jest.spyOn(swaggerClients, 'appsApi');
  mockNotebooksApi = jest.spyOn(swaggerClients, 'notebooksApi');
  helpSidebarConfigIdForUIApp[UIAppType.RSTUDIO] = rstudioConfigIconId;
  helpSidebarConfigIdForUIApp[UIAppType.SAS] = sasConfigIconId;
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

const renderInteractiveNotebook = (pathParameters: { params: MatchParams }) =>
  render(
    <MemoryRouter
      initialEntries={[
        `${analysisTabPath('sampleNameSpace', 'sampleWorkspace')}/preview/${
          pathParameters.params.nbName
        }`,
      ]}
    >
      <Route
        path={`/workspaces/:ns/:terraName/${analysisTabName}/preview/:nbName`}
      >
        <InteractiveNotebook hideSpinner={() => {}} match={pathParameters} />
      </Route>
    </MemoryRouter>
  );

test.each([
  [
    '.Rmd',
    'RStudio',
    createListAppsRStudioResponse({
      status: AppStatus.RUNNING,
    }),
  ],
  [
    '.R',
    'RStudio',
    createListAppsRStudioResponse({
      status: AppStatus.RUNNING,
    }),
  ],
  [
    '.sas',
    'SAS',
    createListAppsSASResponse({
      status: AppStatus.RUNNING,
    }),
  ],
])(
  'Edit %s file with running %s',
  async (suffix: string, appType: string, app: UserAppEnvironment) => {
    const user = setup(
      { localizeApp: () => Promise.resolve({}) },
      {
        getNotebookLockingMetadata: () => Promise.resolve({}),
      }
    );
    userAppsStore.set({
      userApps: [app],
    });
    const matchParams: MatchParams = { nbName: `test${suffix}` };
    const pathParameters = { params: matchParams };
    const spyWindowOpen = jest.spyOn(window, 'open');
    spyWindowOpen.mockImplementation(jest.fn(() => window));
    renderInteractiveNotebook(pathParameters);
    const editButton = screen.getByTitle('Edit');
    await user.click(editButton);
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith([
        appDisplayPath('sampleNameSpace', 'sampleWorkspace', appType),
      ]);
    });
  }
);

test.each([
  [AppType.RSTUDIO, '.Rmd'],
  [AppType.RSTUDIO, '.R'],
  [AppType.SAS, '.sas'],
])(
  'Should open the %s configuration panel when you click edit on a %s file without a running environment',
  async (appType: AppType, suffix: string) => {
    const user = setup(
      { localizeApp: () => Promise.resolve({}) },
      {
        getNotebookLockingMetadata: () => Promise.resolve({}),
      }
    );

    userAppsStore.set({
      userApps: [],
    });
    const matchParams: MatchParams = { nbName: `test${suffix}` };
    const pathParameters = { params: matchParams };
    const spyWindowOpen = jest.spyOn(window, 'open');
    spyWindowOpen.mockImplementation(jest.fn(() => window));
    renderInteractiveNotebook(pathParameters);
    const editButton = screen.getByTitle('Edit');
    await user.click(editButton);
    expect(spyWindowOpen).toHaveBeenCalledTimes(0);
    if (appType === AppType.RSTUDIO) {
      expect(sidebarActiveIconStore.value).toEqual(rstudioConfigIconId);
    } else if (appType === AppType.SAS) {
      expect(sidebarActiveIconStore.value).toEqual(sasConfigIconId);
    } else {
      fail(`Unknown app type ${appType}`);
    }
  }
);
