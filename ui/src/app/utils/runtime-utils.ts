import {runtimeApi} from 'app/services/swagger-fetch-clients';
import {switchCase, withAsyncErrorHandling} from 'app/utils';
import {ExceededActionCountError, LeoRuntimeInitializer,} from 'app/utils/leo-runtime-initializer';
import {runtimeStore, useStore} from 'app/utils/stores';
import {DataprocConfig, Runtime, RuntimeStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';

import * as React from 'react';
import {ComputeType, findMachineByName, Machine} from './machines';

const {useState, useEffect} = React;

export enum RuntimeStatusRequest {
  Delete = 'Delete'
}

interface RuntimeDiff {
  desc: string;
  previous: string;
  new: string;
  differenceType: RuntimeDiffState;
}

enum RuntimeDiffState {
  NO_CHANGE,
  CAN_UPDATE,
  NEEDS_DELETE
}

interface RuntimeConfig {
  computeType: ComputeType;
  machine: Machine;
  diskSize: number;
  dataprocConfig: DataprocConfig;
}

function compareComputeTypes(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  return {
    desc: 'Change Compute Type',
    previous: oldRuntime.computeType,
    new: newRuntime.computeType,
    differenceType: oldRuntime.computeType === newRuntime.computeType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareMachineCpu(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  const oldCpu = oldRuntime.machine.cpu;
  const newCpu = newRuntime.machine.cpu;

  return {
    desc: (newCpu < oldCpu ?  'Decrease' : 'Increase') + ' Number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    differenceType: oldCpu === newCpu ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareMachineMemory(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  const oldMemory = oldRuntime.machine.memory;
  const newMemory = newRuntime.machine.memory;

  return {
    desc: (newMemory < oldMemory ?  'Decrease' : 'Increase') + ' Memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    differenceType: oldMemory === newMemory ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDiskSize(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  let desc = 'Disk Size';
  let diffType;

  if (newRuntime.diskSize < oldRuntime.diskSize) {
    desc = 'Decease ' + desc;
    diffType = RuntimeDiffState.NEEDS_DELETE;
  } else if (newRuntime.diskSize > oldRuntime.diskSize) {
    desc = 'Increase ' + desc;
    diffType = RuntimeDiffState.CAN_UPDATE;
  } else {
    diffType = RuntimeDiffState.NO_CHANGE;
  }

  return {
    desc: desc,
    previous: oldRuntime.diskSize.toString() + ' GB',
    new: newRuntime.diskSize.toString() + ' GB',
    differenceType: diffType
  };
}

function compareDataprocMasterDiskSize(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  let desc = 'Dataproc Master Machine Disk Size';
  let diffType;

  if (newRuntime.dataprocConfig.masterDiskSize < oldRuntime.dataprocConfig.masterDiskSize) {
    desc = 'Decease ' + desc;
    diffType = RuntimeDiffState.NEEDS_DELETE;
  } else if (newRuntime.dataprocConfig.masterDiskSize > oldRuntime.dataprocConfig.masterDiskSize) {
    desc = 'Increase ' + desc;
    diffType = RuntimeDiffState.CAN_UPDATE;
  } else {
    diffType = RuntimeDiffState.NO_CHANGE;
  }

  return {
    desc: desc,
    previous: oldRuntime.dataprocConfig.masterDiskSize.toString() + ' GB',
    new: newRuntime.dataprocConfig.masterDiskSize.toString() + ' GB',
    differenceType: diffType
  };
}

function compareDataprocMasterMachineType(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  return {
    desc: 'Change Master Machine Type',
    previous: oldRuntime.dataprocConfig.masterMachineType,
    new: newRuntime.dataprocConfig.masterMachineType,
    differenceType: oldRuntime.dataprocConfig.masterMachineType === newRuntime.dataprocConfig.masterMachineType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocWorkerMachineType(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  return {
    desc: 'Change Worker Machine Type',
    previous: oldRuntime.dataprocConfig.workerMachineType,
    new: newRuntime.dataprocConfig.workerMachineType,
    differenceType: oldRuntime.dataprocConfig.workerMachineType === newRuntime.dataprocConfig.workerMachineType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocWorkerDiskSize(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  const oldDiskSize = oldRuntime.dataprocConfig.workerDiskSize || 0;
  const newDiskSize = newRuntime.dataprocConfig.workerDiskSize || 0;

  return {
    desc: (newDiskSize < oldDiskSize ?  'Decrease' : 'Increase') + ' Change Worker Machine Type',
    previous: oldDiskSize.toString() + ' GB',
    new: newDiskSize.toString() + ' GB',
    differenceType: oldDiskSize === newDiskSize ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
}

function compareDataprocNumberOfPreemptibleWorkers(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfPreemptibleWorkers || 0;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfPreemptibleWorkers || 0;

  return {
    desc: (newNumWorkers < oldNumWorkers ?  'Decrease' : 'Increase') + ' Number of Preemptible Workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType: oldNumWorkers === newNumWorkers ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.CAN_UPDATE
  };
}

function compareDataprocNumberOfWorkers(oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff {
  if (oldRuntime.dataprocConfig === null || newRuntime.dataprocConfig === null) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfWorkers || 0;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfWorkers || 0;

  return {
    desc: (newNumWorkers < oldNumWorkers ?  'Decrease' : 'Increase') + ' Number of Workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType: oldNumWorkers === newNumWorkers ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.CAN_UPDATE
  };
}

export const getRuntimeConfigDiffs = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff[] => {
  const compareFns = [compareComputeTypes, compareDiskSize, compareMachineCpu,
    compareMachineMemory, compareDataprocMasterDiskSize, compareDataprocMasterMachineType,
    compareDataprocNumberOfPreemptibleWorkers, compareDataprocNumberOfWorkers,
    compareDataprocWorkerDiskSize, compareDataprocWorkerMachineType];

  return compareFns.map(compareFn => compareFn(oldRuntime, newRuntime)).filter(diff => diff !== null);
};

function getRuntimeDiffs(oldRuntime: Runtime, newRuntime: Runtime): RuntimeDiff[] {
  return getRuntimeConfigDiffs(toRuntimeConfig(oldRuntime), toRuntimeConfig(newRuntime));
}

function toRuntimeConfig(runtime: Runtime): RuntimeConfig {
  if (runtime.gceConfig) {
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(runtime.gceConfig.machineType),
      diskSize: runtime.gceConfig.diskSize,
      dataprocConfig: null
    };
  } else if (runtime.dataprocConfig) {
    return {
      computeType: ComputeType.Dataproc,
      machine: findMachineByName(runtime.dataprocConfig.masterMachineType),
      diskSize: runtime.dataprocConfig.masterDiskSize,
      dataprocConfig: runtime.dataprocConfig
    };
  }
}

// useRuntime hook is a simple hook to populate the runtime store.
// This is only used by other runtime hooks
const useRuntime = (currentWorkspaceNamespace) => {
  // No cleanup is being handled at the moment.
  // When the user initiates a runtime change we want that change to take place even if they navigate away
  useEffect(() => {
    const getRuntime = withAsyncErrorHandling(
      () => runtimeStore.set({workspaceNamespace: null, runtime: null}),
      async() => {
        const leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
        if (currentWorkspaceNamespace === runtimeStore.get().workspaceNamespace) {
          runtimeStore.set({
            workspaceNamespace: currentWorkspaceNamespace,
            runtime: leoRuntime
          });
        }
      });

    if (currentWorkspaceNamespace !== runtimeStore.get().workspaceNamespace) {
      runtimeStore.set({workspaceNamespace: currentWorkspaceNamespace, runtime: undefined});
      getRuntime();
    }
  }, []);
};

// useRuntimeStatus hook can be used to change the status of the runtime
// Only 'Delete' is supported at the moment. This setter returns a promise which
// resolves when any proximal fetch has completed, but does not wait for any
// polling, which may continue asynchronously.
export const useRuntimeStatus = (currentWorkspaceNamespace): [
  RuntimeStatus | undefined, (statusRequest: RuntimeStatusRequest) => Promise<void>]  => {
  const [runtimeStatus, setRuntimeStatus] = useState<RuntimeStatusRequest>();
  const {runtime} = useStore(runtimeStore);
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    // Additional status changes can be put here
    if (!!runtimeStatus) {
      switchCase(runtimeStatus,
        [RuntimeStatusRequest.Delete, async() => {
          try {
            await LeoRuntimeInitializer.initialize({workspaceNamespace: currentWorkspaceNamespace, maxCreateCount: 0});
          } catch (e) {
            // ExceededActionCountError is expected, as we exceed our create limit of 0.
            if (!(e instanceof ExceededActionCountError)) {
              throw e;
            }
          }
        }]);
    }

  }, [runtimeStatus]);

  const setStatusRequest = async(req) => {
    await switchCase(req, [
      RuntimeStatusRequest.Delete, () => runtimeApi().deleteRuntime(currentWorkspaceNamespace)
    ]);
    setRuntimeStatus(req);
  };
  return [runtime ? runtime.status : undefined, setStatusRequest];
};

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.

export const useCustomRuntime = (currentWorkspaceNamespace): [Runtime, (runtime: Runtime) => void] => {
  const {runtime, workspaceNamespace} = useStore(runtimeStore);
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime>();
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    const runAction = async() => {
      if (runtime) {
        const runtimeDiffTypes = getRuntimeDiffs(runtime, requestedRuntime).map(diff => diff.differenceType);

        if (runtimeDiffTypes.includes(RuntimeDiffState.NEEDS_DELETE)) {
          if (runtime.status !== RuntimeStatus.Deleted) {
            await runtimeApi().deleteRuntime(currentWorkspaceNamespace);
          }
        } else if (runtimeDiffTypes.includes(RuntimeDiffState.CAN_UPDATE)) {
          // TODO eric: what happens if we get can update request during a non running/stopped state?
          if (runtime.status === RuntimeStatus.Running || runtime.status === RuntimeStatus.Stopped) {
            await runtimeApi().updateRuntime(currentWorkspaceNamespace, {runtime: requestedRuntime});
            // Calling updateRuntime will not immediately set the Runtime status to not Running so the
            // default initializer will resolve on its first call. The polling below first checks for the
            // non Running status before initializing the default one that checks for Running status
            await LeoRuntimeInitializer.initialize({
              workspaceNamespace,
              targetRuntime: requestedRuntime,
              resolutionCond: (runtime: Runtime) => runtime.status !== RuntimeStatus.Running
            });
          }
        } else {
          // There are no differences, no extra requests needed
        }
      }

      await LeoRuntimeInitializer.initialize({
        workspaceNamespace,
        targetRuntime: requestedRuntime
      });
    };

    if (requestedRuntime !== undefined && !fp.equals(requestedRuntime, runtime)) {
      runAction();
    }
  }, [requestedRuntime]);

  return [runtime, setRequestedRuntime];
};

