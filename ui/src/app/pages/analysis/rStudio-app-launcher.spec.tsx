import '@testing-library/jest-dom';

import { Router } from 'react-router';
import { Route } from 'react-router-dom';
import { createMemoryHistory } from 'history';

import { AppType } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UIAppType } from 'app/components/apps-panel/utils';
import { GKEAppLauncher } from 'app/pages/analysis/gke-app-launcher';
import { userAppsStore } from 'app/utils/stores';

const setup = () => {
  userAppsStore.set({
    updating: false,
    userApps: [
      {
        googleProject: 'googleProject',
        appName: 'rstudioApp',
        appType: AppType.RSTUDIO,
        proxyUrls: { app: `https://fakeRStudioUrl/` },
      },
    ],
  });

  const history = createMemoryHistory({
    initialEntries: ['/workspaces/:ns/:wsid/RStudio/appname'],
  });

  return {
    container: render(
      <Router history={history}>
        <Route path={`/workspaces/:ns/:wsid/${UIAppType.RSTUDIO}/:nbName`}>
          <GKEAppLauncher hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </Router>
    ).container,
    user: userEvent.setup(),
  };
};

it('Should display iframe with rStudio App Url', async () => {
  setup();
  // Confirm IFrame exist
  expect(screen.getByTitle('RStudio embed')).toBeInTheDocument();
  // Confirm IFrame source is same as proxyUrl for Rstudio L24
  expect(screen.getByTitle('RStudio embed').getAttribute('src')).toBe(
    'https://fakeRStudioUrl/'
  );
});
