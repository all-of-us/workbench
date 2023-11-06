import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  DataprocConfig,
  Disk,
  DiskType,
  ErrorCode,
  GpuConfig,
  ListRuntimeResponse,
  PersistentDiskRequest,
  Runtime,
  RuntimeConfigurationType,
  RuntimeError,
  RuntimeStatus,
  SecuritySuspendedErrorParameters,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import {
  ExceededErrorCountError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  AutopauseMinuteThresholds,
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_DISK_SIZE,
  DEFAULT_MACHINE_TYPE,
  findMachineByName,
  Machine,
} from 'app/utils/machines';
import {
  compoundRuntimeOpStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';

import { runtimePresets } from './runtime-presets';

export class ComputeSecuritySuspendedError extends Error {
  constructor(public params: SecuritySuspendedErrorParameters) {
    super('user is suspended from compute');
    // See https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
    Object.setPrototypeOf(this, ComputeSecuritySuspendedError.prototype);

    this.name = 'ComputeSecuritySuspendedError';
  }
}

export class RuntimeStatusError extends Error {
  constructor(public errors?: RuntimeError[]) {
    super(
      'runtime creation failed:\n' +
        errors?.map((m) => m.errorMessage).join('\n')
    );
    // See https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
    Object.setPrototypeOf(this, RuntimeStatusError.prototype);

    this.name = 'RuntimeStatusError';
  }
}

export enum RuntimeStatusRequest {
  DeleteRuntime = 'DeleteRuntime',
  DeleteRuntimeAndPD = 'DeleteRuntimeAndPD',
  DeletePD = 'DeletePD',
  Start = 'Start',
  Stop = 'Stop',
}

export interface AnalysisDiff {
  desc: string;
  previous: string;
  new: string;
  diff: AnalysisDiffState;
  diskDiff?: AnalysisDiffState;
}

export enum AnalysisDiffState {
  // Arranged in order of increasing severity. We depend on this ordering below.
  NO_CHANGE,
  CAN_UPDATE_IN_PLACE,
  CAN_UPDATE_WITH_REBOOT,
  NEEDS_DELETE,
}

export const diskTypeLabels = {
  [DiskType.STANDARD]: 'Standard Disk',
  [DiskType.SSD]: 'Solid State Disk',
};

export interface DiskConfig {
  size: number;
  detachable: boolean;
  detachableType: DiskType | null;
  existingDiskName: string | null;
}

export interface AnalysisConfig {
  computeType: ComputeType;
  machine: Machine;
  diskConfig: DiskConfig;
  // TODO: Document types of disks available (RW-9490)
  // This should only be populated if !diskconfig.detachable.
  detachedDisk: Disk;
  // TODO: Refactor this type to an intermediate representation.
  dataprocConfig: DataprocConfig;
  // TODO: Refactor this type to an intermediate representation.
  gpuConfig: GpuConfig;
  autopauseThreshold: number;
  numNodes?: number;
}

export interface UpdateMessaging {
  applyAction: string;
  warn?: string;
  warnMore?: string;
}

export const RUNTIME_ERROR_STATUS_MESSAGE_SHORT =
  'An error was encountered with your cloud environment. ' +
  'To resolve, please see the cloud analysis environment side panel.';

const errorToSecuritySuspendedParams = async (
  error
): Promise<SecuritySuspendedErrorParameters> => {
  if (error?.status !== 412) {
    return null;
  }
  const body = await error?.json();
  if (body?.errorCode !== ErrorCode.COMPUTE_SECURITY_SUSPENDED) {
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
  states: AnalysisDiffState[]
): AnalysisDiffState => {
  return fp.last(
    [...states].filter((s) => s !== null && s !== undefined).sort()
  );
};

export const applyUpdate: UpdateMessaging = {
  applyAction: 'APPLY',
};
export const rebootUpdate: UpdateMessaging = {
  applyAction: 'APPLY & REBOOT',
  warn: 'These changes require a reboot of your cloud environment to take effect.',
  warnMore:
    'Any in-memory state will be erased, but local file ' +
    'modifications will be preserved. Data stored in workspace ' +
    'buckets is never affected by changes to your cloud environment.',
};
export const recreateEnvUpdate: UpdateMessaging = {
  applyAction: 'APPLY & RECREATE',
  warn:
    'These changes require deletion and re-creation of your cloud ' +
    'environment to take effect.',
  warnMore:
    'Any in-memory state and local file modifications will be ' +
    'erased. Data stored in workspace buckets is never affected ' +
    'by changes to your cloud environment.',
};
export const recreateEnvAndPDUpdate: UpdateMessaging = {
  applyAction: 'APPLY & RECREATE',
  warn:
    'These changes require deletion and re-creation of your persistent disk and cloud environment to take ' +
    'effect. This will delete all files on the disk.',
  warnMore:
    'If you want to save some files permanently, such as input data, analysis outputs, or installed packages, ' +
    'move them to the workspace bucket. \nNote: Jupyter notebooks are autosaved to the workspace bucket, and deleting ' +
    'your disk will not delete your notebooks.',
};
export const diffsToUpdateMessaging = (
  diffs: AnalysisDiff[]
): UpdateMessaging => {
  const diffType = findMostSevereDiffState(diffs.map(({ diff }) => diff));
  const diskDiffType = findMostSevereDiffState(
    diffs.map(({ diskDiff }) => diskDiff)
  );

    return cond(
        [
            diffType === AnalysisDiffState.NEEDS_DELETE &&
            diskDiffType === AnalysisDiffState.NEEDS_DELETE,
            recreateEnvAndPDUpdate,
        ],
        [
            diffType === AnalysisDiffState.NEEDS_DELETE &&
            diskDiffType !== AnalysisDiffState.NEEDS_DELETE,
            recreateEnvUpdate,
        ],
        [diffType === AnalysisDiffState.CAN_UPDATE_WITH_REBOOT, rebootUpdate],
        // All other cases can be applied without user disruption.
        applyUpdate
    );
};

const compareComputeTypes = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  return {
    desc: 'Change compute type',
    previous: oldConfig.computeType,
    new: newConfig.computeType,
    diff:
      oldConfig.computeType === newConfig.computeType
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.NEEDS_DELETE,
  };
};

const compareMachineCpu = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  const oldCpu = oldConfig.machine.cpu;
  const newCpu = newConfig.machine.cpu;

  return {
    desc: (newCpu < oldCpu ? 'Decrease' : 'Increase') + ' number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    diff:
      oldCpu === newCpu
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
  };
};

