import * as React from 'react';
import { mount } from 'enzyme';

import { NotebooksApi, RuntimeApi, RuntimeStatus } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { isVisible } from 'app/utils/runtime-utils';
import { runtimeStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { findNodesContainingText } from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub, workspaceStubs } from 'testing/stubs/workspaces';

import { AppsPanel } from './apps-panel';

const stubFunction = () => ({});

const component = async () => {
  return mount(
    <AppsPanel
      workspace={workspaceStubs[0]}
      onClose={stubFunction}
      onClickRuntimeConf={stubFunction}
      onClickDeleteRuntime={stubFunction}
    />
  );
};

describe('AppsPanel', () => {
  let runtimeStub: RuntimeApiStub;
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({
      config: { ...defaultServerConfig, enableGkeApp: true },
    });
    runtimeStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClient(NotebooksApi, new NotebooksApiStub());
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

  // at initial implementation, Jupyter is the only possible ActiveApp
  // so what these tests actually show is whether Jupyter is an ActiveApp
  test.each([
    [RuntimeStatus.Running, true, true],
    [RuntimeStatus.Stopped, true, true],
    [RuntimeStatus.Stopping, true, true],
    [RuntimeStatus.Starting, true, true],
    [RuntimeStatus.Creating, true, true],
    [RuntimeStatus.Deleting, true, true],
    [RuntimeStatus.Updating, true, true],
    [RuntimeStatus.Deleted, false, true], // not visible [isVisible() = false]
    [RuntimeStatus.Error, false, true], // not visible [isVisible() = false]
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

      expect(
        findNodesContainingText(wrapper, 'Launch other applications').exists()
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
});
