import * as React from 'react';
import { mount } from 'enzyme';

import {
  AppsApi,
  AppStatus,
  AppType,
  NotebooksApi,
  RuntimeApi,
  RuntimeStatus,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
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
import { workspaceDataStub, workspaceStubs } from 'testing/stubs/workspaces';

import { AppsPanel } from './apps-panel';

const stubFunction = () => ({});

const component = async () =>
  mount(
    <AppsPanel
      workspace={workspaceStubs[0]}
      onClose={stubFunction}
      onClickRuntimeConf={stubFunction}
      onClickDeleteRuntime={stubFunction}
    />
  );

describe('AppsPanel', () => {
  const appsStub = new AppsApiStub();
  const runtimeStub = new RuntimeApiStub();
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({ config: defaultServerConfig });
    environment.showAppsPanel = true;
    registerApiClient(AppsApi, appsStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    registerApiClient(RuntimeApi, runtimeStub);
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
  });

  it('should render', async () => {
    const wrapper = await component();
    expect(wrapper.exists()).toBeTruthy();
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

      expect(
        findNodesContainingText(wrapper, 'Active applications').exists()
      ).toBe(activeExpected);

      // this changes based on whether there are active applications above this section
      const availableExpectedHeader = activeExpected
        ? 'Launch other applications'
        : 'Launch applications';

      expect(
        findNodesContainingText(wrapper, availableExpectedHeader).exists()
      ).toBe(availableExpected);
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

      expect(
        findNodesContainingText(wrapper, 'Active applications').exists()
      ).toBe(activeExpected);

      // this changes based on whether there are active applications above this section
      const availableExpectedHeader = activeExpected
        ? 'Launch other applications'
        : 'Launch applications';

      expect(
        findNodesContainingText(wrapper, availableExpectedHeader).exists()
      ).toBe(availableExpected);
    }
  );
});
