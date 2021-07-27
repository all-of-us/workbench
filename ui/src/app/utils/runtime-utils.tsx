import {leoRuntimesApi} from 'app/services/notebooks-swagger-fetch-clients';
import {disksApi, runtimeApi} from 'app/services/swagger-fetch-clients';
import {switchCase, withAsyncErrorHandling} from 'app/utils';
import {ExceededActionCountError, LeoRuntimeInitializationAbortedError, LeoRuntimeInitializer, } from 'app/utils/leo-runtime-initializer';
import {ComputeType, findMachineByName, Machine} from 'app/utils/machines';
import {
  compoundRuntimeOpStore,
  diskStore,
  markCompoundRuntimeOperationCompleted,
  registerCompoundRuntimeOperation,
  runtimeStore,
  useStore
} from 'app/utils/stores';

import {DataprocConfig, DiskType, Runtime, RuntimeConfigurationType, RuntimeStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {runtimePresets} from "./runtime-presets";

const {useState, useEffect} = React;

export enum RuntimeStatusRequest {
  Delete = 'Delete',
  Start = 'Start',
  Stop = 'Stop'
}

export interface RuntimeDiff {
  desc: string;
  previous: string;
  new: string;
  differenceType: RuntimeDiffState;
}

export enum RuntimeDiffState {
  NO_CHANGE,
  CAN_UPDATE,
  NEEDS_DELETE
}

export interface RuntimeConfig {
  computeType: ComputeType;
  machine: Machine;
  diskSize: number;
  dataprocConfig: DataprocConfig;
}

const compareComputeTypes = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  console.log("compareComputeTypes: ",oldRuntime,newRuntime);
  return {
    desc: 'Change compute type',
    previous: oldRuntime.computeType,
    new: newRuntime.computeType,
    differenceType: oldRuntime.computeType === newRuntime.computeType ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
};

const compareMachineCpu = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  const oldCpu = oldRuntime.machine.cpu;
  const newCpu = newRuntime.machine.cpu;

  return {
    desc: (newCpu < oldCpu ?  'Decrease' : 'Increase') + ' number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    differenceType: oldCpu === newCpu ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.CAN_UPDATE
  };
};

const compareMachineMemory = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  const oldMemory = oldRuntime.machine.memory;
  const newMemory = newRuntime.machine.memory;

  return {
    desc: (newMemory < oldMemory ?  'Decrease' : 'Increase') + ' memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    differenceType: oldMemory === newMemory ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.CAN_UPDATE
  };
};

const compareDiskSize = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
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
    previous: oldRuntime.diskSize && oldRuntime.diskSize.toString() + ' GB',
    new: newRuntime.diskSize && newRuntime.diskSize.toString() + ' GB',
    differenceType: diffType
  };
};

const compareWorkerCpu = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldCpu = findMachineByName(oldRuntime.dataprocConfig.workerMachineType).cpu;
  const newCpu = findMachineByName(newRuntime.dataprocConfig.workerMachineType).cpu;

  return {
    desc: (newCpu < oldCpu ?  'Decrease' : 'Increase') + ' number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    differenceType: oldCpu === newCpu ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
};

const compareWorkerMemory = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldMemory = findMachineByName(oldRuntime.dataprocConfig.workerMachineType).memory;
  const newMemory = findMachineByName(newRuntime.dataprocConfig.workerMachineType).memory;

  return {
    desc: (newMemory < oldMemory ?  'Decrease' : 'Increase') + ' memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    differenceType: oldMemory === newMemory ? RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
};

const compareDataprocWorkerDiskSize = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldDiskSize = oldRuntime.dataprocConfig.workerDiskSize || 0;
  const newDiskSize = newRuntime.dataprocConfig.workerDiskSize || 0;

  return {
    desc: (newDiskSize < oldDiskSize ?  'Decrease' : 'Increase') + ' worker disk size',
    previous: oldDiskSize.toString() + ' GB',
    new: newDiskSize.toString() + ' GB',
    differenceType: oldDiskSize === newDiskSize ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.NEEDS_DELETE
  };
};

const compareDataprocNumberOfPreemptibleWorkers = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfPreemptibleWorkers || 0;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfPreemptibleWorkers || 0;

  return {
    desc: (newNumWorkers < oldNumWorkers ?  'Decrease' : 'Increase') + ' number of preemptible workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType: oldNumWorkers === newNumWorkers ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.CAN_UPDATE
  };
};

