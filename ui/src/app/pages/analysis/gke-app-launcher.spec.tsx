import '@testing-library/jest-dom';

import { Router } from 'react-router';
import { Route } from 'react-router-dom';
import { createMemoryHistory } from 'history';

import { AppType } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GKEAppLauncher } from 'app/pages/analysis/gke-app-launcher';
import { userAppsStore } from 'app/utils/stores';

const createHistory = (queryParam: string) => {
  const history = createMemoryHistory({
    initialEntries: ['/workspaces/:ns/:wsid/gkeApp'],
  });
  history.location.search = queryParam;
  return history;
};

const createApp = (appType: AppType = AppType.RSTUDIO) => ({
  googleProject: 'googleProject',
  appName: 'rstudioApp',
  appType: appType,
  proxyUrls: { app: `https://fakeRStudioUrl/` },
});

const createRoute = (queryParam: string) => (
  <Router history={createHistory(queryParam)}>
    <Route path={`/workspaces/:ns/:wsid/gkeApp`}>
      <GKEAppLauncher hideSpinner={() => {}} showSpinner={() => {}} />
    </Route>
  </Router>
);

const setup = (
  queryParam: string = 'appType=RStudio',
  appType: AppType = AppType.RSTUDIO
) => {
  userAppsStore.set({
    updating: false,
    userApps: [createApp(appType)],
  });

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
    container: render(createRoute('appType=RStudio')).container,
    user: userEvent.setup(),
  };
};

it('Should display iframe with rStudio App Url', async () => {
  setup();
  // Confirm IFrame exist
  expect(screen.getByTitle('Gke-App embed')).toBeInTheDocument();
  // Confirm IFrame source is same as proxyUrl for Rstudio L24
  expect(screen.getByTitle('Gke-App embed').getAttribute('src')).toBe(
    'https://fakeRStudioUrl/'
  );
});

it('Should display error message if App type is not found', async () => {
  setup('appType=fakeApp');
  expect(screen.getByText(/something went wrong please try later/i));
});

it('Should display error message if User App is not found', async () => {
  setup('appType=RStudio', AppType.CROMWELL);
  expect(screen.getByText(/something went wrong please try later/i));
});

it('Should display error message if there are no user apps in userAppStore', async () => {
  setupEmptyUserStore();
  expect(screen.getByText(/something went wrong please try later/i));
});
