import {act} from 'react-dom/test-utils';
import {mount} from 'enzyme';
import {useCustomRuntime, RuntimeDiffState, findMostSevereDiffState} from 'app/utils/runtime-utils';
import {runtimeStore} from 'app/utils/stores';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {RuntimeApi} from 'generated/fetch/api';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate, waitForFakeTimersAndUpdate} from 'testing/react-test-helpers';
import * as React from 'react';

const WORKSPACE_NS = 'test';

const Runtime = ({id}) => {
  const [{currentRuntime}, ] = useCustomRuntime(WORKSPACE_NS)
  const {runtimeName = ''} = currentRuntime || {};
  return <div id={id}>{runtimeName}</div>
}

const TestComponent = () => {
  return <div>
    <Runtime id='1'/>
    <Runtime id='2'/>
  </div>;
}

describe('runtime-utils', () => {
  let runtimeApiStub: RuntimeApiStub;

  beforeEach(() => {
    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);

    // For a component using the runtime store to function properly, there must
    // be an active workspace context provided - in the real application this is
    // configured by a central component. This line simulates what would
    // normally happen in WorkspaceWrapper.
    runtimeStore.set({workspaceNamespace: WORKSPACE_NS, runtime: undefined});
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with a value', async () => {
    const wrapper = mount(<TestComponent/>);
    await waitOneTickAndUpdate(wrapper)

    // Runtime initialization is in progress at this point.
    const runtime = (id) => wrapper.find({id}).first();
    expect(runtime('1').text()).toEqual('');
    expect(runtime('2').text()).toEqual('');

    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtime('1').text()).toEqual('Runtime Name');
    expect(runtime('2').text()).toEqual('Runtime Name');
  });

  it('should update when runtime store updates', async () => {
    const wrapper = mount(<TestComponent/>);
    await waitOneTickAndUpdate(wrapper)

    const runtime = (id) => wrapper.find({id}).first();

    await waitForFakeTimersAndUpdate(wrapper);
    expect(runtime('1').text()).toEqual('Runtime Name');
    expect(runtime('2').text()).toEqual('Runtime Name');

    act(() => runtimeStore.set({
      ...runtimeStore.get(),
      runtime: {
        ...runtimeApiStub.runtime,
        runtimeName: 'foo'
      }
    }));
    waitForFakeTimersAndUpdate(wrapper);
    expect(runtime('1').text()).toEqual('foo');
    expect(runtime('2').text()).toEqual('foo');
  });

  test.each([
    [[], undefined],
    [[RuntimeDiffState.NEEDS_DELETE], RuntimeDiffState.NEEDS_DELETE],
    [
      [
        RuntimeDiffState.CAN_UPDATE_IN_PLACE,
        RuntimeDiffState.NEEDS_DELETE,
        RuntimeDiffState.NO_CHANGE,
        RuntimeDiffState.CAN_UPDATE_WITH_REBOOT,
        RuntimeDiffState.NO_CHANGE,
      ],
      RuntimeDiffState.NEEDS_DELETE
    ],
    [
      [
        RuntimeDiffState.CAN_UPDATE_IN_PLACE,
        RuntimeDiffState.NO_CHANGE,
        RuntimeDiffState.CAN_UPDATE_WITH_REBOOT,
        RuntimeDiffState.CAN_UPDATE_IN_PLACE
      ],
      RuntimeDiffState.CAN_UPDATE_WITH_REBOOT
    ],
    [
      [
        RuntimeDiffState.NO_CHANGE,
        RuntimeDiffState.CAN_UPDATE_IN_PLACE,
        RuntimeDiffState.NO_CHANGE
      ],
      RuntimeDiffState.CAN_UPDATE_IN_PLACE
    ],
    [
      [
        RuntimeDiffState.NO_CHANGE
      ],
      RuntimeDiffState.NO_CHANGE
    ],
  ])('findMostSevereDiffState(%s) = %s', (diffStates, want) => {
    expect(findMostSevereDiffState(diffStates)).toEqual(want);
  });
});