const compareMachineMemory = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  const oldMemory = oldConfig.machine.memory;
  const newMemory = newConfig.machine.memory;

  return {
    desc: (newMemory < oldMemory ? 'Decrease' : 'Increase') + ' memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    diff:
      oldMemory === newMemory
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
  };
};

export const compareGpu = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  const oldGpuExists = !!oldConfig.gpuConfig;
  const newGpuExists = !!newConfig.gpuConfig;
  return {
    desc: 'Change GPU config',
    previous: oldGpuExists
      ? `${oldConfig.gpuConfig.numOfGpus} ${oldConfig.gpuConfig.gpuType} GPU`
      : 'No GPUs',
    new: newGpuExists
      ? `${newConfig.gpuConfig.numOfGpus} ${newConfig.gpuConfig.gpuType} GPU`
      : 'No GPUs',
    diff:
      (!oldGpuExists && !newGpuExists) ||
      (oldGpuExists &&
        newGpuExists &&
        oldConfig.gpuConfig.gpuType === newConfig.gpuConfig.gpuType &&
        oldConfig.gpuConfig.numOfGpus === newConfig.gpuConfig.numOfGpus)
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.NEEDS_DELETE,
  };
};

const compareDiskSize = (
  { diskConfig: oldDiskConfig }: AnalysisConfig,
  { diskConfig: newDiskConfig }: AnalysisConfig
): AnalysisDiff => {
  let desc = 'Disk Size';
  let diff: AnalysisDiffState;
  let diskDiff: AnalysisDiffState;
  if (newDiskConfig.size < oldDiskConfig.size) {
    desc = 'Decrease ' + desc;
    diff = AnalysisDiffState.NEEDS_DELETE;
    if (newDiskConfig.detachable && oldDiskConfig.detachable) {
      diskDiff = AnalysisDiffState.NEEDS_DELETE;
    } else {
      diskDiff = AnalysisDiffState.NO_CHANGE;
    }
  } else if (newDiskConfig.size > oldDiskConfig.size) {
    desc = 'Increase ' + desc;
    if (newDiskConfig.detachable) {
      // Technically, a PD can always be extended in place. However, the only Leo-supported
      // way to claim the newly allocated space is by rebooting (disk is remounted).
      diff = AnalysisDiffState.NO_CHANGE;
      diskDiff = AnalysisDiffState.CAN_UPDATE_IN_PLACE;
    } else {
      // Attached PDs are not distinct from the runtime itself.
      diff = AnalysisDiffState.CAN_UPDATE_WITH_REBOOT;
    }
  } else {
    diff = AnalysisDiffState.NO_CHANGE;
  }

  return {
    desc: desc,
    previous: oldDiskConfig.size && oldDiskConfig.size.toString() + ' GB',
    new: newDiskConfig.size && newDiskConfig.size.toString() + ' GB',
    diff,
    diskDiff,
  };
};

