import '@testing-library/jest-dom';

import { MemoryRouter } from 'react-router';
import { Route } from 'react-router-dom';
import { mockNavigate } from 'setupTests';

import {
  AppStatus,
  AppType,
  ListAppsResponse,
  UserAppEnvironment,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { toAppType, UIAppType } from 'app/components/apps-panel/utils';
import { GKEAppLauncher } from 'app/pages/analysis/gke-app-launcher';
import { analysisTabPath, appDisplayPath } from 'app/routing/utils';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { userAppsStore } from 'app/utils/stores';

import { workspaceStubs } from 'testing/stubs/workspaces';

const RSTUDIO_FAKE_URL = 'https://fakeRStudioUrl/';
const SAS_FAKE_URL = 'https://fakeSasUrl/';
const defaultApps = () => [
  {
    googleProject: 'googleProject',
    appName: 'rstudioApp',
    appType: AppType.RSTUDIO,
    status: AppStatus.RUNNING,
    proxyUrls: { app: RSTUDIO_FAKE_URL },
  },
  {
    googleProject: 'googleProject',
    appName: 'sasApp',
    appType: AppType.SAS,
    status: AppStatus.RUNNING,
    proxyUrls: { app: SAS_FAKE_URL },
  },
];

const setUpWorkspace = () => {
  currentWorkspaceStore.next({
    ...workspaceStubs[0],
    accessLevel: WorkspaceAccessLevel.WRITER,
  });
};
const currentWorkspace = workspaceStubs[0];

const createRoute = (appType: string) => (
  <MemoryRouter
    initialEntries={[
      appDisplayPath(
        currentWorkspace.namespace,
        currentWorkspace.terraName,
        appType
      ),
    ]}
  >
    <Route path='/workspaces/:ns/:terraName/analysis/userApp/:appType'>
      <GKEAppLauncher hideSpinner={() => {}} showSpinner={() => {}} />
    </Route>
  </MemoryRouter>
);

const pathToCurrentWSAnalysisTab = analysisTabPath(
  currentWorkspace.namespace,
  currentWorkspace.terraName
);

const setup = (queryParam, userApps: UserAppEnvironment[] = defaultApps()) => {
  userAppsStore.set({
    updating: false,
    userApps: userApps,
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

test.each([
  [UIAppType.RSTUDIO, RSTUDIO_FAKE_URL],
  [UIAppType.SAS, SAS_FAKE_URL],
])(
  'Should display iframe with %s App Url',
  async (app: UIAppType, iframeSrcUrl: string) => {
    setup(app);
    // Confirm IFrame exist
    expect(screen.getByTitle(`${app} embed`)).toBeInTheDocument();
    // Confirm IFrame source is same as proxyUrl for App
    expect(screen.getByTitle(`${app} embed`).getAttribute('src')).toBe(
      iframeSrcUrl
    );
  }
);

test.each([
  [UIAppType.RSTUDIO, 'rstudioApp', RSTUDIO_FAKE_URL],
  [UIAppType.SAS, 'sasApp', SAS_FAKE_URL],
])(
  'Should redirect user to analysis page in case %s App is deleted',
  async (app: UIAppType, name: string, iframeUrl: string) => {
    // Confirm the App is up and displayed in iframe and no navigate is called
    setup(app);
    expect(screen.getByTitle(`${app} embed`)).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalledWith([pathToCurrentWSAnalysisTab]);

    // Set the app status to be deleted and set it in userAppsStore
    const deletedAppList: ListAppsResponse = [
      {
        googleProject: 'googleProject',
        appName: name,
        appType: toAppType[app],
        status: AppStatus.DELETING,
        proxyUrls: { app: iframeUrl },
      },
    ];
    userAppsStore.set({
      updating: false,
      userApps: deletedAppList,
    });

    await waitFor(() => {
      // Since the app is now deleted navigate to analysis tab for the same workspace
      expect(mockNavigate).toHaveBeenCalledWith([pathToCurrentWSAnalysisTab]);
    });
  }
);

it('Should display error message if valid User App is not in userAppStore', async () => {
  const cromwellUserApp = [
    {
      googleProject: 'googleProject',
      appName: 'cromwellApp',
      appType: AppType.CROMWELL,
      status: AppStatus.RUNNING,
      proxyUrls: { app: RSTUDIO_FAKE_URL },
    },
  ];
  setup('RStudio', cromwellUserApp);

  expect(
    screen.getByText(
      /an error was encountered with your rstudio environment\. to resolve, please see the applications side panel\./i
    )
  );
});

it('Should display error message if there are no user apps in userAppStore', async () => {
  setupEmptyUserStore();
  expect(
    screen.getByText(
      /an error was encountered with your rstudio environment\. to resolve, please see the applications side panel\./i
    )
  );
});
