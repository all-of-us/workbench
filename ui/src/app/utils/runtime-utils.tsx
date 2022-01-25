import { leoRuntimesApi } from 'app/services/notebooks-swagger-fetch-clients';
import { disksApi, runtimeApi } from 'app/services/swagger-fetch-clients';
import { DEFAULT, switchCase, withAsyncErrorHandling } from 'app/utils';
import {
  ExceededActionCountError,
  ExceededErrorCountError,
  LeoRuntimeInitializationAbortedError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_DISK_SIZE,
  DEFAULT_MACHINE_TYPE,
  findMachineByName,
  Machine,
} from 'app/utils/machines';
import {
  compoundRuntimeOpStore,
  diskStore,
  markCompoundRuntimeOperationCompleted,
  registerCompoundRuntimeOperation,
  runtimeStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';

import {
  DataprocConfig,
  Disk,
  DiskType,
  ErrorCode,
  GpuConfig,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus,
  SecuritySuspendedErrorParameters,
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import { runtimePresets } from './runtime-presets';

const { useState, useEffect } = React;

export class ComputeSecuritySuspendedError extends Error {
  constructor(public params: SecuritySuspendedErrorParameters) {
    super('user is suspended from compute');
    // See https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
    Object.setPrototypeOf(this, ComputeSecuritySuspendedError.prototype);

    this.name = 'ComputeSecuritySuspendedError';
  }
}

export enum RuntimeStatusRequest {
  DeleteRuntime = 'DeleteRuntime',
  DeleteRuntimeAndPD = 'DeleteRuntimeAndPD',
  DeletePD = 'DeletePD',
  Start = 'Start',
  Stop = 'Stop',
}

export interface RuntimeDiff {
  desc: string;
  previous: string;
  new: string;
  differenceType: RuntimeDiffState;
}

export enum RuntimeDiffState {
  // Arranged in order of increasing severity. We depend on this ordering below.
  NO_CHANGE,
  CAN_UPDATE_IN_PLACE,
  CAN_UPDATE_WITH_REBOOT,
  NEEDS_DELETE_RUNTIME,
  NEEDS_DELETE_PD,
}

export interface RuntimeConfig {
  computeType: ComputeType;
  machine: Machine;
  detachableDisk: boolean;
  diskSize: number;
  detachableDiskType: DiskType | null;
  // TODO: Refactor this type to an intermediate representation.
  dataprocConfig: DataprocConfig;
  // TODO: Refactor this type to an intermediate representation.
  gpuConfig: GpuConfig;
  autopauseThreshold: number;
}

export interface UpdateMessaging {
  applyAction: string;
  warn?: string;
  warnMore?: string;
}

// Used to wrap the sate of runtime panel
export interface RuntimeCtx {
  runtimeExists: boolean;
  gceExists: boolean;
  dataprocExists: boolean;
  pdExists: boolean;
  enablePD: boolean;
}

const errorToSecuritySuspendedParams = async (
  error
): Promise<SecuritySuspendedErrorParameters> => {
  if (error?.status !== 412) {
    return null;
  }
  const body = await error?.json();
  if (body?.errorCode !== ErrorCode.COMPUTESECURITYSUSPENDED) {
    return null;
  }

  return body?.parameters as SecuritySuspendedErrorParameters;
};

export const maybeUnwrapSecuritySuspendedError = async (
  error: Error
): Promise<Error> => {
  if (error instanceof ExceededErrorCountError) {
    error = error.lastError;
  }
  const suspendedParams = await errorToSecuritySuspendedParams(error);
  if (suspendedParams) {
    return new ComputeSecuritySuspendedError(suspendedParams);
  }
  return error;
};

// Visible for testing only.
export const findMostSevereDiffState = (
  states: RuntimeDiffState[]
): RuntimeDiffState => {
  return fp.last([...states].sort());
};

export const diffsToUpdateMessaging = (
  diffs: RuntimeDiff[]
): UpdateMessaging => {
  const diffType = findMostSevereDiffState(
    diffs.map(({ differenceType }) => differenceType)
  );
  return switchCase(
    diffType,
    [
      RuntimeDiffState.NEEDS_DELETE_PD,
      () => ({
        applyAction: 'APPLY & RECREATE',
        warn: 'Reducing the size of a persistent disk requires it to be deleted and recreated. This will delete all files on the disk.',
        warnMore:
          'If you want to save some files permanently, such as input data, analysis outputs, or installed packages, ' +
          'move them to the workspace bucket. \nNote: Jupyter notebooks are autosaved to the workspace bucket, and deleting ' +
          'your disk will not delete your notebooks.',
      }),
    ],
    [
      RuntimeDiffState.NEEDS_DELETE_RUNTIME,
      () => ({
        applyAction: 'APPLY & RECREATE',
        warn:
          'These changes require deletion and re-creation of your cloud ' +
          'environment to take effect.',
        warnMore:
          'Any in-memory state and local file modifications will be ' +
          'erased. Data stored in workspace buckets is never affected ' +
          'by changes to your cloud environment.',
      }),
    ],
    [
      RuntimeDiffState.CAN_UPDATE_WITH_REBOOT,
      () => ({
        applyAction: 'APPLY & REBOOT',
        warn: 'These changes require a reboot of your cloud environment to take effect.',
        warnMore:
          'Any in-memory state will be erased, but local file ' +
          'modifications will be preserved. Data stored in workspace ' +
          'buckets is never affected by changes to your cloud environment.',
      }),
    ],
    [
      DEFAULT,
      () => ({
        applyAction: 'APPLY',
      }),
    ]
  );
};

const compareComputeTypes = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  return {
    desc: 'Change compute type',
    previous: oldRuntime.computeType,
    new: newRuntime.computeType,
    differenceType:
      oldRuntime.computeType === newRuntime.computeType
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.NEEDS_DELETE_RUNTIME,
  };
};

const compareMachineCpu = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  const oldCpu = oldRuntime.machine.cpu;
  const newCpu = newRuntime.machine.cpu;

  return {
    desc: (newCpu < oldCpu ? 'Decrease' : 'Increase') + ' number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    differenceType:
      oldCpu === newCpu
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.CAN_UPDATE_WITH_REBOOT,
  };
};