const compareDiskTypes = (
  { diskConfig: oldDiskConfig }: AnalysisConfig,
  { diskConfig: newDiskConfig }: AnalysisConfig
): AnalysisDiff => {
  let diff: AnalysisDiffState;
  let diskDiff: AnalysisDiffState;
  if (newDiskConfig.detachable === oldDiskConfig.detachable) {
    if (newDiskConfig.detachableType === oldDiskConfig.detachableType) {
      diff = AnalysisDiffState.NO_CHANGE;
      diskDiff = AnalysisDiffState.NO_CHANGE;
    } else {
      diff = AnalysisDiffState.NEEDS_DELETE;
      diskDiff = AnalysisDiffState.NEEDS_DELETE;
    }
  } else {
    diff = AnalysisDiffState.NEEDS_DELETE;
    // Detachability state has changed. There is no forced PD change in this
    // scenario. However, there may be changes required on an unattached PD
    // (not handled here).
    diskDiff = AnalysisDiffState.NO_CHANGE;
  }

  const describeDiskType = ({ detachable, detachableType }: DiskConfig) => {
    return detachable
      ? `Reattachable ${diskTypeLabels[detachableType]}`
      : 'Standard Disk';
  };
  return {
    desc: 'Change disk type',
    previous: describeDiskType(oldDiskConfig),
    new: describeDiskType(newDiskConfig),
    diff,
    diskDiff,
  };
};

const compareDetachedDisks = (
  { diskConfig: oldDiskConfig, detachedDisk: oldDetachedDisk }: AnalysisConfig,
  { diskConfig: newDiskConfig, detachedDisk: newDetachedDisk }: AnalysisConfig
): AnalysisDiff => {
  // This comparator only concerns itself with legal state transitions within
  // the Workbench. Currently the only allowed interaction with an unattached
  // disk is to delete it. Therefore, this only returns a diff if an analysis
  // config transition deletes a detached disk.
  const oldDetachableExists = oldDiskConfig.detachable || !!oldDetachedDisk;
  const newDetachableExists = newDiskConfig.detachable || !!newDetachedDisk;
  if (oldDetachableExists && !newDetachableExists) {
    const oldSize = oldDiskConfig.detachable
      ? oldDiskConfig.size
      : oldDetachedDisk.size;
    return {
      desc: 'Reattachable disk',
      previous: `${oldSize}GB disk`,
      new: 'None',
      diff: AnalysisDiffState.NO_CHANGE,
      diskDiff: AnalysisDiffState.NEEDS_DELETE,
    };
  }
  return {
    desc: 'Reattachable disk',
    previous: '',
    new: '',
    diff: AnalysisDiffState.NO_CHANGE,
  };
};

const compareWorkerCpu = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  if (!oldConfig.dataprocConfig || !newConfig.dataprocConfig) {
    return null;
  }

  const oldCpu = findMachineByName(
    oldConfig.dataprocConfig.workerMachineType
  ).cpu;
  const newCpu = findMachineByName(
    newConfig.dataprocConfig.workerMachineType
  ).cpu;

  return {
    desc: (newCpu < oldCpu ? 'Decrease' : 'Increase') + ' number of CPUs',
    previous: oldCpu.toString(),
    new: newCpu.toString(),
    diff:
      oldCpu === newCpu
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.NEEDS_DELETE,
  };
};

const compareWorkerMemory = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  if (!oldConfig.dataprocConfig || !newConfig.dataprocConfig) {
    return null;
  }

  const oldMemory = findMachineByName(
    oldConfig.dataprocConfig.workerMachineType
  ).memory;
  const newMemory = findMachineByName(
    newConfig.dataprocConfig.workerMachineType
  ).memory;

  return {
    desc: (newMemory < oldMemory ? 'Decrease' : 'Increase') + ' memory',
    previous: oldMemory.toString() + ' GB',
    new: newMemory.toString() + ' GB',
    diff:
      oldMemory === newMemory
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.NEEDS_DELETE,
  };
};