const compareDataprocNumberOfWorkers = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfWorkers || 0;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfWorkers || 0;

  return {
    desc: (newNumWorkers < oldNumWorkers ?  'Decrease' : 'Increase') + ' number of workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType: oldNumWorkers === newNumWorkers ?
      RuntimeDiffState.NO_CHANGE : RuntimeDiffState.CAN_UPDATE
  };
};

const toRuntimeConfig = (runtime: Runtime): RuntimeConfig => {
  if (runtime.gceConfig) {
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(runtime.gceConfig.machineType),
      diskSize: runtime.gceConfig.diskSize,
      dataprocConfig: null
    };
  } else if(runtime.gceWithPdConfig){
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(runtime.gceWithPdConfig.machineType),
      diskSize: runtime.gceWithPdConfig.persistentDisk.size,
      dataprocConfig: null
    };
  }
  else if (runtime.dataprocConfig) {
    return {
      computeType: ComputeType.Dataproc,
      machine: findMachineByName(runtime.dataprocConfig.masterMachineType),
      diskSize: runtime.dataprocConfig.masterDiskSize,
      dataprocConfig: runtime.dataprocConfig
    };
  }
};

export const getRuntimeConfigDiffs = (oldRuntime: RuntimeConfig, newRuntime: RuntimeConfig): RuntimeDiff[] => {
  console.log("print getRuntimeConfigDiffs",oldRuntime, newRuntime);
  return [compareWorkerCpu, compareWorkerMemory, compareDataprocWorkerDiskSize,
    compareDataprocNumberOfPreemptibleWorkers, compareDataprocNumberOfWorkers,
    compareComputeTypes, compareMachineCpu, compareMachineMemory, compareDiskSize]
    .map(compareFn => compareFn(oldRuntime, newRuntime))
    .filter(diff => diff !== null)
    .filter(diff => diff.differenceType !== RuntimeDiffState.NO_CHANGE);
};

const getRuntimeDiffs = (oldRuntime: Runtime, newRuntime: Runtime): RuntimeDiff[] => {
  return getRuntimeConfigDiffs(toRuntimeConfig(oldRuntime), toRuntimeConfig(newRuntime));
};

// useRuntime hook is a simple hook to populate the runtime store.
// This is only used by other runtime hooks
const useRuntime = (currentWorkspaceNamespace) => {
  // No cleanup is being handled at the moment.
  // When the user initiates a runtime change we want that change to take place even if they navigate away
  useEffect(() => {
    if (!currentWorkspaceNamespace) {
      return;
    }

    const getRuntime = withAsyncErrorHandling(
      () => runtimeStore.set({workspaceNamespace: null, runtime: null}),
      async() => {
        let leoRuntime;
        try {
          leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
        } catch (e) {
          if (!(e instanceof Response && e.status === 404)) {
            throw e;
          }
          // null on the runtime store indicates no existing runtime
          leoRuntime = null;
        }
        if (currentWorkspaceNamespace === runtimeStore.get().workspaceNamespace) {
          runtimeStore.set({
            workspaceNamespace: currentWorkspaceNamespace,
            runtime: leoRuntime
          });
        }
      });
    getRuntime();
  }, [currentWorkspaceNamespace]);
};

// useDisk hook is a simple hook to populate the disk store.
// This is only used by other disk hooks
export const useDisk = (currentWorkspaceNamespace) => {
  // No cleanup is being handled at the moment.
  // When the user initiates a runtime change we want that change to take place even if they navigate away
  console.log("useDisk",currentWorkspaceNamespace)
  useEffect(() => {
    if (!currentWorkspaceNamespace) {
      return;
    }
    console.log("useDisk",1)
    const getDisk = withAsyncErrorHandling(
        () => diskStore.set({workspaceNamespace: null, disk: null}),
        async() => {
          let pd;
          try {
            pd = await disksApi().getDisk(currentWorkspaceNamespace);
            console.log("disksApi().getDisk",pd)
          } catch (e) {
            if (!(e instanceof Response && e.status === 404)) {
              throw e;
            }
            // null on the runtime store indicates no existing runtime
            pd = null;
          }
          console.log("useDisk 4",currentWorkspaceNamespace,diskStore.get().workspaceNamespace )
          if (currentWorkspaceNamespace === diskStore.get().workspaceNamespace) {
            console.log("useDisk 3",pd)
            diskStore.set({
              workspaceNamespace: currentWorkspaceNamespace,
              disk: pd
            });
          }
        });
    getDisk();
    console.log("useDisk",2)
  }, [currentWorkspaceNamespace]);
};

