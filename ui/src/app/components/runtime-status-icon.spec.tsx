// import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { mount } from 'enzyme';

import { RuntimeApi, RuntimeStatus, WorkspacesApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  clearCompoundRuntimeOperations,
  compoundRuntimeOpStore,
  runtimeStore,
} from 'app/utils/stores';

import { waitForFakeTimersAndUpdate } from 'testing/react-test-helpers';
import { defaultRuntime, RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { RuntimeStatusIcon } from './runtime-status-icon';

let runtimeStub: RuntimeApiStub;

const component = () => {
  return mount(
    <RuntimeStatusIcon
      store={runtimeStore}
      workspaceNamespace={workspaceDataStub.namespace}
      compoundRuntimeOpStore={compoundRuntimeOpStore}
    />
  );
};

const setRuntimeStatus = (status: RuntimeStatus) => {
  const runtime = { ...defaultRuntime(), status };
  runtimeStub.runtime = runtime;
  runtimeStore.set({
    workspaceNamespace: workspaceDataStub.namespace,
    runtime,
    runtimeLoaded: true,
  });
};

const runtimeStatusIcon = (wrapper, exists = true) => {
  const icon = wrapper
    .find({ 'data-test-id': 'runtime-status-icon-container' })
    .find('svg');
  expect(icon.exists()).toEqual(exists);
  return icon;
};

const clearRuntime = () => {
  runtimeStub.runtime = null;
  runtimeStore.set({
    workspaceNamespace: workspaceDataStub.namespace,
    runtime: null,
    runtimeLoaded: false,
  });
};

describe('RuntimeStatusIcon', () => {
  beforeEach(() => {
    runtimeStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeStub);
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());

    currentWorkspaceStore.next(workspaceDataStub);

    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });

    compoundRuntimeOpStore.set({});

    // mock timers
    jest.useFakeTimers('modern');
  });

  afterEach(() => {
    act(() => clearCompoundRuntimeOperations());
    jest.useRealTimers();
  });

  it('should display dynamic runtime status icon', async () => {
    setRuntimeStatus(RuntimeStatus.Running);
    const wrapper = component();
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.running
    );

    act(() => setRuntimeStatus(RuntimeStatus.Deleting));
    await waitForFakeTimersAndUpdate(wrapper);

    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.stopping
    );

    act(() => clearRuntime());
    await waitForFakeTimersAndUpdate(wrapper);
    runtimeStatusIcon(wrapper, /* exists */ false);

    act(() => setRuntimeStatus(RuntimeStatus.Creating));
    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtimeStatusIcon(wrapper).prop('style').color).toEqual(
      colors.asyncOperationStatus.starting
    );
  });
});
