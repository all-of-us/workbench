import { Disk, RuntimeApi, RuntimeStatus } from 'generated/fetch';

import { waitFor } from '@testing-library/react';
import { act, renderHook } from '@testing-library/react-hooks';
import {
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';

import { stubDisk } from 'testing/stubs/disks-api-stub';
import { defaultRuntime, RuntimeApiStub } from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { LeoRuntimeInitializer } from './leo-runtime-initializer';
import { useCustomRuntime } from './runtime-hooks';
import { runtimeStore } from './stores';
import useFakeTimers = jest.useFakeTimers;

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

  const testUseCustomRuntime = (currentDisk?: Disk) => {
    const { result } = renderHook(() =>
      useCustomRuntime('workspaceNamespace', null)
    );

    return result.current;
  };

  it('should update runtime when request includes updated runtime', async () => {
    const [_, setRequest] = testUseCustomRuntime();

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
    const [_, setRequest] = testUseCustomRuntime();

    const newRuntime = currentRuntime;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    expect(updateRuntimeSpy).not.toHaveBeenCalled();
  });

  it('should delete runtime and create a new one when disk is decreased', async () => {
    const currentDisk = stubDisk();
    const newDisk = null;
    const [_, setRequest] = testUseCustomRuntime(currentDisk);

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
    const [_, setRequest] = testUseCustomRuntime();

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
