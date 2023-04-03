import * as fp from 'lodash/fp';

import { DiskType, Runtime, RuntimeConfigurationType } from 'generated/fetch';

import {
  DATAPROC_MIN_DISK_SIZE_GB,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_MACHINE_NAME,
  MIN_DISK_SIZE_GB,
} from './machines';

export const runtimePresets: {
  [runtimePresetName: string]: {
    displayName: string;
    runtimeTemplate: Runtime;
  };
} = {
  generalAnalysis: {
    displayName: 'General Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      autopauseThreshold: DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
      // TODO: Support specifying toolDockerImage here.
      gceWithPdConfig: {
        persistentDisk: {
          diskType: DiskType.Standard,
          size: MIN_DISK_SIZE_GB,
          labels: {},
          name: null,
        },
        machineType: DEFAULT_MACHINE_NAME,
        gpuConfig: null,
      },
    },
  },
  hailAnalysis: {
    displayName: 'Hail Genomics Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      autopauseThreshold: DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
      dataprocConfig: {
        masterMachineType: DEFAULT_MACHINE_NAME,
        masterDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
        workerMachineType: DEFAULT_MACHINE_NAME,
        workerDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
        numberOfWorkers: 2,
        numberOfPreemptibleWorkers: 0,
      },
    },
  },
};

export const applyPresetOverride = (runtime) => {
  if (!runtime) {
    return runtime;
  }

  const runtimePresetKey = fp
    .keys(runtimePresets)
    .find(
      (key) =>
        runtimePresets[key].runtimeTemplate.configurationType ===
        runtime.configurationType
    );

  if (runtimePresetKey) {
    const { gceConfig, gceWithPdConfig, dataprocConfig } =
      runtimePresets[runtimePresetKey].runtimeTemplate;

    return { ...runtime, gceConfig, gceWithPdConfig, dataprocConfig };
  }

  return runtime;
};
