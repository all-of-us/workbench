import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  AppsApi,
  AppStatus,
  AppType,
  BillingStatus,
  NotebooksApi,
  RuntimeApi,
  RuntimeStatus,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { isVisible } from 'app/utils/runtime-utils';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  findNodesContainingText,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { AppsApiStub } from 'testing/stubs/apps-api-stub';
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
  wrapper.find({ 'data-test-id': `${appName}-unexpanded` });

const findExpandedApp = (wrapper: ReactWrapper, appName: string) =>
  wrapper.find({ 'data-test-id': `${appName}-expanded` });

describe('AppsPanel', () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    environment.showAppsPanel = true;
    registerApiClient(AppsApi, appsStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(RuntimeApi, runtimeStub);
    runtimeStore.set({
      workspaceNamespace: workspaceStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
  });

  // these tests assume that there are no User GKE Apps
  // so what these tests actually show is whether Jupyter is an ActiveApp
  test.each([
    [RuntimeStatus.Running, true, true],
    [RuntimeStatus.Stopped, true, true],
    [RuntimeStatus.Stopping, true, true],
    [RuntimeStatus.Starting, true, true],
    [RuntimeStatus.Creating, true, true],
    [RuntimeStatus.Deleting, true, true],
    [RuntimeStatus.Updating, true, true],

    // not visible [isVisible() = false]

    [RuntimeStatus.Deleted, false, true],
    [RuntimeStatus.Error, false, true],

    [null, false, true],
  ])(
    'should render / not render ActiveApps and AvailableApps when the runtime status is %s',
    async (status, activeExpected, availableExpected) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component();
      expect(wrapper.exists()).toBeTruthy();

      // sanity check: isVisible() is equivalent to activeExpected
      expect(!!isVisible(status)).toBe(activeExpected);

      expect(findActiveApps(wrapper).exists()).toBe(activeExpected);
      expect(findAvailableApps(wrapper, activeExpected).exists()).toBe(
        availableExpected
      );
    }
  );

  // Error and Deleted statuses are not included because they're not "visible" [isVisible() = false]
  test.each([
    [RuntimeStatus.Creating, RuntimeStatus.Creating],
    [RuntimeStatus.Running, RuntimeStatus.Running],
    [RuntimeStatus.Updating, RuntimeStatus.Updating],
    [RuntimeStatus.Deleting, RuntimeStatus.Deleting],
    ['Paused', RuntimeStatus.Stopped],
    ['Pausing', RuntimeStatus.Stopping],
    ['Resuming', RuntimeStatus.Starting],
  ])(
    'should show the status text %s when the runtime status is %s',
    async (statusText, status) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component();
      expect(wrapper.exists()).toBeTruthy();

      const runtimeCost = wrapper.find('[data-test-id="runtime-cost"]');
      expect(runtimeCost.exists()).toBeTruthy();

      expect(runtimeCost.text()).toContain(statusText);
    }
  );

  // these tests assume that there are no Jupyter runtimes
  // so what these tests actually show is whether Cromwell is an ActiveApp
  test.each([
    [AppStatus.RUNNING, true, true],
    [AppStatus.STOPPED, true, true],
    [AppStatus.STOPPING, true, true],
    [AppStatus.STARTING, true, true],
    [AppStatus.PROVISIONING, true, true],
    [AppStatus.DELETING, true, true],
    [AppStatus.STATUSUNSPECIFIED, true, true],
    [AppStatus.ERROR, true, true],

    // not visible [isVisible() = false]
    [AppStatus.DELETED, false, true],

    [null, false, true],
  ])(
    'should render / not render ActiveApps and AvailableApps when the Cromwell status is %s',
    async (status, activeExpected, availableExpected) => {
      runtimeStub.runtime.status = RuntimeStatus.Deleted;
      appsStub.listAppsResponse = [{ status, appType: AppType.CROMWELL }];

      const wrapper = await component();
      expect(wrapper.exists()).toBeTruthy();
      await waitOneTickAndUpdate(wrapper);

      expect(findActiveApps(wrapper).exists()).toBe(activeExpected);
      expect(findAvailableApps(wrapper, activeExpected).exists()).toBe(
        availableExpected
      );
    }
  );

  it('should render ActiveApps only, when both Jupyter and Cromwell are RUNNING', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Running;
    appsStub.listAppsResponse = [
      { status: AppStatus.RUNNING, appType: AppType.CROMWELL },
    ];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeTruthy();
    expect(findAvailableApps(wrapper, true).exists()).toBeFalsy();
  });

  it('should render AvailableApps only, when neither Jupyter nor Cromwell are present', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeTruthy();
  });

  it('should render AvailableApps only, when both Jupyter and Cromwell are DELETED', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Deleted;
    appsStub.listAppsResponse = [
      { status: AppStatus.DELETED, appType: AppType.CROMWELL },
    ];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeTruthy();
  });

  it('should allow a user to expand available apps', async () => {
    // initial state: no Jupyter runtime or Cromwell app exists

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

    // Click unexpanded Cromwell app

    expect(findUnexpandedApp(wrapper, 'Cromwell').exists()).toBeTruthy();
    const clickCromwell = findUnexpandedApp(wrapper, 'Cromwell').prop(
      'onClick'
    );
    await clickCromwell();
    await waitOneTickAndUpdate(wrapper);

    expect(findUnexpandedApp(wrapper, 'Cromwell').exists()).toBeFalsy();
    expect(findExpandedApp(wrapper, 'Cromwell').exists()).toBeTruthy();

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
});
