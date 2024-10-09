import {
  DataprocConfig,
  EmptyResponse,
  GceConfig,
  GceWithPdConfig,
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeLocalizeResponse,
  RuntimeStatus,
} from 'generated/fetch';

import { disksApi } from 'app/services/swagger-fetch-clients';
import {
  DATAPROC_MIN_DISK_SIZE_GB,
  MIN_DISK_SIZE_GB,
} from 'app/utils/machines';

import { DisksApiStub } from 'testing/stubs/disks-api-stub';

import { stubDisk } from './disks-api-stub';

export const defaultGceConfig = (): GceConfig => ({
  // Set the default disk size a bit over the minimum for ease of testing
  // decreases in the disk size.
  diskSize: MIN_DISK_SIZE_GB + 30,
  machineType: 'n1-standard-4',
  zone: 'us-central1-a',
});

export const defaultGceWithPdConfig = (): GceWithPdConfig => ({
  machineType: 'n1-standard-4',
  persistentDisk: stubDisk(),
  zone: 'us-central1-a',
});

export const defaultDataprocConfig = (): DataprocConfig => ({
  masterMachineType: 'n1-standard-4',
  masterDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
  workerDiskSize: DATAPROC_MIN_DISK_SIZE_GB,
  workerMachineType: 'n1-standard-4',
  numberOfWorkers: 2,
  numberOfPreemptibleWorkers: 3,
  numberOfWorkerLocalSSDs: 0,
});

const runtimeDefaults: Runtime = {
  runtimeName: 'Runtime Name',
  googleProject: 'Namespace',
  status: RuntimeStatus.RUNNING,
  createdDate: '08/08/2018',
  toolDockerImage: 'broadinstitute/terra-jupyter-aou:1.0.999',
  configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
  errors: [],
};

export const defaultGceRuntime = (): Runtime => ({
  ...runtimeDefaults,
  gceConfig: defaultGceConfig(),
});

export const defaultGceRuntimeWithPd = (): Runtime => ({
  ...runtimeDefaults,
  gceWithPdConfig: defaultGceWithPdConfig(),
});

export const defaultDataProcRuntime = (): Runtime => ({
  ...runtimeDefaults,
  dataprocConfig: defaultDataprocConfig(),
  configurationType: RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
});

export const defaultRuntime = defaultGceRuntime;

export class RuntimeApiStub extends RuntimeApi {
  public runtime: Runtime;

  constructor() {
    super(undefined);
    this.runtime = defaultRuntime();
  }

  getRuntime(): Promise<Runtime> {
    return new Promise<Runtime>((resolve) => {
      resolve(this.runtime);
    });
  }

  async createRuntime(
    workspaceNamespace: string,
    runtime: Runtime
  ): Promise<EmptyResponse> {
    const reqDisk = runtime?.gceWithPdConfig?.persistentDisk;
    if (reqDisk && !reqDisk.name) {
      const dapi = disksApi();
      if (dapi instanceof DisksApiStub) {
        dapi.disk = {
          ...stubDisk(),
          size: reqDisk.size,
          diskType: reqDisk.diskType,
        };
        reqDisk.name = dapi.disk.name;
      }
    }
    this.runtime = { ...runtime, status: RuntimeStatus.CREATING };
    return {};
  }

  async deleteRuntime(
    workspaceNamespace: string,
    deleteDisk: boolean
  ): Promise<EmptyResponse> {
    if (deleteDisk) {
      await disksApi().deleteDisk(
        workspaceNamespace,
        this.runtime.gceWithPdConfig?.persistentDisk?.name
      );
    }
    this.runtime.status = RuntimeStatus.DELETING;
    return {};
  }

  updateRuntime(): Promise<EmptyResponse> {
    return new Promise((resolve) => {
      // Setting it to Running doesn't really make sense but it reflects
      // what is currently happening in the product.
      this.runtime.status = RuntimeStatus.RUNNING;
      resolve({});
    });
  }

  localize(): Promise<RuntimeLocalizeResponse> {
    return new Promise<RuntimeLocalizeResponse>((resolve) => {
      resolve({ runtimeLocalDirectory: 'workspaces-local-dir' });
    });
  }
}
