import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AppsApi,
  BillingStatus,
  NotebooksApi,
  RuntimeApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
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

describe('AppsPanel', () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  const leoAppsStub = new LeoAppsApiStub();
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

  it('should show the disabled panel when the workspace has INACTIVE billing status', async () => {
    workspaceStub.billingStatus = BillingStatus.INACTIVE;

    const { container } = await component();
    expect(container).toBeInTheDocument();

    expect(
      screen.queryByText('Cloud services are disabled for this workspace.')
    ).toBeInTheDocument();

    expect(findActiveApps()).not.toBeInTheDocument();
    expect(findAvailableApps(false)).not.toBeInTheDocument();
  });

  test.each([
    [true, true],
    [true, false],
    [false, true],
    [false, false],
  ])(
    'should / should not show apps based on feature flags enableRStudioGKEApp %s, enableSasGKEApp %s',
    async (enableRStudioGKEApp, enableSasGKEApp) => {
      serverConfigStore.set({
        config: {
          ...defaultServerConfig,
          enableRStudioGKEApp,
          enableSasGKEApp,
        },
      });
      appsStub.listAppsResponse = [];

      await component();

      // Cromwell is always available
      expectUnexpandedApp('Cromwell');

      if (enableRStudioGKEApp) {
        expectUnexpandedApp('RStudio');
      } else {
        expectAppIsMissing('RStudio');
      }

      if (enableSasGKEApp) {
        expectUnexpandedApp('SAS');
      } else {
        expectAppIsMissing('SAS');
      }
    }
  );
});