const compareDataprocWorkerDiskSize = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  if (!oldConfig.dataprocConfig || !newConfig.dataprocConfig) {
    return null;
  }

  const oldDiskSize = oldConfig.dataprocConfig.workerDiskSize || 0;
  const newDiskSize = newConfig.dataprocConfig.workerDiskSize || 0;

  return {
    desc:
      (newDiskSize < oldDiskSize ? 'Decrease' : 'Increase') +
      ' worker disk size',
    previous: oldDiskSize.toString() + ' GB',
    new: newDiskSize.toString() + ' GB',
    diff:
      oldDiskSize === newDiskSize
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.NEEDS_DELETE,
  };
};

const compareDataprocNumberOfPreemptibleWorkers = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  if (!oldConfig.dataprocConfig || !newConfig.dataprocConfig) {
    return null;
  }

  const oldNumWorkers =
    oldConfig.dataprocConfig.numberOfPreemptibleWorkers || 0;
  const newNumWorkers =
    newConfig.dataprocConfig.numberOfPreemptibleWorkers || 0;

  return {
    desc:
      (newNumWorkers < oldNumWorkers ? 'Decrease' : 'Increase') +
      ' number of preemptible workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    diff:
      oldNumWorkers === newNumWorkers
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.CAN_UPDATE_IN_PLACE,
  };
};

const compareDataprocNumberOfWorkers = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  if (!oldConfig.dataprocConfig || !newConfig.dataprocConfig) {
    return null;
  }

  const oldNumWorkers = oldConfig.dataprocConfig.numberOfWorkers || 0;
  const newNumWorkers = newConfig.dataprocConfig.numberOfWorkers || 0;

  return {
    desc:
      (newNumWorkers < oldNumWorkers ? 'Decrease' : 'Increase') +
      ' number of workers',
    previous: oldNumWorkers.toString(),
    new: newNumWorkers.toString(),
    diff:
      oldNumWorkers === newNumWorkers
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.CAN_UPDATE_IN_PLACE,
  };
};

const compareAutopauseThreshold = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff => {
  const oldAutopauseThreshold =
    oldConfig.autopauseThreshold === null ||
    oldConfig.autopauseThreshold === undefined
      ? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
      : oldConfig.autopauseThreshold;
  const newAutopauseThreshold =
    newConfig.autopauseThreshold == null ||
    newConfig.autopauseThreshold === undefined
      ? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES
      : newConfig.autopauseThreshold;

  return {
    desc:
      (newAutopauseThreshold < oldAutopauseThreshold
        ? 'Decrease'
        : 'Increase') + ' autopause threshold',
    previous: AutopauseMinuteThresholds.get(oldAutopauseThreshold),
    new: AutopauseMinuteThresholds.get(newAutopauseThreshold),
    diff:
      oldAutopauseThreshold === newAutopauseThreshold
        ? AnalysisDiffState.NO_CHANGE
        : AnalysisDiffState.CAN_UPDATE_IN_PLACE,
  };
};

// Returns true if two runtimes are equivalent in terms of the fields which are
// affected by runtime presets.
const presetEquals = (a: Runtime, b: Runtime): boolean => {
  const strip = fp.flow(
    // In the future, things like toolDockerImage and autopause may be considerations.
    // With https://precisionmedicineinitiative.atlassian.net/browse/RW-9167, general analysis
    // should have persistent disk
    fp.pick(['gceWithPdConfig', 'dataprocConfig']),
    // numberOfWorkerLocalSSDs is currently part of the API spec, but is not used by the panel.
    fp.omit(['dataprocConfig.numberOfWorkerLocalSSDs'])
  );
  return fp.isEqual(strip(a), strip(b));
};