const compareMachineMemory = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  const oldMemory = oldRuntime.machine.memory;
  const newMemory = newRuntime.machine.memory;

  return {
    desc: (newMemory < oldMemory ? 'Decrease' : 'Increase') + ' memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    differenceType:
      oldMemory === newMemory
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.CAN_UPDATE_WITH_REBOOT,
  };
};

export const compareGpu = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  const oldGpuExists = !!oldRuntime.gpuConfig;
  const newGpuExists = !!newRuntime.gpuConfig;
  return {
    desc: 'Change GPU config',
    previous: oldGpuExists
      ? `${oldRuntime.gpuConfig.numOfGpus} ${oldRuntime.gpuConfig.gpuType} GPU`
      : 'No GPUs',
    new: newGpuExists
      ? `${newRuntime.gpuConfig.numOfGpus} ${newRuntime.gpuConfig.gpuType} GPU`
      : 'No GPUs',
    differenceType:
      (!oldGpuExists && !newGpuExists) ||
      (oldGpuExists &&
        newGpuExists &&
        oldRuntime.gpuConfig.gpuType === newRuntime.gpuConfig.gpuType &&
        oldRuntime.gpuConfig.numOfGpus === newRuntime.gpuConfig.numOfGpus)
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.NEEDS_DELETE_RUNTIME,
  };
};

const compareDiskSize = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  let desc = 'Disk Size';
  let diffType: RuntimeDiffState;
  if (newRuntime.diskSize < oldRuntime.diskSize) {
    desc = 'Decease ' + desc;
    if (newRuntime.detachableDisk && oldRuntime.detachableDisk) {
      diffType = RuntimeDiffState.NEEDS_DELETE_PD;
    } else {
      diffType = RuntimeDiffState.NEEDS_DELETE_RUNTIME;
    }
  } else if (newRuntime.diskSize > oldRuntime.diskSize) {
    desc = 'Increase ' + desc;
    diffType = RuntimeDiffState.CAN_UPDATE_WITH_REBOOT;
  } else {
    diffType = RuntimeDiffState.NO_CHANGE;
  }

  return {
    desc: desc,
    previous: oldRuntime.diskSize && oldRuntime.diskSize.toString() + ' GB',
    new: newRuntime.diskSize && newRuntime.diskSize.toString() + ' GB',
    differenceType: diffType,
  };
};

