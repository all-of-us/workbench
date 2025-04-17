import {
  DataprocConfig,
  Disk,
  DiskType,
  GpuConfig,
  PersistentDiskRequest,
  Runtime,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { serverConfigStore } from 'app/utils/stores';

import {
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_DISK_SIZE,
  DEFAULT_MACHINE_TYPE,
  findMachineByName,
  Machine,
} from './machines';
import { runtimePresets } from './runtime-presets';
import { DiskConfig } from './runtime-utils';

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
  zone?: string;
}

export const fromAnalysisConfig = (analysisConfig: AnalysisConfig): Runtime => {
  const {
    autopauseThreshold,
    computeType,
    dataprocConfig,
    diskConfig,
    gpuConfig,
    machine: { name: machineType },
    zone,
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
      zone,
    };
  } else {
    runtime.gceConfig = {
      machineType,
      gpuConfig,
      diskSize: diskConfig.size,
      zone,
    };
  }

  return runtime;
};

export const canUseExistingDisk = (
  { detachableType, size }: Partial<DiskConfig>,
  existingDisk: Disk | null
) =>
  !!existingDisk &&
  (!detachableType || detachableType === existingDisk.diskType) &&
  size >= existingDisk.size;

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
    zone,
  } = r;
  let existingDiskName = null;
  const computeType = r.computeType ?? ComputeType.Standard;
  // For computeType Standard: We are moving away from storage disk as Standard
  // As part of RW-9167, we are disabling Standard storage disk if computeType is standard
  // Eventually we will be removing this option altogether
  if (computeType === ComputeType.Standard) {
    zone ??= serverConfigStore.get().config.defaultGceVmZone;
    detachable = true;
    detachableType ??= existingPersistentDisk?.diskType ?? DiskType.STANDARD;
    if (existingPersistentDisk) {
      zone = existingPersistentDisk.zone;
      size ??= existingPersistentDisk?.size ?? DEFAULT_DISK_SIZE;
      if (canUseExistingDisk(r.diskConfig, existingPersistentDisk)) {
        existingDiskName = existingPersistentDisk.name;
      }
    }
  } else if (computeType === ComputeType.Dataproc) {
    detachable = false;
    detachableType = null;
    gpuConfig = null;
    zone = null;

    const defaults =
      runtimePresets().hailAnalysis.runtimeTemplate.dataprocConfig;
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
    zone,
  };
};

export const toAnalysisConfig = (
  runtime: Runtime,
  existingDisk: Disk | null
): AnalysisConfig => {
  const toGceConfig = () => {
    const { machineType, diskSize, gpuConfig, zone } = runtime.gceConfig;
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
      zone,
    };
  };

  const toGceWithPdConfig = () => {
    const {
      machineType,
      persistentDisk: { size, diskType: detachableType },
      gpuConfig,
      zone,
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
      zone,
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
