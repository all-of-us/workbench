import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { mount } from 'enzyme';

import { RuntimeApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  AnalysisDiffState,
  findMostSevereDiffState,
  useCustomRuntime,
} from 'app/utils/runtime-utils';
import {
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  waitForFakeTimersAndUpdate,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';

const WORKSPACE_NS = 'test';

const Runtime = ({ id }) => {
  const [{ currentRuntime }] = useCustomRuntime(
    WORKSPACE_NS,
    runtimeDiskStore.get().gcePersistentDisk
  );
  const { runtimeName = '' } = currentRuntime || {};
  return <div id={id}>{runtimeName}</div>;
};

const TestComponent = () => {
  return (
    <div>
      <Runtime id='1' />
      <Runtime id='2' />
    </div>
  );
};

describe('runtime-utils', () => {
  let runtimeApiStub: RuntimeApiStub;

  beforeEach(() => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);
    serverConfigStore.set({ config: { ...defaultServerConfig } });

    // For a component using the runtime store to function properly, there must
    // be an active workspace context provided - in the real application this is
    // configured by a central component. This line simulates what would
    // normally happen in WorkspaceWrapper.
    runtimeStore.set({
      workspaceNamespace: WORKSPACE_NS,
      runtime: undefined,
      runtimeLoaded: true,
    });
    runtimeDiskStore.set({
      workspaceNamespace: WORKSPACE_NS,
      gcePersistentDisk: undefined,
    });
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with a value', async () => {
    const wrapper = mount(<TestComponent />);
    await waitOneTickAndUpdate(wrapper);

    // Runtime initialization is in progress at this point.
    const runtime = (id) => wrapper.find({ id }).first();
    expect(runtime('1').text()).toEqual('');
    expect(runtime('2').text()).toEqual('');

    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtime('1').text()).toEqual('Runtime Name');
    expect(runtime('2').text()).toEqual('Runtime Name');
  });

  it('should update when runtime store updates', async () => {
    const wrapper = mount(<TestComponent />);
    await waitOneTickAndUpdate(wrapper);

    const runtime = (id) => wrapper.find({ id }).first();

    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtime('1').text()).toEqual('Runtime Name');
    expect(runtime('2').text()).toEqual('Runtime Name');

    act(() =>
      runtimeStore.set({
        ...runtimeStore.get(),
        runtime: {
          ...runtimeApiStub.runtime,
          runtimeName: 'foo',
        },
      })
    );
    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtime('1').text()).toEqual('foo');
    expect(runtime('2').text()).toEqual('foo');
  });

  test.each([
    [[], undefined],
    [[AnalysisDiffState.NEEDS_DELETE], AnalysisDiffState.NEEDS_DELETE],
    [
      [AnalysisDiffState.NEEDS_DELETE, undefined],
      AnalysisDiffState.NEEDS_DELETE,
    ],
    [
      [
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NEEDS_DELETE,
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
        AnalysisDiffState.NO_CHANGE,
      ],
      AnalysisDiffState.NEEDS_DELETE,
    ],
    [
      [
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
      ],
      AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
    ],
    [
      [
        AnalysisDiffState.NO_CHANGE,
        AnalysisDiffState.CAN_UPDATE_IN_PLACE,
        AnalysisDiffState.NO_CHANGE,
      ],
      AnalysisDiffState.CAN_UPDATE_IN_PLACE,
    ],
    [[AnalysisDiffState.NO_CHANGE], AnalysisDiffState.NO_CHANGE],
  ])('findMostSevereDiffState(%s) = %s', (diffStates, want) => {
    expect(findMostSevereDiffState(diffStates)).toEqual(want);
  });
});