export const maybeInitializeRuntime = async(workspaceNamespace: string, signal: AbortSignal): Promise<Runtime> => {
  if (workspaceNamespace in compoundRuntimeOpStore.get()) {
    await new Promise<void>((resolve, reject) => {
      signal.addEventListener('abort', reject);
      const {unsubscribe} = compoundRuntimeOpStore.subscribe((v => {
        if (!(workspaceNamespace in v)) {
          unsubscribe();
          signal.removeEventListener('abort', reject);
          resolve();
        }
      }));
    });
  }

  return await LeoRuntimeInitializer.initialize({workspaceNamespace, pollAbortSignal: signal});
};

// useRuntimeStatus hook can be used to change the status of the runtime
// This setter returns a promise which resolves when any proximal fetch has completed,
// but does not wait for any polling, which may continue asynchronously.
export const useRuntimeStatus = (currentWorkspaceNamespace, currentGoogleProject, deleteDisk): [
  RuntimeStatus | undefined, (statusRequest: RuntimeStatusRequest) => Promise<void>]  => {
  const [runtimeStatus, setRuntimeStatus] = useState<RuntimeStatusRequest>();
  const {runtime} = useStore(runtimeStore);

  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    // Additional status changes can be put here
    const resolutionCondition: (r: Runtime) => boolean = switchCase(runtimeStatus,
        [RuntimeStatusRequest.Delete, () => (r) => r === null || r.status === RuntimeStatus.Deleted],
        [RuntimeStatusRequest.Start, () => (r) => r.status === RuntimeStatus.Running],
        [RuntimeStatusRequest.Stop, () => (r) => r.status === RuntimeStatus.Stopped]
    );
    const initializePolling = async() => {
      if (!!runtimeStatus) {
        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: currentWorkspaceNamespace,
            maxCreateCount: 0,
            maxDeleteCount: 0,
            resolutionCondition: (r) => resolutionCondition(r)
          });
        } catch (e) {
          // ExceededActionCountError is expected, as we exceed our create limit of 0.
          if (!(e instanceof ExceededActionCountError ||
              e instanceof LeoRuntimeInitializationAbortedError)) {
            throw e;
          }
        }
      }
    };
    initializePolling();
  }, [runtimeStatus]);

  const setStatusRequest = async(req) => {
    await switchCase(req,
      [RuntimeStatusRequest.Delete, () => {
        return runtimeApi().deleteRuntime(currentWorkspaceNamespace, deleteDisk);
      }],
      [RuntimeStatusRequest.Start, () => {
        return leoRuntimesApi().startRuntime(currentGoogleProject, runtime.runtimeName);
      }],
      [RuntimeStatusRequest.Stop, () => {
        return leoRuntimesApi().stopRuntime(currentGoogleProject, runtime.runtimeName);
      }]
    );
    setRuntimeStatus(req);
  };
  return [runtime ? runtime.status : undefined, setStatusRequest];
};

