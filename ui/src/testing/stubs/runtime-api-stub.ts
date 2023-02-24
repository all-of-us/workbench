import {
  DataprocConfig,
  GceConfig,
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
import { stubNotImplementedError } from 'testing/stubs/stub-utils';

import { stubDisk } from './disks-api-stub';

export const defaultGceConfig = (): GceConfig => ({
  // Set the default disk size a bit over the minimum for ease of testing
  // decreases in the disk size.
  diskSize: MIN_DISK_SIZE_GB + 30,
  machineType: 'n1-standard-4',
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

export const defaultRuntime = () => ({
  runtimeName: 'Runtime Name',
  googleProject: 'Namespace',
  status: RuntimeStatus.Running,
  createdDate: '08/08/2018',
  toolDockerImage: 'broadinstitute/terra-jupyter-aou:1.0.999',
  configurationType: RuntimeConfigurationType.GeneralAnalysis,
  gceConfig: defaultGceConfig(),
  errors: [],
});

export class RuntimeApiStub extends RuntimeApi {
  public runtime: Runtime;

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
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
  ): Promise<{}> {
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
    this.runtime = { ...runtime, status: RuntimeStatus.Creating };
    return {};
  }

  async deleteRuntime(
    workspaceNamespace: string,
    deleteDisk: boolean
  ): Promise<{}> {
    if (deleteDisk) {
      await disksApi().deleteDisk(
        workspaceNamespace,
        this.runtime.gceWithPdConfig?.persistentDisk?.name
      );
    }
    this.runtime.status = RuntimeStatus.Deleting;
    return {};
  }

  updateRuntime(): Promise<{}> {
    return new Promise<{}>((resolve) => {
      // Setting it to Running doesn't really make sense but it reflects
      // what is currently happening in the product.
      this.runtime.status = RuntimeStatus.Running;
      resolve({});
    });
  }

  localize(): Promise<RuntimeLocalizeResponse> {
    return new Promise<RuntimeLocalizeResponse>((resolve) => {
      resolve({ runtimeLocalDirectory: 'workspaces/${req.workspaceId}' });
    });
  }
}