const compareWorkerCpu = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldCpu = findMachineByName(
    oldRuntime.dataprocConfig.workerMachineType
  ).cpu;
  const newCpu = findMachineByName(
    newRuntime.dataprocConfig.workerMachineType
  ).cpu;

  return {
    desc: (newCpu < oldCpu ? 'Decrease' : 'Increase') + ' number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    differenceType:
      oldCpu === newCpu
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.NEEDS_DELETE_RUNTIME,
  };
};

const compareWorkerMemory = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldMemory = findMachineByName(
    oldRuntime.dataprocConfig.workerMachineType
  ).memory;
  const newMemory = findMachineByName(
    newRuntime.dataprocConfig.workerMachineType
  ).memory;

  return {
    desc: (newMemory < oldMemory ? 'Decrease' : 'Increase') + ' memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    differenceType:
      oldMemory === newMemory
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.NEEDS_DELETE_RUNTIME,
  };
};

const compareDataprocWorkerDiskSize = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldDiskSize = oldRuntime.dataprocConfig.workerDiskSize || 0;
  const newDiskSize = newRuntime.dataprocConfig.workerDiskSize || 0;

  return {
    desc:
      (newDiskSize < oldDiskSize ? 'Decrease' : 'Increase') +
      ' worker disk size',
    previous: oldDiskSize.toString() + ' GB',
    new: newDiskSize.toString() + ' GB',
    differenceType:
      oldDiskSize === newDiskSize
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.NEEDS_DELETE_RUNTIME,
  };
};

const compareDataprocNumberOfPreemptibleWorkers = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldNumWorkers =
    oldRuntime.dataprocConfig.numberOfPreemptibleWorkers || 0;
  const newNumWorkers =
    newRuntime.dataprocConfig.numberOfPreemptibleWorkers || 0;

  return {
    desc:
      (newNumWorkers < oldNumWorkers ? 'Decrease' : 'Increase') +
      ' number of preemptible workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType:
      oldNumWorkers === newNumWorkers
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.CAN_UPDATE_IN_PLACE,
  };
};

const compareDataprocNumberOfWorkers = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  if (!oldRuntime.dataprocConfig || !newRuntime.dataprocConfig) {
    return null;
  }

  const oldNumWorkers = oldRuntime.dataprocConfig.numberOfWorkers || 0;
  const newNumWorkers = newRuntime.dataprocConfig.numberOfWorkers || 0;

  return {
    desc:
      (newNumWorkers < oldNumWorkers ? 'Decrease' : 'Increase') +
      ' number of workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    differenceType:
      oldNumWorkers === newNumWorkers
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.CAN_UPDATE_IN_PLACE,
  };
};

const compareAutopauseThreshold = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff => {
  const oldAutopauseThreshold =
    oldRuntime.autopauseThreshold === null ||
    oldRuntime.autopauseThreshold === undefined
      ? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
      : oldRuntime.autopauseThreshold;
  const newAutopauseThreshold =
    newRuntime.autopauseThreshold == null ||
    newRuntime.autopauseThreshold === undefined
      ? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
      : newRuntime.autopauseThreshold;

  return {
    desc:
      (newAutopauseThreshold < oldAutopauseThreshold
        ? 'Decrease'
        : 'Increase') + ' autopause threshold',
    previous: AutopauseMinuteThresholds.get(oldAutopauseThreshold),
    new: AutopauseMinuteThresholds.get(newAutopauseThreshold),
    differenceType:
      oldAutopauseThreshold === newAutopauseThreshold
        ? RuntimeDiffState.NO_CHANGE
        : RuntimeDiffState.CAN_UPDATE_IN_PLACE,
  };
};

// Returns true if two runtimes are equivalent in terms of the fields which are
// affected by runtime presets.
const presetEquals = (a: Runtime, b: Runtime): boolean => {
  const strip = fp.flow(
    // In the future, things like toolDockerImage and autopause may be considerations.
    fp.pick(['gceConfig', 'dataprocConfig']),
    // numberOfWorkerLocalSSDs is currently part of the API spec, but is not used by the panel.
    fp.omit(['dataprocConfig.numberOfWorkerLocalSSDs'])
  );
  return fp.isEqual(strip(a), strip(b));
};