export const fromAnalysisConfig = (analysisConfig: AnalysisConfig): Runtime => {
  const {
    autopauseThreshold,
    computeType,
    dataprocConfig,
    diskConfig,
    gpuConfig,
    machine: { name: machineType },
  } = analysisConfig;

  const runtime: Runtime = {
    autopauseThreshold,
  };
  if (computeType === ComputeType.Dataproc) {
    runtime.dataprocConfig = {
      ...dataprocConfig,
      masterMachineType: machineType,
      masterDiskSize: diskConfig.size,
    };
  } else if (diskConfig.detachable) {
    runtime.gceWithPdConfig = {
      machineType,
      gpuConfig,
      persistentDisk: {
        size: diskConfig.size,
        diskType: diskConfig.detachableType,
        labels: {},
        name: diskConfig.existingDiskName,
      },
    };
  } else {
    runtime.gceConfig = {
      machineType,
      gpuConfig,
      diskSize: diskConfig.size,
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
    ) || RuntimeConfigurationType.USER_OVERRIDE;

  return runtime;
};

export const canUseExistingDisk = (
  { detachableType, size }: Partial<DiskConfig>,
  existingDisk: Disk | null
) => {
  return (
    !!existingDisk &&
    detachableType === existingDisk.diskType &&
    size >= existingDisk.size
  );
};

export const maybeWithExistingDiskName = (
  c: Omit<DiskConfig, 'existingDiskName'>,
  existingDisk: Disk | null
): DiskConfig => {
  if (canUseExistingDisk(c, existingDisk)) {
    return { ...c, existingDiskName: existingDisk.name };
  }
  return { ...c, existingDiskName: null };
};

export const maybeWithPersistentDisk = (
  runtime: Runtime,
  persistentDisk: Disk | PersistentDiskRequest | null | undefined
): Runtime => {
  if (!runtime || !persistentDisk || !runtime.gceConfig) {
    return runtime;
  }
  // TODO: why not all fields?
  const { name, size, diskType } = persistentDisk;
  return {
    ...runtime,
    gceConfig: null, // TODO: why not undefined?
    gceWithPdConfig: {
      ...runtime.gceConfig, // note: gceConfig.diskSize is discarded.  this is what we want.
      persistentDisk: { name, size, diskType },
    },
  };
};

// TODO - this is way more complex than it needs to be, and likely has some errors
export const withAnalysisConfigDefaults = (
  r: AnalysisConfig,
  existingPersistentDisk: Disk | null
): AnalysisConfig => {
  let {
    diskConfig: { size, detachable, detachableType },
    gpuConfig,
    dataprocConfig,
  } = r;
  let existingDiskName = null;
  const computeType = r.computeType ?? ComputeType.Standard;
  // For computeType Standard: We are moving away from storage disk as Standard
  // As part of RW-9167, we are disabling Standard storage disk if computeType is standard
  // Eventually we will be removing this option altogether
  if (computeType === ComputeType.Standard) {
    if (existingPersistentDisk) {
      detachable = true;
      size = size ?? existingPersistentDisk?.size ?? DEFAULT_DISK_SIZE;
      detachableType =
        detachableType ?? existingPersistentDisk?.diskType ?? DiskType.STANDARD;
      if (canUseExistingDisk(r.diskConfig, existingPersistentDisk)) {
        existingDiskName = existingPersistentDisk.name;
      }
    } else {
      // No existing disk.
      detachableType = DiskType.STANDARD;
      detachable = true;
    }
  } else if (computeType === ComputeType.Dataproc) {
    detachable = false;
    detachableType = null;
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
    size = size ?? existingPersistentDisk?.size ?? DATAPROC_MIN_DISK_SIZE_GB;
  } else {
    throw Error(`unknown computeType: '${computeType}'`);
  }

  return {
    computeType,
    machine: r.machine ?? DEFAULT_MACHINE_TYPE,
    diskConfig: {
      size: size ?? DEFAULT_DISK_SIZE,
      detachable,
      detachableType,
      existingDiskName,
    },
    detachedDisk: detachable ? null : existingPersistentDisk,
    dataprocConfig,
    gpuConfig,
    autopauseThreshold:
      r.autopauseThreshold ?? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  };
};

export const toAnalysisConfig = (
  runtime: Runtime,
  existingDisk: Disk | null
): AnalysisConfig => {
  const toGceConfig = () => {
    const { machineType, diskSize, gpuConfig } = runtime.gceConfig;
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(machineType),
      diskConfig: {
        size: diskSize,
        detachable: false,
        detachableType: null,
        existingDiskName: null,
      },
      detachedDisk: existingDisk,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: null,
      gpuConfig,
    };
  };
  const toGceWithPdConfig = () => {
    const {
      machineType,
      persistentDisk: { size, diskType: detachableType },
      gpuConfig,
    } = runtime.gceWithPdConfig;
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(machineType),
      diskConfig: maybeWithExistingDiskName(
        {
          size,
          detachable: true,
          detachableType,
        },
        existingDisk
      ),
      detachedDisk: null,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: null,
      gpuConfig,
    };
  };
  const toDataprocConfig = () => {
    const { dataprocConfig, autopauseThreshold } = runtime;
    const { masterMachineType, masterDiskSize } = dataprocConfig;
    return {
      computeType: ComputeType.Dataproc,
      machine: findMachineByName(masterMachineType),
      diskConfig: {
        size: masterDiskSize,
        detachable: false,
        detachableType: null,
        existingDiskName: null,
      },
      detachedDisk: existingDisk,
      autopauseThreshold,
      dataprocConfig,
      gpuConfig: null,
    };
  };
  const toEmptyConfig = () => ({
    computeType: null,
    machine: null,
    diskConfig: {
      size: null,
      detachable: null,
      detachableType: null,
      existingDiskName: null,
    },
    detachedDisk: existingDisk,
    autopauseThreshold: null,
    dataprocConfig: null,
    gpuConfig: null,
  });

  return cond(
    [!!runtime.gceConfig, toGceConfig],
    [!!runtime.gceWithPdConfig, toGceWithPdConfig],
    [!!runtime.dataprocConfig, toDataprocConfig],
    toEmptyConfig
  );
};

