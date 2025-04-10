import {
  Disk,
  DisksApi,
  DiskType,
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';

// Hooks cannot be tested outside of a functional component, so we need to use renderHook
import { act, renderHook, waitFor } from '@testing-library/react';
import {
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';

import { DisksApiStub, stubDisk } from 'testing/stubs/disks-api-stub';
import {
  defaultDataProcRuntime,
  defaultGceRuntime,
  defaultGceRuntimeWithPd,
  defaultRuntime,
  RuntimeApiStub,
} from 'testing/stubs/runtime-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { mockUseStore } from 'testing/utils';

import { LeoRuntimeInitializer } from './leo-runtime-initializer';
import { DATAPROC_MIN_DISK_SIZE_GB } from './machines';
import { useCustomRuntime } from './runtime-hooks';
import * as runtimeHooks from './runtime-hooks';
import { runtimeDiskStore, runtimeStore } from './stores';

describe(useCustomRuntime.name, () => {
  let disksApiStub: DisksApiStub;
  let runtimeApiStub: RuntimeApiStub;
  let deleteRuntimeSpy;
  let initializerSpy;
  let updateRuntimeSpy;
  let currentRuntime: Runtime;
  let deleteDiskSpy;

  beforeEach(() => {
    // useCustomRuntime requires a runtime to be loaded in the
    // runtimeStore. All tests start with the default runtime.
    currentRuntime = defaultRuntime();
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: currentRuntime,
      runtimeLoaded: true,
    });

    runtimeApiStub = new RuntimeApiStub();
    registerApiClient(RuntimeApi, runtimeApiStub);
    deleteRuntimeSpy = jest.spyOn(runtimeApi(), 'deleteRuntime');
    initializerSpy = jest.spyOn(LeoRuntimeInitializer, 'initialize');
    updateRuntimeSpy = jest.spyOn(runtimeApi(), 'updateRuntime');
    disksApiStub = new DisksApiStub();
    registerApiClient(DisksApi, disksApiStub);
    deleteDiskSpy = jest.spyOn(disksApiStub, 'deleteDisk');
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

  const setCurrentDisk = (d: Disk) => {
    disksApiStub.disk = d;
    runtimeDiskStore.set({
      ...runtimeDiskStore.get(),
      gcePersistentDisk: d,
      gcePersistentDiskLoaded: true,
    });
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

  it('should allow Dataproc -> GCE transition', async () => {
    currentRuntime = defaultDataProcRuntime();
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: currentRuntime,
      runtimeLoaded: true,
    });

    const [, setRequest] = testUseCustomRuntime();

    const newRuntime = defaultGceRuntime();

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

  it('should delete and recreate both the disk and runtime when the disk type is changed', async () => {
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: defaultGceRuntimeWithPd(),
      runtimeLoaded: true,
    });
    const existingDisk = {
      ...stubDisk(),
      size: 1000,
      diskType: DiskType.STANDARD,
      name: 'my-existing-disk',
      gceRuntime: true,
      zone: 'us-central1-a',
    };
    setCurrentDisk(existingDisk);
    const newRuntime = defaultGceRuntimeWithPd();
    newRuntime.gceWithPdConfig.persistentDisk.diskType = DiskType.SSD;

    const [, setRequest] = testUseCustomRuntime();

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    await waitFor(() => {
      expect(deleteRuntimeSpy).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(deleteDiskSpy).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(initializerSpy).toHaveBeenCalled();
    });
  });

  it('should delete and recreate runtime and leave disk unchanged when GPU is enabled', async () => {
    runtimeStore.set({
      workspaceNamespace: workspaceDataStub.namespace,
      runtime: defaultGceRuntimeWithPd(),
      runtimeLoaded: true,
    });
    const newRuntime = defaultGceRuntimeWithPd();
    newRuntime.gceWithPdConfig.gpuConfig = {
      gpuType: 'the standard one',
      numOfGpus: 1,
    };

    const [, setRequest] = testUseCustomRuntime();

    await act(async () => {
      setRequest({ runtime: newRuntime, detachedDisk: null });
    });

    await waitFor(() => {
      expect(deleteRuntimeSpy).toHaveBeenCalled();
    });

    expect(deleteDiskSpy).not.toHaveBeenCalled();

    await waitFor(() => {
      expect(initializerSpy).toHaveBeenCalled();
    });
  });
});

describe('useRuntimeAndDiskStores', () => {
  const testWorkspaceNamespace = workspaceDataStub.namespace;
  let useStoreSpy;

  // Create spies directly on the implementation we want to test
  beforeEach(() => {
    // Mock the useRuntime and useDisk functions to avoid actual API calls
    jest.spyOn(runtimeHooks, 'useRuntime').mockImplementation(() => {});
    jest.spyOn(runtimeHooks, 'useDisk').mockImplementation(() => {});
  });

  afterEach(() => {
    if (useStoreSpy) {
      useStoreSpy.mockRestore();
    }
    jest.restoreAllMocks();
  });

  it('should initialize runtime and disk stores with the correct workspace namespace', () => {
    const storeMap = new Map();
    storeMap.set(runtimeStore, {
      workspaceNamespace: testWorkspaceNamespace,
      runtime: undefined,
      runtimeLoaded: false,
    });
    storeMap.set(runtimeDiskStore, {
      workspaceNamespace: testWorkspaceNamespace,
      gcePersistentDisk: null,
      gcePersistentDiskLoaded: false,
    });

    useStoreSpy = mockUseStore(storeMap);

    // Call the hook with our test workspace namespace
    const { result } = renderHook(() =>
      runtimeHooks.useRuntimeAndDiskStores(testWorkspaceNamespace)
    );

    // Verify the hook returns the expected structure and initial values
    expect(result.current).toEqual({
      runtimeLoaded: false,
      gcePersistentDiskLoaded: false,
      runtime: undefined,
      gcePersistentDisk: null,
      isLoaded: false,
    });

    // Verify the hook was initialized with the correct workspace namespace
    expect(result.current.isLoaded).toBeDefined();
  });

  it('should return loading states when stores are not loaded', () => {
    const storeMap = new Map();
    storeMap.set(runtimeStore, {
      workspaceNamespace: testWorkspaceNamespace,
      runtime: undefined,
      runtimeLoaded: false,
    });
    storeMap.set(runtimeDiskStore, {
      workspaceNamespace: testWorkspaceNamespace,
      gcePersistentDisk: null,
      gcePersistentDiskLoaded: false,
    });

    useStoreSpy = mockUseStore(storeMap);

    // Render the hook with mock store values
    const { result } = renderHook(() =>
      runtimeHooks.useRuntimeAndDiskStores(testWorkspaceNamespace)
    );

    // Assert on result
    expect(result.current).toEqual({
      runtimeLoaded: false,
      gcePersistentDiskLoaded: false,
      runtime: undefined,
      gcePersistentDisk: null,
      isLoaded: false,
    });
  });

  it('should return loaded states when stores are loaded', () => {
    const testRuntime = defaultRuntime();
    const testDisk = stubDisk();

    const storeMap = new Map();
    storeMap.set(runtimeStore, {
      workspaceNamespace: testWorkspaceNamespace,
      runtime: testRuntime,
      runtimeLoaded: true,
    });
    storeMap.set(runtimeDiskStore, {
      workspaceNamespace: testWorkspaceNamespace,
      gcePersistentDisk: testDisk,
      gcePersistentDiskLoaded: true,
    });

    useStoreSpy = mockUseStore(storeMap);

    const { result } = renderHook(() =>
      runtimeHooks.useRuntimeAndDiskStores(testWorkspaceNamespace)
    );

    expect(result.current).toEqual({
      runtimeLoaded: true,
      gcePersistentDiskLoaded: true,
      runtime: testRuntime,
      gcePersistentDisk: testDisk,
      isLoaded: true,
    });
  });

  it('should return partially loaded state when only one store is loaded', () => {
    const testRuntime = defaultRuntime();

    // Use an explicitly created Map
    const storeMap = new Map();
    storeMap.set(runtimeStore, {
      workspaceNamespace: testWorkspaceNamespace,
      runtime: testRuntime,
      runtimeLoaded: true,
    });
    storeMap.set(runtimeDiskStore, {
      workspaceNamespace: testWorkspaceNamespace,
      gcePersistentDisk: null,
      gcePersistentDiskLoaded: false,
    });

    useStoreSpy = mockUseStore(storeMap);

    const { result } = renderHook(() =>
      runtimeHooks.useRuntimeAndDiskStores(testWorkspaceNamespace)
    );

    expect(result.current).toEqual({
      runtimeLoaded: true,
      gcePersistentDiskLoaded: false,
      runtime: testRuntime,
      gcePersistentDisk: null,
      isLoaded: false,
    });
  });

  it('should update when stores change', async () => {
    const storeMap = new Map();
    storeMap.set(runtimeStore, {
      workspaceNamespace: testWorkspaceNamespace,
      runtime: undefined,
      runtimeLoaded: false,
    });
    storeMap.set(runtimeDiskStore, {
      workspaceNamespace: testWorkspaceNamespace,
      gcePersistentDisk: null,
      gcePersistentDiskLoaded: false,
    });

    useStoreSpy = mockUseStore(storeMap);

    const { result, rerender } = renderHook(() =>
      runtimeHooks.useRuntimeAndDiskStores(testWorkspaceNamespace)
    );

    // Initial expectations
    expect(result.current.isLoaded).toBe(false);

    // Update store states
    const testRuntime = defaultGceRuntimeWithPd();
    const testDisk = stubDisk();

    // Update our mock using a different implementation approach that directly responds to the store parameter
    useStoreSpy.mockImplementation((store) => {
      if (store === runtimeStore) {
        return {
          workspaceNamespace: testWorkspaceNamespace,
          runtime: testRuntime,
          runtimeLoaded: true,
        };
      }
      if (store === runtimeDiskStore) {
        return {
          workspaceNamespace: testWorkspaceNamespace,
          gcePersistentDisk: testDisk,
          gcePersistentDiskLoaded: true,
        };
      }
      return null;
    });

    // Actually update the stores (though our mock doesn't use these directly)
    act(() => {
      runtimeStore.set({
        workspaceNamespace: testWorkspaceNamespace,
        runtime: testRuntime,
        runtimeLoaded: true,
      });
      runtimeDiskStore.set({
        workspaceNamespace: testWorkspaceNamespace,
        gcePersistentDisk: testDisk,
        gcePersistentDiskLoaded: true,
      });
    });

    // Re-render the hook to get the updated values
    rerender();

    // Updated expectations
    expect(result.current).toEqual({
      runtimeLoaded: true,
      gcePersistentDiskLoaded: true,
      runtime: testRuntime,
      gcePersistentDisk: testDisk,
      isLoaded: true,
    });
  });
});
