import {
  Runtime,
  RuntimeApi,
  RuntimeConfigurationType,
  RuntimeLocalizeRequest,
  RuntimeLocalizeResponse,
  RuntimeStatus
} from 'generated/fetch';

export class RuntimeApiStub extends RuntimeApi {
  public runtime: Runtime;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.runtime = {
      runtimeName: 'Runtime Name',
      googleProject: 'Namespace',
      status: RuntimeStatus.Running,
      createdDate: '08/08/2018',
      toolDockerImage: 'docker',
      configurationType: RuntimeConfigurationType.DefaultDataproc
    };
  }

  getRuntime(workspaceNamespace: string, options?: any): Promise<Runtime> {
    return new Promise<Runtime>(resolve => {
      resolve(this.runtime);
    });
  }

  createRuntime(workspaceNamespace: string, options?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
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
