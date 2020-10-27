import {Runtime, RuntimeConfigurationType} from 'generated/fetch';


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
        bootDiskSize: 50
      }
    }
  },
  hailAnalysis: {
    displayName: 'Hail Genomics Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.HailGenomicAnalysis,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 50,
        numberOfWorkers: 3
        // Take the Leo defaults for workers, currently 100GB disk and n1-standard-4
      }
    }
  }
};
