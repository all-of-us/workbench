import {
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeLocalizeRequest,
  RuntimeLocalizeResponse,
  RuntimeStatus
} from 'generated/fetch';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';

export class RuntimeApiStub extends RuntimeApi {
  public runtime: Runtime;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw StubImplementationRequired; });
    this.runtime = {
      runtimeName: 'Runtime Name',
      googleProject: 'Namespace',
      status: RuntimeStatus.Running,
      createdDate: '08/08/2018',
      toolDockerImage: 'broadinstitute/terra-jupyter-aou:1.0.999',
      configurationType: RuntimeConfigurationType.GeneralAnalysis,
      dataprocConfig: {
        masterMachineType: 'n1-standard-4',
        masterDiskSize: 80,
        numberOfWorkers: 0
      }
    };
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

  localize(projectName: string, req: RuntimeLocalizeRequest,
    extraHttpRequestParams?: any): Promise<RuntimeLocalizeResponse> {
    return new Promise<RuntimeLocalizeResponse>(resolve => {
      resolve({runtimeLocalDirectory: 'workspaces/${req.workspaceId}'});
    });
  }
}
