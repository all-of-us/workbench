import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AppsApi,
  BillingStatus,
  NotebooksApi,
  RuntimeApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { registerApiClient as registerLeoApiClient } from 'app/services/notebooks-swagger-fetch-clients';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import { AppsApi as LeoAppsApi } from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { LeoAppsApiStub } from 'testing/stubs/leo-apps-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { AppsPanel } from './apps-panel';

const stubFunction = () => ({});
const NOW = new Date('2020-01-01');

let workspaceStub: WorkspaceData;

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

const mockInitialCredits = (
  exhausted: boolean,
  expired: boolean,
  expirationBypassed: boolean
) => {
  workspaceStub.initialCredits = {
    exhausted,
    expirationEpochMillis: NOW.getTime() + (expired ? -1 : 1),
    expirationBypassed,
  };
};

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

const expectExpandedApp = (appName: string): HTMLElement => {
  expect(findAppBanner(appName)).toBeInTheDocument();
  // arbitrary button choice
  const deleteEnvButton = screen.queryByRole('button', {
    name: `Delete ${appName} Environment`,
  });
  expect(deleteEnvButton).toBeInTheDocument();
  return deleteEnvButton;
};

const expectExpandableApp = async (user: UserEvent, appName: string) => {
  expect(findActiveApps()).not.toBeInTheDocument();
  expect(findAvailableApps(false)).toBeInTheDocument();

  // Click unexpanded app

  const unexpandedApp = expectUnexpandedApp(appName);
  unexpandedApp.click();

  await waitFor(() => expectExpandedApp(appName));

  // the overall apps panel state doesn't change: there are still no ActiveApps
  // the newly expanded app is in the AvailableApps section

  expect(findActiveApps()).not.toBeInTheDocument();
  expect(findAvailableApps(false)).toBeInTheDocument();
};

const expectAppsInAppsPanelToBeDisabled = async (user: UserEvent) => {
  expect(findActiveApps()).not.toBeInTheDocument();
  expect(findAvailableApps(false)).toBeInTheDocument();

  // Click unexpanded Jupyter app

  const jupyter = expectUnexpandedApp('Jupyter');

  await user.pointer([{ pointerName: 'mouse', target: jupyter }]);

  // Show tooltip when hovering over disabled Jupyter.
  await screen.findByText(
    'You have either run out of initial credits or have an inactive billing account.'
  );

  jupyter.click();
  // Expecting Jupyter to be disabled. If it was not, the
  // "Create New" button would show after clicking on Jupyter.
  expect(screen.queryByText('Create New')).not.toBeInTheDocument();

  const cromwell = expectUnexpandedApp('Cromwell');
  await user.pointer([{ pointerName: 'mouse', target: cromwell }]);
  // Show tooltip when hovering over disabled Cromwell.
  await screen.findByText(
    'You have either run out of initial credits or have an inactive billing account.'
  );
  cromwell.click();
  // Expecting GKE Apps, such as Cromwell, to be disabled. If
  // they were not, the apps panel would close after clicking on
  // an unexpanded GKE app.
  expect(findAvailableApps(false)).toBeInTheDocument();
};

describe(AppsPanel.name, () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  const leoAppsStub = new LeoAppsApiStub();
  let user: UserEvent;
  beforeEach(() => {
    const freeTierBillingAccountId = 'FreeTierBillingAccountId';
    registerApiClient(AppsApi, appsStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(RuntimeApi, runtimeStub);
    registerLeoApiClient(LeoAppsApi, leoAppsStub);
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];
    workspaceStub = {
      ...workspaceDataStub,
      billingStatus: BillingStatus.ACTIVE,
      billingAccountName: `billingAccounts/${freeTierBillingAccountId}`,
    };
    runtimeStore.set({
      workspaceNamespace: workspaceStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    serverConfigStore.set({
      config: { ...defaultServerConfig, freeTierBillingAccountId },
    });
    jest.useFakeTimers().setSystemTime(NOW);
    // AdvanceTimersByTime in order to allow fake timers and RTL queries to work together
    // https://testing-library.com/docs/user-event/options/#advancetimers
    user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should allow a user to expand Jupyter', async () => {
    mockInitialCredits(false, false, false);
    await component();

    await expectExpandableApp(user, 'Jupyter');
  });

  it('should disable apps when initial credit expiration is enabled and initial credits are exhausted', async () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableInitialCreditsExpiration: true,
      },
    });
    mockInitialCredits(true, false, false);

    const { container } = await component();
    expect(container).toBeInTheDocument();

    expect(findActiveApps()).not.toBeInTheDocument();
    expect(findAvailableApps(false)).toBeInTheDocument();

    // Click unexpanded Jupyter app

    const jupyter = expectUnexpandedApp('Jupyter');
    await user.pointer([{ pointerName: 'mouse', target: jupyter }]);
    // Show tooltip when hovering over disabled Jupyter.
    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
    jupyter.click();
    // Expecting Jupyter to be disabled. If it was not, the
    // "Create New" button would show after clicking on Jupyter.
    expect(screen.queryByText('Create New')).not.toBeInTheDocument();

    const cromwell = expectUnexpandedApp('Cromwell');
    await user.pointer([{ pointerName: 'mouse', target: cromwell }]);
    // Show tooltip when hovering over disabled Cromwell.
    await screen.findByText(
      'You have either run out of initial credits or have an inactive billing account.'
    );
    cromwell.click();
    // Expecting GKE Apps, such as Cromwell, to be disabled. If
    // they were not, the apps panel would close after clicking on
    // an unexpanded GKE app.
    expect(findAvailableApps(false)).toBeInTheDocument();
  });

  it('should disable apps when initial credit expiration is enabled and initial credits are expired', async () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableInitialCreditsExpiration: true,
      },
    });
    mockInitialCredits(false, true, false);

    await component();
    await expectAppsInAppsPanelToBeDisabled(user);
  });

  it('should disable apps when initial credit expiration is enabled, initial credits are expired, and expiration is bypassed', async () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableInitialCreditsExpiration: true,
      },
    });
    mockInitialCredits(false, true, true);

    await component();

    await expectExpandableApp(user, 'Jupyter');
  });

  it('should disable apps when initial credit expiration is enabled and billing is inactive', async () => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableInitialCreditsExpiration: false },
    });
    workspaceStub.billingStatus = BillingStatus.INACTIVE;

    await component();

    await expectAppsInAppsPanelToBeDisabled(user);
  });

  it('should show unexpanded apps by default', async () => {
    appsStub.listAppsResponse = [];

    await component();

    expectUnexpandedApp('Cromwell');
    expectUnexpandedApp('RStudio');
    expectUnexpandedApp('SAS');
  });
});
