import * as React from 'react';
import { mount } from 'enzyme';

import { RuntimeStatus } from 'generated/fetch';

import {
  registerCompoundRuntimeOperation,
  runtimeStore,
} from 'app/utils/stores';

import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';

import { RuntimeStatusIndicator } from './runtime-status-indicator';
import {
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  UpdatingIcon,
} from './status-icon';

describe('Runtime Status Indicator', () => {
  test.each([
    [RuntimeStatus.CREATING, UpdatingIcon],
    [RuntimeStatus.STOPPED, StoppedIcon],
    [RuntimeStatus.RUNNING, RunningIcon],
    [RuntimeStatus.STOPPING, StoppingIcon],
    [RuntimeStatus.ERROR, ErrorIcon],
  ])(
    'Runtime Status indicator renders correct indicator when runtime is in %s state',
    (status, icon) => {
      const runtimeStub = new RuntimeApiStub();
      runtimeStub.runtime.status = status;
      runtimeStore.set({
        workspaceNamespace: '',
        runtime: runtimeStub.runtime,
        runtimeLoaded: true,
      });
      const wrapper = mount(<RuntimeStatusIndicator />);
      expect(wrapper.exists()).toBeTruthy();
      const statusIcon = wrapper.find(icon);
      expect(statusIcon.exists()).toBeTruthy();
    }
  );

  it('Verify that a runtime with an undefined status does not have a status indicator', () => {
    const runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = undefined;
    runtimeStore.set({
      workspaceNamespace: '',
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    const wrapper = mount(<RuntimeStatusIndicator />);
    expect(wrapper.exists()).toBeTruthy();
    const iconContainer = wrapper.find(
      'div[data-test-id="runtime-status-icon-container"]'
    );
    expect(iconContainer.exists()).toBeTruthy();
    expect(iconContainer.children().length).toEqual(0);
  });

  it('Verify that a runtime that is part of a compound runtimeop is shown as updating', () => {
    const currentWorkspaceNamespace = 'testNamespace';
    const runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = undefined;
    runtimeStore.set({
      workspaceNamespace: currentWorkspaceNamespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    const aborter = new AbortController();
    registerCompoundRuntimeOperation(currentWorkspaceNamespace, {
      pendingRuntime: runtimeStub.runtime,
      aborter,
    });

    const wrapper = mount(
      <RuntimeStatusIndicator workspaceNamespace={currentWorkspaceNamespace} />
    );
    expect(wrapper.exists()).toBeTruthy();
    const statusIcon = wrapper.find(UpdatingIcon);
    expect(statusIcon.exists()).toBeTruthy();
  });
});