const synchronousRecreate = async(runtime, requestedRuntime, workspaceNamespace, aborter ) => {
  if (runtime.diskConfig.size > requestedRuntime.gceConfig.diskSize) {
    await runtimeApi().deleteRuntime(workspaceNamespace, true);
    await LeoRuntimeInitializer.initialize({
      workspaceNamespace,
      targetRuntime: requestedRuntime,
      resolutionCondition: r => r.status === RuntimeStatus.Deleted,
      pollAbortSignal: aborter.signal,
      overallTimeout: 10000 * 300 // The switch to a non running status should occur quickly
    });

    const runtimeRequest: Runtime = {
      gceWithPdConfig: {
        bootDiskSize: 50,
        machineType: requestedRuntime.gceConfig.machineType,
        persistentDisk: {
          name: 'pd',
          size: requestedRuntime.gceConfig.diskSize,
          diskType: DiskType.Standard,
          labels: {}

        }
      }
      // gceConfig: {
      //   machineType: runtime.machine.name,
      //   diskSize: runtime.diskSize
      // }
    };

    // If the selected runtime matches a preset, plumb through the appropriate configuration type.
    runtimeRequest.configurationType = RuntimeConfigurationType.UserOverride;
    console.log('up createRuntimeRequest: ', runtimeRequest);
    await runtimeApi().createRuntime(workspaceNamespace, runtimeRequest);

  } else {
    await runtimeApi().updateRuntime(workspaceNamespace, {runtime: requestedRuntime});
  }
};

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useCustomRuntime = (currentWorkspaceNamespace):
    [{currentRuntime: Runtime, pendingRuntime: Runtime}, (runtime: Runtime) => void] => {
  const {runtime, workspaceNamespace} = useStore(runtimeStore);
  const runtimeOps = useStore(compoundRuntimeOpStore);
  const {pendingRuntime = null} = runtimeOps[currentWorkspaceNamespace] || {};
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime>();


  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);

  console.log("print runtime", runtime);

  useEffect(() => {
    let mounted = true;
    const aborter = new AbortController();
    const runAction = async() => {
      // Only delete if the runtime already exists.
      // TODO: It is likely more correct here to use the LeoRuntimeInitializer wait for the runtime
      // to reach a terminal status before attempting deletion.
      try {
        if (runtime) {
          const runtimeDiffTypes = getRuntimeDiffs(runtime, requestedRuntime).map(diff => diff.differenceType);
          console.log('use effect stage: ', runtime);
          if (runtimeDiffTypes.includes(RuntimeDiffState.NEEDS_DELETE)) {
            if (runtime.status !== RuntimeStatus.Deleted) {
              await runtimeApi().deleteRuntime(currentWorkspaceNamespace, false, {
                signal: aborter.signal
              });
            }
          } else if (runtimeDiffTypes.includes(RuntimeDiffState.CAN_UPDATE)) {
            if (runtime.status === RuntimeStatus.Running || runtime.status === RuntimeStatus.Stopped) {
              console.log('update stage: ', runtime.diskConfig, requestedRuntime);
              await synchronousRecreate(runtime, requestedRuntime, currentWorkspaceNamespace, aborter);
              // Calling updateRuntime will not immediately set the Runtime status to not Running so the
              // default initializer will resolve on its first call. The polling below first checks for the
              // non Running status before initializing the default one that checks for Running status
              await LeoRuntimeInitializer.initialize({
                workspaceNamespace,
                targetRuntime: requestedRuntime,
                resolutionCondition: r => r.status !== RuntimeStatus.Running,
                pollAbortSignal: aborter.signal,
                overallTimeout: 1000 * 60 // The switch to a non running status should occur quickly
              });
            } else if (runtime.status === RuntimeStatus.Deleted) {

              console.log('only PD createRuntimeRequest step 1: ', requestedRuntime);

              await runtimeApi().createRuntime(currentWorkspaceNamespace, requestedRuntime);
              await LeoRuntimeInitializer.initialize({
                workspaceNamespace,
                targetRuntime: requestedRuntime,
                resolutionCondition: r => r.status === RuntimeStatus.Running,
                pollAbortSignal: aborter.signal,
                overallTimeout: 10000 * 300 // The switch to a non running status should occur quickly
              });
              console.log('only PD createRuntimeRequest step 2: ');
              await synchronousRecreate(runtime,requestedRuntime,currentWorkspaceNamespace,aborter);

            }
          } else {
            // There are no differences, no extra requests needed
          }
        }

        await LeoRuntimeInitializer.initialize({
          workspaceNamespace,
          targetRuntime: requestedRuntime,
          pollAbortSignal: aborter.signal
        });
      } catch (e) {
        if (!(e instanceof LeoRuntimeInitializationAbortedError)) {
          throw e;
        }
      } finally {
        markCompoundRuntimeOperationCompleted(currentWorkspaceNamespace);
        if (mounted) {
          setRequestedRuntime(undefined);
        }
      }
    };

    if (requestedRuntime !== undefined && !fp.equals(requestedRuntime, runtime)) {
      registerCompoundRuntimeOperation(currentWorkspaceNamespace, {
        pendingRuntime: requestedRuntime,
        aborter
      });
      runAction();
    }

    // After dismount, we still want the above store modifications to occur.
    // However, we should not continue to mutate the now unmounted hook state -
    // this will result in React warnings.
    return () => { mounted = false; };
  }, [requestedRuntime]);

  return [{currentRuntime: runtime, pendingRuntime}, setRequestedRuntime];
};

export const withRuntimeStore = () => WrappedComponent => {
  return (props) => {
    const value = useStore(runtimeStore);

    // Ensure that a runtime gets initialized, if it hasn't already been.
    useRuntime(value.workspaceNamespace);

    return <WrappedComponent {...props} runtimeStore={value} />;
  };
};

