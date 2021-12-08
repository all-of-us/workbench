import {
  DataprocConfig, GceConfig,
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeLocalizeRequest,
  RuntimeLocalizeResponse,
  RuntimeStatus
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';
import {DATAPROC_WORKER_MIN_DISK_SIZE_GB, MIN_DISK_SIZE_GB} from 'app/pages/analysis/runtime-panel';

export const defaultGceConfig = (): GceConfig => ({
  diskSize: 120,
  machineType: 'n1-standard-4'
});

export const defaultDataprocConfig = (): DataprocConfig => ({
  masterMachineType: 'n1-standard-4',
  masterDiskSize: MIN_DISK_SIZE_GB,
  workerDiskSize: DATAPROC_WORKER_MIN_DISK_SIZE_GB,
  workerMachineType: 'n1-standard-4',
  numberOfWorkers: 2,
  numberOfPreemptibleWorkers: 3,
  numberOfWorkerLocalSSDs: 0
});

export const defaultRuntime = () => ({
  runtimeName: 'Runtime Name',
  googleProject: 'Namespace',
  status: RuntimeStatus.Running,
  createdDate: '08/08/2018',
  toolDockerImage: 'broadinstitute/terra-jupyter-aou:1.0.999',
  configurationType: RuntimeConfigurationType.GeneralAnalysis,
  gceConfig: defaultGceConfig(),
  errors: []
});

export class RuntimeApiStub extends RuntimeApi {
  public runtime: Runtime;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
    this.runtime = defaultRuntime();
  }

  getRuntime(workspaceNamespace: string, options?: any): Promise<Runtime> {
    return new Promise<Runtime>(resolve => {
      resolve(this.runtime);
    });
  }

  createRuntime(workspaceNamespace: string, runtime: Runtime): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.runtime = {...runtime, status: RuntimeStatus.Creating};
      resolve({});
    });
  }

  deleteRuntime(workspaceNamespace: string, options?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.runtime.status = RuntimeStatus.Deleting;
      resolve({});
    });
  }

  updateRuntime(workspaceNamespace: string, options?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      // Setting it to Running doesn't really make sense but it reflects
      // what is currently happening in the product.
      this.runtime.status = RuntimeStatus.Running;
      resolve({});
    });
  }

  localize(projectName: string, req: RuntimeLocalizeRequest,
    extraHttpRequestParams?: any): Promise<RuntimeLocalizeResponse> {
    return new Promise<RuntimeLocalizeResponse>(resolve => {
      resolve({runtimeLocalDirectory: 'workspaces/${req.workspaceId}'});
    });
  }
}
