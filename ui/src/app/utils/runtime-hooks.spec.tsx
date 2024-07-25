import {
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';

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

import {
  defaultDataProcRuntime,
  defaultGceRuntime,
  defaultRuntime,
  RuntimeApiStub,
} from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { LeoRuntimeInitializer } from './leo-runtime-initializer';
import { DATAPROC_MIN_DISK_SIZE_GB } from './machines';
import { useCustomRuntime } from './runtime-hooks';
import { runtimeStore } from './stores';

describe(useCustomRuntime.name, () => {
  let deleteRuntimeSpy;
  let initializerSpy;
  let updateRuntimeSpy;
  let currentRuntime;

  beforeEach(() => {
    jest.useFakeTimers();
    // useCustomRuntime requires a runtime to be loaded in the
    // runtimeStore. All tests start with the default runtime.
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

    // Result's current property is the return value of the hook
    return result.current;
  };

  it('should update runtime when request includes updated runtime', async () => {
    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = defaultRuntime();
    newRuntime.gceConfig.diskSize = currentRuntime.gceConfig.diskSize * 2;

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

    // Since the runtimes are the same, the hook should not update the runtime
    expect(updateRuntimeSpy).not.toHaveBeenCalled();
  });

  it('should delete runtime and create a new one when disk is decreased', async () => {
    const newDisk = null;
    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = defaultRuntime();
    newRuntime.gceConfig.diskSize = currentRuntime.gceConfig.diskSize / 2;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: newDisk });
    });

    // Triggers a delete, because disk size decreased
    await waitFor(() => {
      expect(deleteRuntimeSpy).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(initializerSpy).toHaveBeenCalled();
    });
  });

  it('should delete runtime and create a new one when current runtime is in error state', async () => {
    const [, setRequest] = testUseCustomRuntime();

    // Set the new runtime to be the same as the current runtime, but
    // by using the spread operator, we create a new object
    // rather than a reference
    const newRuntime = { ...currentRuntime };

    // This means that settign the status to error will not affect the new runtime.
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

  it('should allow Dataproc -> PD transition', async () => {
    currentRuntime = defaultDataProcRuntime();
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: currentRuntime,
      runtimeLoaded: true,
    });

    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = defaultGceRuntime();

    // This means that settign the status to error will not affect the new runtime.
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

  it("should update runtime, and not delete, when dataproc's master disk size is increased", async () => {
    currentRuntime = {
      ...defaultRuntime(),
      status: RuntimeStatus.RUNNING,
      configurationType: RuntimeConfigurationType.USER_OVERRIDE,
      gceConfig: null,
      gceWithPdConfig: null,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 1000,
        numberOfWorkers: 2,
        numberOfPreemptibleWorkers: 0,
        workerMachineType: 'n1-standard-4',
        workerDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
      },
    };

    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: currentRuntime,
      runtimeLoaded: true,
    });

    const [, setRequest] = testUseCustomRuntime();
    const newRuntime = JSON.parse(JSON.stringify(currentRuntime));
    newRuntime.dataprocConfig.masterDiskSize =
      newRuntime.dataprocConfig.masterDiskSize + 20;

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    await waitFor(() => {
      expect(updateRuntimeSpy).toHaveBeenCalled();
    });
    expect(deleteRuntimeSpy).not.toHaveBeenCalled();
  });
});
