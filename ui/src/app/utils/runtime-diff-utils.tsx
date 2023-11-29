import * as fp from 'lodash/fp';

import { cond } from '@terra-ui-packages/core-utils';

import {
  AutopauseMinuteThresholds,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  findMachineByName,
} from './machines';
import {
  AnalysisConfig,
  DiskConfig,
  diskTypeLabels,
  UpdateMessaging,
} from './runtime-utils';

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
