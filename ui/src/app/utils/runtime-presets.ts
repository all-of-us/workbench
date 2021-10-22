import {Runtime, RuntimeConfigurationType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES} from './machines';

export const runtimePresets: {
  [runtimePresetName: string]: {displayName: string, runtimeTemplate: Runtime}
} = {
  generalAnalysis: {
    displayName: 'General Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      autopauseThreshold: DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
      // TODO: Support specifying toolDockerImage here.
      gceConfig: {
        machineType: 'n1-standard-4',
        diskSize: 100,
        gpuConfig: null,
      },
    }
  },
  hailAnalysis: {
    displayName: 'Hail Genomics Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      autopauseThreshold: DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 100,
        workerMachineType: 'n1-standard-4',
        workerDiskSize: 100,
        numberOfWorkers: 2,
        numberOfPreemptibleWorkers: 0
      }
    }
  }
};

export const applyPresetOverride = (runtime) => {
  if (!runtime) {
    return runtime;
  }

  const newRuntime = {...runtime};

  const runtimePresetKey = fp.keys(runtimePresets)
    .find(key => runtimePresets[key].runtimeTemplate.configurationType === newRuntime.configurationType);

  if (runtimePresetKey) {
    newRuntime.gceConfig = runtimePresets[runtimePresetKey].runtimeTemplate.gceConfig;
    newRuntime.dataprocConfig = runtimePresets[runtimePresetKey].runtimeTemplate.dataprocConfig;
  }

  return newRuntime;
};
