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

import { defaultRStudioConfig } from 'app/components/apps-panel/utils';
import { registerApiClient as registerLeoApiClient } from 'app/services/notebooks-swagger-fetch-clients';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { isVisible } from 'app/utils/runtime-utils';
import {
  notificationStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';
import { AppsApi as LeoAppsApi } from 'notebooks-generated/fetch';

import defaultServerConfig from 'testing/default-server-config';
import {
  findNodesContainingText,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsRStudioResponse,
} from 'testing/stubs/apps-api-stub';
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

  // these tests assume that there are no User GKE Apps
  // so what these tests actually show is whether Jupyter is an ActiveApp
  test.each([
    [RuntimeStatus.Running, true],
    [RuntimeStatus.Stopped, true],
    [RuntimeStatus.Stopping, true],
    [RuntimeStatus.Starting, true],
    [RuntimeStatus.Creating, true],
    [RuntimeStatus.Deleting, true],
    [RuntimeStatus.Updating, true],

    // not visible [isVisible() = false]

    [RuntimeStatus.Deleted, false],
    [RuntimeStatus.Error, false],

    [null, false],
  ])(
    'should render / not render ActiveApps and AvailableApps when the runtime status is %s',
    async (status, activeExpected) => {
      runtimeStub.runtime.status = status;

      const wrapper = await component();
      expect(wrapper.exists()).toBeTruthy();

      // sanity check: isVisible() is equivalent to activeExpected
      expect(!!isVisible(status)).toBe(activeExpected);

      // no User GKE Apps, so there is always at least one Available app (Cromwell)
      expect(findAvailableApps(wrapper, activeExpected).exists()).toBeTruthy();

      expect(findActiveApps(wrapper).exists()).toBe(activeExpected);
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
  // so what these tests actually show is whether a GKE app is an ActiveApp
  test.each([
    [AppType.CROMWELL, AppStatus.RUNNING, true],
    [AppType.CROMWELL, AppStatus.STOPPED, true],
    [AppType.CROMWELL, AppStatus.STOPPING, true],
    [AppType.CROMWELL, AppStatus.STARTING, true],
    [AppType.CROMWELL, AppStatus.PROVISIONING, true],
    [AppType.CROMWELL, AppStatus.DELETING, true],
    [AppType.CROMWELL, AppStatus.STATUSUNSPECIFIED, true],
    [AppType.CROMWELL, AppStatus.ERROR, true],

    // not visible [isVisible() = false]
    [AppType.CROMWELL, AppStatus.DELETED, false],

    [AppType.CROMWELL, null, false],

    // RStudio should have the same behavior.  Choose a few to demonstrate.

    [AppType.RSTUDIO, AppStatus.RUNNING, true],
    [AppType.RSTUDIO, AppStatus.DELETED, false],
  ])(
    'should render / not render ActiveApps and AvailableApps when %s status is %s',
    async (appType, status, activeExpected) => {
      runtimeStub.runtime.status = RuntimeStatus.Deleted;
      appsStub.listAppsResponse = [{ status, appType }];

      const wrapper = await component();
      expect(wrapper.exists()).toBeTruthy();
      await waitOneTickAndUpdate(wrapper);

      // no Jupyter runtimes, so there is always at least one Available app (Jupyter)
      expect(findAvailableApps(wrapper, activeExpected).exists()).toBeTruthy();

      expect(findActiveApps(wrapper).exists()).toBe(activeExpected);
    }
  );

  it('should render ActiveApps only, when all apps are RUNNING', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Running;
    appsStub.listAppsResponse = [
      { status: AppStatus.RUNNING, appType: AppType.CROMWELL },
      createListAppsRStudioResponse({ status: AppStatus.RUNNING }),
    ];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeTruthy();
    expect(findAvailableApps(wrapper, true).exists()).toBeFalsy();
  });

  it('should render AvailableApps only, when no apps are present', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeTruthy();
  });

  it('should render AvailableApps only, when all apps are DELETED', async () => {
    runtimeStub.runtime.status = RuntimeStatus.Deleted;
    appsStub.listAppsResponse = [
      { status: AppStatus.DELETED, appType: AppType.CROMWELL },
      createListAppsRStudioResponse({ status: AppStatus.DELETED }),
    ];

    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findActiveApps(wrapper).exists()).toBeFalsy();
    expect(findAvailableApps(wrapper, false).exists()).toBeTruthy();
  });

  it('should allow a user to expand available apps', async () => {
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

    // Click unexpanded Cromwell app

    expect(findUnexpandedApp(wrapper, 'Cromwell').exists()).toBeTruthy();
    const clickCromwell = findUnexpandedApp(wrapper, 'Cromwell').prop(
      'onClick'
    );
    await clickCromwell();
    await waitOneTickAndUpdate(wrapper);

    expect(findUnexpandedApp(wrapper, 'Cromwell').exists()).toBeFalsy();
    expect(findExpandedApp(wrapper, 'Cromwell').exists()).toBeTruthy();

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

  it('should not be possible to configure an RStudio app', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);
    await findUnexpandedApp(wrapper, 'RStudio').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    const rstudioPanel = findExpandedApp(wrapper, 'RStudio');

    expect(
      rstudioPanel
        .find({ 'data-test-id': `rstudio-settings-button` })
        .prop('disabled')
    ).toBeTruthy();
  });

  it('should pause a running RStudio app', async () => {
    runtimeStub.runtime.status = undefined;
    const userAppEnvironment = createListAppsRStudioResponse({
      status: AppStatus.RUNNING,
    });
    appsStub.listAppsResponse = [userAppEnvironment];
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);
    const rstudioPanel = findExpandedApp(wrapper, 'RStudio');
    const stopAppSpy = jest.spyOn(leoAppsStub, 'stopApp');

    expect(
      rstudioPanel.find({ 'data-test-id': `apps-panel-button-Resume` }).exists()
    ).toBeFalsy();
    const pauseButton = rstudioPanel.find({
      'data-test-id': `apps-panel-button-Pause`,
    });
    expect(pauseButton.exists()).toBeTruthy();

    pauseButton.simulate('click');
    expect(stopAppSpy).toHaveBeenCalledWith(
      userAppEnvironment.googleProject,
      userAppEnvironment.appName
    );
  });

  it('should resume a stopped RStudio app', async () => {
    runtimeStub.runtime.status = undefined;
    const userAppEnvironment = createListAppsRStudioResponse({
      status: AppStatus.STOPPED,
    });
    appsStub.listAppsResponse = [userAppEnvironment];
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);
    const rstudioPanel = findExpandedApp(wrapper, 'RStudio');
    const startAppSpy = jest.spyOn(leoAppsStub, 'startApp');

    expect(
      rstudioPanel.find({ 'data-test-id': `apps-panel-button-Pause` }).exists()
    ).toBeFalsy();
    const pauseButton = rstudioPanel.find({
      'data-test-id': `apps-panel-button-Resume`,
    });
    expect(pauseButton.exists()).toBeTruthy();

    pauseButton.simulate('click');
    expect(startAppSpy).toHaveBeenCalledWith(
      userAppEnvironment.googleProject,
      userAppEnvironment.appName
    );
  });

  it('should be able to launch an RStudio app', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);
    await findUnexpandedApp(wrapper, 'RStudio').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    appsStub.createApp = jest.fn(() => Promise.resolve({}));

    const launchButton = () =>
      findExpandedApp(wrapper, 'RStudio').find({
        'data-test-id': `rstudio-launch-button`,
      });
    expect(launchButton().prop('disabled')).toBeFalsy();
    expect(launchButton().prop('buttonText')).toEqual('Launch');

    launchButton().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(appsStub.createApp).toHaveBeenCalledWith(
      workspaceStub.namespace,
      defaultRStudioConfig
    );
    expect(launchButton().prop('buttonText')).toEqual('Launching');
    expect(launchButton().prop('disabled')).toBeTruthy();
  });

  it('should be able to delete an RStudio app', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [
      createListAppsRStudioResponse({ status: AppStatus.RUNNING }),
    ];
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);

    appsStub.deleteApp = jest.fn(() => Promise.resolve({}));

    const deleteButton = () =>
      findExpandedApp(wrapper, 'RStudio').find({
        'data-test-id': `RStudio-delete-button`,
      });
    expect(deleteButton().prop('disabled')).toBeFalsy();
    deleteButton().simulate('click');

    expect(appsStub.deleteApp).toHaveBeenCalled();
    expect(deleteButton().prop('disabled')).toBeTruthy();
  });

  it('should show an error if the initial request to launch RStudio fails', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];
    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);
    await findUnexpandedApp(wrapper, 'RStudio').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    appsStub.createApp = jest.fn(() => Promise.reject());

    findExpandedApp(wrapper, 'RStudio')
      .find({
        'data-test-id': `rstudio-launch-button`,
      })
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(notificationStore.get().title).toEqual(
      'Error Creating RStudio Environment'
    );
  });

  it('should not show RStudio if the feature flag is disabled', async () => {
    runtimeStub.runtime.status = undefined;
    appsStub.listAppsResponse = [];

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableRStudioGKEApp: false,
      },
    });

    const wrapper = await component();
    await waitOneTickAndUpdate(wrapper);

    expect(findUnexpandedApp(wrapper, 'RStudio').exists()).toBeFalsy();
  });
});
