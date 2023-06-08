import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  AppsApi,
  BillingStatus,
  NotebooksApi,
  RuntimeApi,
} from 'generated/fetch';

import { registerApiClient as registerLeoApiClient } from 'app/services/notebooks-swagger-fetch-clients';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';
import { AppsApi as LeoAppsApi } from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import {
  findNodesContainingText,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
import { LeoAppsApiStub } from 'testing/stubs/leo-apps-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { AppsPanel } from './apps-panel';

const stubFunction = () => ({});

const workspaceStub = workspaceDataStub;

const component = async () =>
  mount(
    <AppsPanel
      workspace={workspaceStub}
      onClose={stubFunction}
      onClickRuntimeConf={stubFunction}
      onClickDeleteRuntime={stubFunction}
      onClickDeleteGkeApp={stubFunction}
    />
  );

const findActiveApps = (wrapper: ReactWrapper) =>
  findNodesContainingText(wrapper, 'Active applications');

const findAvailableApps = (wrapper: ReactWrapper, activeAppsExist: boolean) => {
  // the text changes based on whether there are active applications above this section
  const availableExpectedHeader = activeAppsExist
    ? 'Launch other applications'
    : 'Launch applications';

  return findNodesContainingText(wrapper, availableExpectedHeader);
};

const findUnexpandedApp = (wrapper: ReactWrapper, appName: string) =>
  wrapper.find({ 'data-test-id': `${appName}-unexpanded` }).first();

const findExpandedApp = (wrapper: ReactWrapper, appName: string) =>
  wrapper.find({ 'data-test-id': `${appName}-expanded` });

describe('AppsPanel', () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  const leoAppsStub = new LeoAppsApiStub();
  beforeEach(() => {
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableCromwellGKEApp: true },
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

  it('should allow a user to expand Jupyter and RStudio', async () => {
    // initial state: no apps exist

    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeTruthy();

    // Click unexpanded Jupyter app

    expect(findUnexpandedApp(wrapper, 'Jupyter').exists()).toBeTruthy();
    const clickJupyter = findUnexpandedApp(wrapper, 'Jupyter').prop('onClick');
    await clickJupyter();
    await waitOneTickAndUpdate(wrapper);

    expect(findUnexpandedApp(wrapper, 'Jupyter').exists()).toBeFalsy();
    expect(findExpandedApp(wrapper, 'Jupyter').exists()).toBeTruthy();

    // Click unexpanded RStudio app

    expect(findUnexpandedApp(wrapper, 'RStudio').exists()).toBeTruthy();
    const clickRStudio = findUnexpandedApp(wrapper, 'RStudio').prop('onClick');
    await clickRStudio();
    await waitOneTickAndUpdate(wrapper);

    expect(findUnexpandedApp(wrapper, 'RStudio').exists()).toBeFalsy();
    expect(findExpandedApp(wrapper, 'RStudio').exists()).toBeTruthy();

    // the overall apps panel state doesn't change: there are still no ActiveApps
    // the newly expanded apps are in the AvailableApps section

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeTruthy();
  });

  it('should show the disabled panel when the workspace has INACTIVE billing status', async () => {
    workspaceStub.billingStatus = BillingStatus.INACTIVE;

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(
      wrapper.find({ 'data-test-id': 'environment-disabled-panel' }).exists()
    ).toBeTruthy();

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeFalsy();
  });

  test.each([
    [true, true],
    [true, false],
    [false, true],
    [false, false],
  ])(
    'should / should not show apps when the feature flags are Rstudio=%s, Cromwell=%s',
    async (enableRStudioGKEApp, enableCromwellGKEApp) => {
      serverConfigStore.set({
        config: {
          ...defaultServerConfig,
          enableRStudioGKEApp,
          enableCromwellGKEApp,
        },
      });
      appsStub.listAppsResponse = [];

      const wrapper = await component();
      await waitOneTickAndUpdate(wrapper);

      expect(findUnexpandedApp(wrapper, 'RStudio').exists()).toEqual(
        enableRStudioGKEApp
      );
      expect(findUnexpandedApp(wrapper, 'Cromwell').exists()).toEqual(
        enableCromwellGKEApp
      );
    }
  );
});