export const fromRuntimeConfig = (runtimeConfig: RuntimeConfig): Runtime => {
  const {
    diskSize,
    machine: { name: machineType },
    gpuConfig,
  } = runtimeConfig;

  const runtime: Runtime = {
    autopauseThreshold: runtimeConfig.autopauseThreshold,
  };
  if (runtimeConfig.computeType === ComputeType.Dataproc) {
    runtime.dataprocConfig = {
      ...runtimeConfig.dataprocConfig,
      masterMachineType: machineType,
      masterDiskSize: diskSize,
    };
  } else if (runtimeConfig.detachableDisk) {
    runtime.gceWithPdConfig = {
      machineType,
      persistentDisk: {
        size: diskSize,
        diskType: runtimeConfig.detachableDiskType,
        labels: {},
        name: '',
      },
      gpuConfig,
    };
  } else {
    runtime.gceConfig = {
      machineType,
      diskSize,
      gpuConfig,
    };
  }

  // If the selected runtime matches a preset, plumb through the appropriate configuration type.
  runtime.configurationType =
    fp.get(
      'runtimeTemplate.configurationType',
      fp.find(
        ({ runtimeTemplate }) => presetEquals(runtime, runtimeTemplate),
        runtimePresets
      )
    ) || RuntimeConfigurationType.UserOverride;

  return runtime;
};

export const withRuntimeConfigDefaults = (
  r: RuntimeConfig,
  existingDisk: Disk | null
): RuntimeConfig => {
  let {
    detachableDisk,
    detachableDiskType,
    diskSize,
    gpuConfig,
    dataprocConfig,
  } = r;
  const computeType = r.computeType ?? ComputeType.Standard;
  if (computeType === ComputeType.Standard) {
    dataprocConfig = null;
    if (detachableDisk === true) {
      diskSize = diskSize ?? existingDisk?.size ?? DEFAULT_DISK_SIZE;
      detachableDiskType =
        detachableDiskType ?? existingDisk?.diskType ?? DiskType.Standard;
    } else if (detachableDisk === false) {
      detachableDiskType = null;
    } else if (existingDisk) {
      // Detachable unspecified, but we have an existing disk.
      detachableDisk = true;
      diskSize = diskSize ?? existingDisk.size;
      detachableDiskType = detachableDiskType ?? existingDisk.diskType;
    } else {
      // Detachable unspecified and no existing disk.
      detachableDisk = false;
      detachableDiskType = null;
    }
  } else if (computeType === ComputeType.Dataproc) {
    detachableDisk = false;
    detachableDiskType = null;
    gpuConfig = null;

    const defaults = runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig;
    dataprocConfig = {
      numberOfWorkers:
        dataprocConfig?.numberOfWorkers ?? defaults.numberOfWorkers,
      workerMachineType:
        dataprocConfig?.workerMachineType ?? defaults.workerMachineType,
      workerDiskSize: dataprocConfig?.workerDiskSize ?? defaults.workerDiskSize,
      numberOfPreemptibleWorkers:
        dataprocConfig?.numberOfPreemptibleWorkers ??
        defaults.numberOfPreemptibleWorkers,
    };
  } else {
    throw Error(`unknown computeType: '${computeType}'`);
  }
  return {
    computeType,
    machine: r.machine ?? DEFAULT_MACHINE_TYPE,
    detachableDisk,
    diskSize: diskSize ?? DEFAULT_DISK_SIZE,
    detachableDiskType,
    dataprocConfig,
    gpuConfig,
    autopauseThreshold:
      r.autopauseThreshold ?? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  };
};

export const toRuntimeConfig = (runtime: Runtime): RuntimeConfig => {
  if (runtime.gceConfig) {
    const { machineType, diskSize, gpuConfig } = runtime.gceConfig;
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(machineType),
      detachableDisk: false,
      diskSize,
      detachableDiskType: null,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: null,
      gpuConfig,
    };
  } else if (runtime.gceWithPdConfig) {
    const {
      machineType,
      persistentDisk: { size: diskSize, diskType: detachableDiskType },
      gpuConfig,
    } = runtime.gceWithPdConfig;
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(machineType),
      detachableDisk: true,
      diskSize,
      detachableDiskType,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: null,
      gpuConfig,
    };
  } else if (runtime.dataprocConfig) {
    return {
      computeType: ComputeType.Dataproc,
      machine: findMachineByName(runtime.dataprocConfig.masterMachineType),
      detachableDisk: false,
      diskSize: runtime.dataprocConfig.masterDiskSize,
      detachableDiskType: null,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: runtime.dataprocConfig,
      gpuConfig: null,
    };
  } else {
    return {
      computeType: null,
      machine: null,
      detachableDisk: null,
      diskSize: null,
      detachableDiskType: null,
      autopauseThreshold: null,
      dataprocConfig: null,
      gpuConfig: null,
    };
  }
};

