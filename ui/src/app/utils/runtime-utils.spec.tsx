import '@testing-library/jest-dom';

import * as React from 'react';
import { act } from 'react-dom/test-utils';

import { ListRuntimeResponse, RuntimeApi } from 'generated/fetch';

import { render, waitFor } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { getCreator } from 'app/utils/runtime-utils';
import {
  runtimeDiskStore,
  runtimeStore,
  serverConfigStore,
} from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { defaultRuntime, RuntimeApiStub } from 'testing/stubs/runtime-api-stub';

import { useCustomRuntime } from './runtime-hooks';

describe('runtime-utils', () => {
  const workspaceNamespace = 'test';

  const TestRuntime = ({ id }) => {
    const [{ currentRuntime }] = useCustomRuntime(
      workspaceNamespace,
      runtimeDiskStore.get().gcePersistentDisk
    );
    const { runtimeName = '' } = currentRuntime || {};
    return <div id={id}>{runtimeName}</div>;
  };

  const TestComponent = () => {
    return (
      <div>
        <TestRuntime id='1' />
        <TestRuntime id='2' />
      </div>
    );
  };

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
      workspaceNamespace,
      runtime: undefined,
      runtimeLoaded: true,
    });
    runtimeDiskStore.set({
      workspaceNamespace,
      gcePersistentDisk: undefined,
    });
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with a value', async () => {
    const { container } = render(<TestComponent />);

    // Runtime initialization is in progress at this point.
    const runtime = (id) => container.querySelector(`[id="${id}"]`);
    expect(runtime('1')).toHaveTextContent('');
    expect(runtime('2')).toHaveTextContent('');

    await waitFor(() => {
      expect(runtime('1')).toHaveTextContent('Runtime Name');
      expect(runtime('2')).toHaveTextContent('Runtime Name');
    });
  });

  it('should update when runtime store updates', async () => {
    const { container } = render(<TestComponent />);

    const runtime = (id) => container.querySelector(`[id="${id}"]`);

    await waitFor(() => {
      expect(runtime('1')).toHaveTextContent('Runtime Name');
      expect(runtime('2')).toHaveTextContent('Runtime Name');
    });

    act(() =>
      runtimeStore.set({
        ...runtimeStore.get(),
        runtime: {
          ...runtimeApiStub.runtime,
          runtimeName: 'foo',
        },
      })
    );
    await waitFor(() => {
      expect(runtime('1')).toHaveTextContent('foo');
      expect(runtime('2')).toHaveTextContent('foo');
    });
  });
});

describe(getCreator.name, () => {
  test.each([
    ['a runtime without a creator label', defaultRuntime(), undefined],
    [
      'a runtime with a creator label',
      { ...defaultRuntime(), labels: { creator: 'scientist@aou' } },
      'scientist@aou',
    ],
    ['a non-runtime object', {}, undefined],
    ['undefined', undefined, undefined],
    ['null', null, undefined],
  ])(
    'getCreator should have the expected result for %s',
    (
      desc: string,
      runtimeResponse: ListRuntimeResponse,
      expected: string | undefined
    ) => expect(getCreator(runtimeResponse)).toEqual(expected)
  );
});