export const getAnalysisConfigDiffs = (
  oldConfig: AnalysisConfig,
  newConfig: AnalysisConfig
): AnalysisDiff[] => {
  return [
    compareWorkerCpu,
    compareWorkerMemory,
    compareDataprocWorkerDiskSize,
    compareDataprocNumberOfPreemptibleWorkers,
    compareDataprocNumberOfWorkers,
    compareComputeTypes,
    compareMachineCpu,
    compareMachineMemory,
    compareDiskTypes,
    compareDiskSize,
    compareDetachedDisks,
    compareAutopauseThreshold,
    compareGpu,
  ]
    .map((compareFn) => compareFn(oldConfig, newConfig))
    .filter((diff) => diff !== null)
    .filter(
      ({ diff, diskDiff }) =>
        diff !== AnalysisDiffState.NO_CHANGE ||
        (diskDiff !== undefined && diskDiff !== AnalysisDiffState.NO_CHANGE)
    );
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
    const runtime = await LeoRuntimeInitializer.initialize({
      workspaceNamespace,
      pollAbortSignal: signal,
      targetRuntime,
    });
    if (runtime.status === RuntimeStatus.ERROR) {
      throw new RuntimeStatusError(runtime.errors);
    }
    return runtime;
  } catch (error) {
    throw await maybeUnwrapSecuritySuspendedError(error);
  }
};

export const withUserAppsStore = () => (WrappedComponent) => {
  return (props) => {
    const value = useStore(userAppsStore);

    return <WrappedComponent {...props} userAppsStore={value} />;
  };
};

export enum SparkConsolePath {
  Yarn = 'yarn',
  YarnTimeline = 'apphistory',
  SparkHistory = 'sparkhistory',
  JobHistory = 'jobhistory',
}

export enum PanelContent {
  Create = 'Create',
  Customize = 'Customize',
  DeleteRuntime = 'DeleteRuntime',
  DeleteUnattachedPd = 'DeleteUnattachedPd',
  DeleteUnattachedPdAndCreate = 'DeleteUnattachedPdAndCreate',
  Disabled = 'Disabled',
  ConfirmUpdate = 'ConfirmUpdate',
  ConfirmUpdateWithDiskDelete = 'ConfirmUpdateWithDiskDelete',
  SparkConsole = 'SparkConsole',
}

// should we show the runtime in the UI (in most situations)?
// Note: we do make users aware of ERROR runtimes in some situations
export const isVisible = (status: RuntimeStatus) =>
  status &&
  !(
    [RuntimeStatus.DELETED, RuntimeStatus.ERROR] as Array<RuntimeStatus>
  ).includes(status);

// can the user delete the runtime?
export const canDeleteRuntime = (status: RuntimeStatus) =>
  (
    [
      RuntimeStatus.RUNNING,
      RuntimeStatus.STOPPED,
      RuntimeStatus.ERROR,
    ] as Array<RuntimeStatus>
  ).includes(status);

export const getCreator = (runtime: ListRuntimeResponse): string | undefined =>
  // eslint-disable-next-line @typescript-eslint/dot-notation
  runtime?.labels?.['creator'];