export const getRuntimeConfigDiffs = (
  oldRuntime: RuntimeConfig,
  newRuntime: RuntimeConfig
): RuntimeDiff[] => {
  return [
    compareWorkerCpu,
    compareWorkerMemory,
    compareDataprocWorkerDiskSize,
    compareDataprocNumberOfPreemptibleWorkers,
    compareDataprocNumberOfWorkers,
    compareComputeTypes,
    compareMachineCpu,
    compareMachineMemory,
    // TODO(RW-7582): Compare disk types, compare detachable.
    compareDiskSize,
    compareAutopauseThreshold,
    compareGpu,
  ]
    .map((compareFn) => compareFn(oldRuntime, newRuntime))
    .filter((diff) => diff !== null)
    .filter((diff) => diff.differenceType !== RuntimeDiffState.NO_CHANGE);
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
      () =>
        runtimeStore.set({
          workspaceNamespace: undefined,
          runtime: undefined,
          runtimeLoaded: false,
        }),
      async () => {
        let leoRuntime: Runtime;
        try {
          leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
        } catch (e) {
          if (e instanceof Response && e.status === 404) {
            // null on the runtime store indicates no existing runtime
            leoRuntime = null;
          } else {
            runtimeStore.set({
              workspaceNamespace: undefined,
              runtime: undefined,
              runtimeLoaded: false,
              loadingError: await maybeUnwrapSecuritySuspendedError(e),
            });
            return;
          }
        }
        if (
          currentWorkspaceNamespace === runtimeStore.get().workspaceNamespace
        ) {
          runtimeStore.set({
            workspaceNamespace: currentWorkspaceNamespace,
            runtime: leoRuntime,
            runtimeLoaded: true,
          });
        }
      }
    );
    getRuntime();
  }, [currentWorkspaceNamespace]);
};

export const maybeInitializeRuntime = async (
  workspaceNamespace: string,
  signal: AbortSignal,
  targetRuntime?: Runtime
): Promise<Runtime> => {
  if (workspaceNamespace in compoundRuntimeOpStore.get()) {
    await new Promise<void>((resolve, reject) => {
      signal.addEventListener('abort', reject);
      const { unsubscribe } = compoundRuntimeOpStore.subscribe((v) => {
        if (!(workspaceNamespace in v)) {
          unsubscribe();
          signal.removeEventListener('abort', reject);
          resolve();
        }
      });
    });
  }

  try {
    return await LeoRuntimeInitializer.initialize({
      workspaceNamespace,
      pollAbortSignal: signal,
      targetRuntime,
    });
  } catch (error) {
    throw await maybeUnwrapSecuritySuspendedError(error);
  }
};

// useDisk hook is a simple hook to populate the disk store.
// This is only used by other disk hooks
export const useDisk = (currentWorkspaceNamespace: string) => {
  const enablePD = serverConfigStore.get().config.enablePersistentDisk;
  useEffect(() => {
    if (!enablePD || !currentWorkspaceNamespace) {
      return;
    }
    const getDisk = withAsyncErrorHandling(
      () => diskStore.set({ workspaceNamespace: null, persistentDisk: null }),
      async () => {
        let persistentDisk: Disk = null;
        try {
          persistentDisk = await disksApi().getDisk(currentWorkspaceNamespace);
        } catch (e) {
          if (!(e instanceof Response && e.status === 404)) {
            throw e;
          }
        }
        if (currentWorkspaceNamespace === diskStore.get().workspaceNamespace) {
          diskStore.set({
            workspaceNamespace: currentWorkspaceNamespace,
            persistentDisk,
          });
        }
      }
    );
    getDisk();
  }, [currentWorkspaceNamespace]);
};

