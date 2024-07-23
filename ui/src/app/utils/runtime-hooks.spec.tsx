import { RuntimeApi, RuntimeStatus } from 'generated/fetch';

import { waitFor } from '@testing-library/react';
// Hooks cannot be tested outside of a functional component, so we need to use renderHook from @testing-library/react-hooks
// This is explained further here: https://github.com/testing-library/react-hooks-testing-library?tab=readme-ov-file#the-problem
// TODO: Once we update to React 18 and a more recent RTL version,
//  these imports can be replaced with the standard RTL and
// @testing-library/react-hooks can be uninstalled
import { act, renderHook } from '@testing-library/react-hooks';
import {
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';

import { defaultRuntime, RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { LeoRuntimeInitializer } from './leo-runtime-initializer';
import { useCustomRuntime } from './runtime-hooks';
import { runtimeStore } from './stores';

describe('useCustomRuntime', () => {
  let deleteRuntimeSpy;
  let initializerSpy;
  let updateRuntimeSpy;
  let currentRuntime;

  beforeEach(() => {
    jest.useFakeTimers();
    currentRuntime = defaultRuntime();
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: currentRuntime,
      runtimeLoaded: true,
    });

    registerApiClient(RuntimeApi, new RuntimeApiStub());
    deleteRuntimeSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');
    initializerSpy = jest.spyOn(LeoRuntimeInitializer, 'initialize');
    updateRuntimeSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  const testUseCustomRuntime = () => {
    const { result } = renderHook(() =>
      useCustomRuntime('workspaceNamespace', null)
    );

    return result.current;
  };

  it('should update runtime when request includes updated runtime', async () => {
    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = defaultRuntime();
    newRuntime.gceConfig.diskSize = newRuntime.gceConfig.diskSize * 2;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    expect(updateRuntimeSpy).toHaveBeenCalledWith('workspaceNamespace', {
      runtime: newRuntime,
    });
  });

  it('should not update runtime when request does not include an updated runtime', async () => {
    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = currentRuntime;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    expect(updateRuntimeSpy).not.toHaveBeenCalled();
  });

  it('should delete runtime and create a new one when disk is decreased', async () => {
    const newDisk = null;
    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = defaultRuntime();
    // THis is what is triggering the delete
    newRuntime.gceConfig.diskSize = currentRuntime.gceConfig.diskSize / 2;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: newDisk });
    });

    await waitFor(() => {
      expect(deleteRuntimeSpy).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(initializerSpy).toHaveBeenCalled();
    });
  });

  it('should delete runtime and create a new one when current runtime is in error state', async () => {
    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = { ...currentRuntime };

    currentRuntime.status = RuntimeStatus.ERROR;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    await waitFor(() => {
      expect(deleteRuntimeSpy).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(initializerSpy).toHaveBeenCalled();
    });
  });
});
