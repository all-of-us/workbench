import {RuntimeConfigurationType} from 'generated/fetch';

export const runtimePresets = {
  generalAnalysis: {
    displayName: 'General Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.DefaultGce,
      // TODO(RW-4848): Switch this to GCE.
      // TODO: Support specifying toolDockerImage here.
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 50
      }
    }
  },
  hailAnalysis: {
    displayName: 'Hail Genomics Analysis',
    runtimeTemplate: {
      configurationType: RuntimeConfigurationType.DefaultDataproc,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 50,
        numberOfWorkers: 3
        // Take the Leo defaults here, currently 100GB disk and n1-standard-4
      }
    }
  }
};
