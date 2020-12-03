import {Runtime, RuntimeConfigurationType, RuntimeStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';

export const runtimePresets: {
  [runtimePresetName: string]: {displayName: string, runtimeTemplate: Runtime}
} = {
  generalAnalysis: {
    displayName: 'General Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      // TODO: Support specifying toolDockerImage here.
      gceConfig: {
        machineType: 'n1-standard-4',
        diskSize: 50
      },
    }
  },
  hailAnalysis: {
    displayName: 'Hail Genomics Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 50,
        workerMachineType: 'n1-standard-4',
        workerDiskSize: 50,
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
