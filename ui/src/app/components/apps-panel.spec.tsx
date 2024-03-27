import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AppsApi,
  BillingStatus,
  NotebooksApi,
  RuntimeApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient as registerLeoApiClient } from 'app/services/notebooks-swagger-fetch-clients';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import { AppsApi as LeoAppsApi } from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { LeoAppsApiStub } from 'testing/stubs/leo-apps-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { AppsPanel } from './apps-panel';

const stubFunction = () => ({});

const workspaceStub = workspaceDataStub;

const component = async () =>
  render(
    <AppsPanel
      workspace={workspaceStub}
      onClose={stubFunction}
      onClickRuntimeConf={stubFunction}
      onClickDeleteRuntime={stubFunction}
      onClickDeleteGkeApp={stubFunction}
    />
  );

const findActiveApps = () => screen.queryByText('Active Applications');

const findAvailableApps = (activeAppsExist: boolean) =>
  activeAppsExist
    ? screen.queryByText('Launch other applications')
    : screen.queryByText('Launch applications');

const findAppBanner = (name: string) => screen.queryByRole('img', { name });

const expectUnexpandedApp = (appName: string): HTMLElement => {
  // an unexpanded app is just the banner
  const appBanner = findAppBanner(appName);
  expect(appBanner).toBeInTheDocument();

  // now check if it's expanded - arbitrary button choice
  const deleteEnvButton = screen.queryByRole('button', {
    name: `Delete ${appName} Environment`,
  });
  expect(deleteEnvButton).not.toBeInTheDocument();

  return appBanner;
};

const expectAppIsMissing = (appName: string) => {
  expect(findAppBanner(appName)).not.toBeInTheDocument();
};

const expectExpandedApp = (appName: string): HTMLElement => {
  expect(findAppBanner(appName)).toBeInTheDocument();
  // arbitrary button choice
  const deleteEnvButton = screen.queryByRole('button', {
    name: `Delete ${appName} Environment`,
  });
  expect(deleteEnvButton).toBeInTheDocument();
  return deleteEnvButton;
};

describe(AppsPanel.name, () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  const leoAppsStub = new LeoAppsApiStub();
  let user;
  beforeEach(() => {
    serverConfigStore.set({
      config: defaultServerConfig,
    });
    registerApiClient(AppsApi, appsStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(RuntimeApi, runtimeStub);
    registerLeoApiClient(LeoAppsApi, leoAppsStub);
    workspaceStub.billingStatus = BillingStatus.ACTIVE;
    runtimeStore.set({
      workspaceNamespace: workspaceStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    user = userEvent.setup();
  });

  it('should allow a user to expand Jupyter', async () => {
    // initial state: no apps exist

    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];

    const { container } = await component();
    expect(container).toBeInTheDocument();

    expect(findActiveApps()).not.toBeInTheDocument();
    expect(findAvailableApps(false)).toBeInTheDocument();

    // Click unexpanded Jupyter app

    const jupyter = expectUnexpandedApp('Jupyter');
    jupyter.click();

    await waitFor(() => expectExpandedApp('Jupyter'));

    // the overall apps panel state doesn't change: there are still no ActiveApps
    // the newly expanded app is in the AvailableApps section

    expect(findActiveApps()).not.toBeInTheDocument();
    expect(findAvailableApps(false)).toBeInTheDocument();
  });

  it('should disable apps when billing is inactive', async () => {
    // initial state: no apps exist
    workspaceStub.billingStatus = BillingStatus.INACTIVE;

    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];

    const { container } = await component();
    expect(container).toBeInTheDocument();

    expect(findActiveApps()).not.toBeInTheDocument();
    expect(findAvailableApps(false)).toBeInTheDocument();
    // // Click unexpanded Jupyter app

    const jupyter = expectUnexpandedApp('Jupyter');
    await user.pointer([{ pointerName: 'mouse', target: jupyter }]);
    // Show tooltip when hovering over disabled Jupyter.
    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
    jupyter.click();
    // Expecting Jupter to be disabled. If it was not, the
    // "Create New" button would show after clicking on Jupyter.
    expect(screen.queryByText('Create New')).not.toBeInTheDocument();

    const cromwell = expectUnexpandedApp('Cromwell');
    await user.pointer([{ pointerName: 'mouse', target: cromwell }]);
    // Show tooltip when hovering over disabled Jupyter.
    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
    cromwell.click();
    // Expecting GKE Apps, such as Cromwell, to be disabled. If
    // they were not, the apps panel would close after clicking on
    // an unexpanded GKE app.
    expect(findAvailableApps(false)).toBeInTheDocument();
  });

  test.each([true, false])(
    'should / should not show apps based on feature flag enableSasGKEApp = %s',
    async (enableSasGKEApp) => {
      serverConfigStore.set({
        config: {
          ...defaultServerConfig,
          enableSasGKEApp,
        },
      });
      appsStub.listAppsResponse = [];

      await component();

      // Cromwell and RStudio are always available
      expectUnexpandedApp('Cromwell');
      expectUnexpandedApp('RStudio');

      if (enableSasGKEApp) {
        expectUnexpandedApp('SAS');
      } else {
        expectAppIsMissing('SAS');
      }
    }
  );
});