// useRuntimeStatus hook can be used to change the status of the runtime
// This setter returns a promise which resolves when any proximal fetch has completed,
// but does not wait for any polling, which may continue asynchronously.
export const useRuntimeStatus = (
  currentWorkspaceNamespace,
  currentGoogleProject
): [
  RuntimeStatus | undefined,
  (statusRequest: RuntimeStatusRequest) => Promise<void>
] => {
  const [runtimeStatus, setRuntimeStatus] = useState<RuntimeStatusRequest>();
  const { runtime } = useStore(runtimeStore);
  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);
  useDisk(currentWorkspaceNamespace);
  useEffect(() => {
    // Additional status changes can be put here
    const resolutionCondition: (r: Runtime) => boolean = switchCase(
      runtimeStatus,
      [
        RuntimeStatusRequest.DeleteRuntime,
        () => (r) => r === null || r.status === RuntimeStatus.Deleted,
      ],
      [
        RuntimeStatusRequest.DeleteRuntimeAndPD,
        () => (r) => r === null || r.status === RuntimeStatus.Deleted,
      ],
      [
        RuntimeStatusRequest.DeletePD,
        () => (r) =>
          r.status === RuntimeStatus.Running ||
          r.status === RuntimeStatus.Stopped,
      ],
      [
        RuntimeStatusRequest.Start,
        () => (r) => r.status === RuntimeStatus.Running,
      ],
      [
        RuntimeStatusRequest.Stop,
        () => (r) => r.status === RuntimeStatus.Stopped,
      ]
    );
    const initializePolling = async () => {
      if (!!runtimeStatus) {
        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: currentWorkspaceNamespace,
            maxCreateCount: 0,
            resolutionCondition: (r) => resolutionCondition(r),
          });
        } catch (e) {
          // ExceededActionCountError is expected, as we exceed our create limit of 0.
          if (
            !(
              e instanceof ExceededActionCountError ||
              e instanceof LeoRuntimeInitializationAbortedError
            )
          ) {
            throw e;
          }
        }
      }
    };
    initializePolling();
  }, [runtimeStatus]);

  const setStatusRequest = async (req) => {
    await switchCase(
      req,
      [
        RuntimeStatusRequest.DeleteRuntime,
        () => {
          return runtimeApi().deleteRuntime(currentWorkspaceNamespace, false);
        },
      ],
      [
        RuntimeStatusRequest.DeleteRuntimeAndPD,
        () => {
          return runtimeApi().deleteRuntime(currentWorkspaceNamespace, true);
        },
      ],
      [
        RuntimeStatusRequest.DeletePD,
        () => {
          return disksApi().deleteDisk(
            currentWorkspaceNamespace,
            diskStore.get().persistentDisk.name
          );
        },
      ],
      [
        RuntimeStatusRequest.Start,
        () => {
          return leoRuntimesApi().startRuntime(
            currentGoogleProject,
            runtime.runtimeName
          );
        },
      ],
      [
        RuntimeStatusRequest.Stop,
        () => {
          return leoRuntimesApi().stopRuntime(
            currentGoogleProject,
            runtime.runtimeName
          );
        },
      ]
    );
    setRuntimeStatus(req);
  };
  return [runtime ? runtime.status : undefined, setStatusRequest];
};

