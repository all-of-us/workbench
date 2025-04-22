import { DiskType, Runtime } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

import {
  DATAPROC_MIN_DISK_SIZE_GB,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_MACHINE_NAME,
  MIN_DISK_SIZE_GB,
} from './machines';

interface RuntimePreset {
  displayName: string;
  runtimeTemplate: Runtime;
}
export const runtimePresets = (): {
  [runtimePresetName: string]: RuntimePreset;
} => {
  return {
    generalAnalysis: {
      displayName: 'General Analysis',
      runtimeTemplate: {
        autopauseThreshold: DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
        // TODO: Support specifying toolDockerImage here.
        gceWithPdConfig: {
          persistentDisk: {
            diskType: DiskType.STANDARD,
            size: MIN_DISK_SIZE_GB,
            labels: {},
            name: null, // TODO: why not undefined or simply missing?
          },
          machineType: DEFAULT_MACHINE_NAME,
          gpuConfig: null, // TODO: why not undefined or simply missing?
          zone: serverConfigStore.get().config?.defaultGceVmZone,
        },
      },
    },
    hailAnalysis: {
      displayName: 'Hail Genomics Analysis',
      runtimeTemplate: {
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
};
