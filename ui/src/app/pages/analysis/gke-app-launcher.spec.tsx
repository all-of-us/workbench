import '@testing-library/jest-dom';

import { MemoryRouter } from 'react-router';
import { Route } from 'react-router-dom';
import { mockNavigate } from 'setupTests';

import {
  AppStatus,
  AppType,
  ListAppsResponse,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GKEAppLauncher } from 'app/pages/analysis/gke-app-launcher';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { userAppsStore } from 'app/utils/stores';

import { workspaceStubs } from 'testing/stubs/workspaces';

const RSTUDIO_FAKE_URL = 'https://fakeRStudioUrl/';

const createApp = (appType: AppType = AppType.RSTUDIO) => [
  {
    googleProject: 'googleProject',
    appName: 'rstudioApp',
    appType: appType,
    status: AppStatus.RUNNING,
    proxyUrls: { app: RSTUDIO_FAKE_URL },
  },
];

const createRoute = (appType: string) => (
  <MemoryRouter
    initialEntries={[
      `/workspaces/defaultNamespace/1/analysis/userApp/${appType}`,
    ]}
  >
    <Route path='/workspaces/:ns/:wsid/analysis/userApp/:appType'>
      <GKEAppLauncher hideSpinner={() => {}} showSpinner={() => {}} />
    </Route>
  </MemoryRouter>
);

const setUpWorkspace = () => {
  currentWorkspaceStore.next({
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.WRITER,
  });
};

const setup = (
  queryParam: string = 'RStudio',
  app: ListAppsResponse = createApp()
) => {
  userAppsStore.set({
    updating: false,
    userApps: app,
  });
  setUpWorkspace();
  return {
    container: render(createRoute(queryParam)).container,
    user: userEvent.setup(),
  };
};

const setupEmptyUserStore = () => {
  userAppsStore.set({
    updating: false,
    userApps: [],
  });

  return {
    container: render(createRoute('RStudio')).container,
    user: userEvent.setup(),
  };
};

it('Should display iframe with rStudio App Url', async () => {
  setup();
  // Confirm IFrame exist
  expect(screen.getByTitle('RStudio embed')).toBeInTheDocument();
  // Confirm IFrame source is same as proxyUrl for Rstudio L24
  expect(screen.getByTitle('RStudio embed').getAttribute('src')).toBe(
    RSTUDIO_FAKE_URL
  );
});

it('Should display error message if App type is not found', async () => {
  setup('fakeApp');
  expect(
    screen.getByText(
      /an error was encountered with your app fakeapp\. to resolve, please see the applications side panel\./i
    )
  );
});

it('Should display error message if User App is not found', async () => {
  setup('RStudio', createApp(AppType.CROMWELL));
  expect(
    screen.getByText(
      /an error was encountered with your app rstudio\. to resolve, please see the applications side panel\./i
    )
  );
});

it('Should display error message if there are no user apps in userAppStore', async () => {
  setupEmptyUserStore();
  expect(
    screen.getByText(
      /an error was encountered with your app rstudio\. to resolve, please see the applications side panel\./i
    )
  );
});

it('Should redirect user to analysis page in case displayed App is deleted', async () => {
  // Confirm the RStudio app is up and displayed in iframe and no navigate is called
  setup();
  expect(screen.getByTitle('RStudio embed')).toBeInTheDocument();
  expect(mockNavigate).not.toHaveBeenCalledWith([
    'workspaces/defaultNamespace/1/analysis',
  ]);

  // Set the app status to be deleted and set it in userAppsStore
  const deletedAppList: ListAppsResponse = [
    {
      googleProject: 'googleProject',
      appName: 'rstudioApp',
      appType: AppType.RSTUDIO,
      status: AppStatus.DELETING,
      proxyUrls: { app: RSTUDIO_FAKE_URL },
    },
  ];
  userAppsStore.set({
    updating: false,
    userApps: deletedAppList,
  });

  await waitFor(() => {
    // Since the app is now deleted navigate to analysis tab for the same workspace
    expect(mockNavigate).toHaveBeenCalledWith([
      'workspaces/defaultNamespace/1/analysis',
    ]);
  });
});