export const getRuntimeCtx = (runtime: Runtime, pendingRuntime: Runtime) => {
  const pdFeatureFlag = serverConfigStore.get().config.enablePersistentDisk;
  const runtimeExists =
    (runtime?.status &&
      ![RuntimeStatus.Deleted, RuntimeStatus.Error].includes(runtime.status)) ||
    !!pendingRuntime;
  const { dataprocConfig = null } =
    pendingRuntime || runtime || ({} as Partial<Runtime>);
  const initialCompute = dataprocConfig
    ? ComputeType.Dataproc
    : ComputeType.Standard;
  const gceExists = runtimeExists && initialCompute === ComputeType.Standard;
  const persistentDisk = diskStore.get().persistentDisk;
  return {
    runtimeExists: runtimeExists,
    gceExists: runtimeExists && initialCompute === ComputeType.Standard,
    dataprocExists: dataprocConfig !== null,
    pdExists: !!persistentDisk,
    enablePD: pdFeatureFlag && (!!persistentDisk || !gceExists),
  };
};

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useCustomRuntime = (
  currentWorkspaceNamespace: string,
  detachablePd: Disk | null
): [
  { currentRuntime: Runtime; pendingRuntime: Runtime },
  (runtime: Runtime) => void
] => {
  const { runtime, workspaceNamespace } = useStore(runtimeStore);
  const runtimeOps = useStore(compoundRuntimeOpStore);
  const { pendingRuntime = null } = runtimeOps[currentWorkspaceNamespace] || {};
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime>();

  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    let mounted = true;
    const aborter = new AbortController();
    const runAction = async () => {
      // TODO: It is likely more correct here to use the LeoRuntimeInitializer wait for the runtime
      // to reach a terminal status before attempting deletion.
      const runtimeCtx = getRuntimeCtx(runtime, pendingRuntime);
      try {
        if (runtimeCtx.runtimeExists) {
          const oldRuntimeConfig = toRuntimeConfig(runtime);
          const newRuntimeConfig = toRuntimeConfig(requestedRuntime);
          const runtimeDiffTypes = getRuntimeConfigDiffs(
            oldRuntimeConfig,
            newRuntimeConfig
          ).map((diff) => diff.differenceType);
          const pdIncreased =
            !!detachablePd &&
            newRuntimeConfig.detachableDisk &&
            newRuntimeConfig.diskSize > detachablePd.size;

          if (runtimeDiffTypes.includes(RuntimeDiffState.NEEDS_DELETE_PD)) {
            // Directly call disk api to delete pd if there's no runtime or the runtime is dataproc
            if (
              runtime.status === RuntimeStatus.Deleted ||
              runtimeCtx.dataprocExists
            ) {
              await disksApi().deleteDisk(
                currentWorkspaceNamespace,
                detachablePd.name,
                {
                  signal: aborter.signal,
                }
              );
            }
            // Call runtime api to delete pd if the runtime is gce with pd
            if (runtimeCtx.gceExists) {
              await runtimeApi().deleteRuntime(
                currentWorkspaceNamespace,
                true,
                {
                  signal: aborter.signal,
                }
              );
            }
          } else if (
            runtimeDiffTypes.includes(RuntimeDiffState.NEEDS_DELETE_RUNTIME)
          ) {
            await runtimeApi().deleteRuntime(currentWorkspaceNamespace, false, {
              signal: aborter.signal,
            });
          } else if (
            runtimeDiffTypes.includes(
              RuntimeDiffState.CAN_UPDATE_WITH_REBOOT
            ) ||
            runtimeDiffTypes.includes(RuntimeDiffState.CAN_UPDATE_IN_PLACE)
          ) {
            if (
              runtime.status === RuntimeStatus.Running ||
              runtime.status === RuntimeStatus.Stopped
            ) {
              await runtimeApi().updateRuntime(currentWorkspaceNamespace, {
                runtime: requestedRuntime,
              });
              // Calling updateRuntime will not immediately set the Runtime status to not Running so the
              // default initializer will resolve on its first call. The polling below first checks for the
              // non Running status before initializing the default one that checks for Running status
              await LeoRuntimeInitializer.initialize({
                workspaceNamespace,
                targetRuntime: requestedRuntime,
                resolutionCondition: (r) => r.status !== RuntimeStatus.Running,
                pollAbortSignal: aborter.signal,
                overallTimeout: 1000 * 60, // The switch to a non running status should occur quickly
              });
            } else if (
              runtime.status === RuntimeStatus.Deleted &&
              pdIncreased
            ) {
              await disksApi().updateDisk(
                currentWorkspaceNamespace,
                diskStore.get().persistentDisk.name,
                requestedRuntime.gceWithPdConfig.persistentDisk.size
              );
            }
          }
        } else if (runtime?.status === RuntimeStatus.Error) {
          await runtimeApi().deleteRuntime(currentWorkspaceNamespace, false, {
            signal: aborter.signal,
          });
        }

        await LeoRuntimeInitializer.initialize({
          workspaceNamespace,
          targetRuntime: requestedRuntime,
          pollAbortSignal: aborter.signal,
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

    if (
      requestedRuntime !== undefined &&
      !fp.equals(requestedRuntime, runtime)
    ) {
      registerCompoundRuntimeOperation(currentWorkspaceNamespace, {
        pendingRuntime: requestedRuntime,
        aborter,
      });
      runAction();
    }

    // After dismount, we still want the above store modifications to occur.
    // However, we should not continue to mutate the now unmounted hook state -
    // this will result in React warnings.
    return () => {
      mounted = false;
    };
  }, [requestedRuntime]);

  return [{ currentRuntime: runtime, pendingRuntime }, setRequestedRuntime];
};

export const withRuntimeStore = () => (WrappedComponent) => {
  return (props) => {
    const value = useStore(runtimeStore);
    // Ensure that a runtime gets initialized, if it hasn't already been.
    useRuntime(value.workspaceNamespace);
    useDisk(value.workspaceNamespace);

    return <WrappedComponent {...props} runtimeStore={value} />;
  };
};
