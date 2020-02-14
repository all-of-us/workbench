import {
  Cluster,
  ClusterApi,
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterStatus,
  CreateClusterResponse,
  GetClusterResponse,
} from 'generated/fetch';

export class ClusterApiStub extends ClusterApi {
  public cluster: Cluster;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.cluster = {
      clusterName: 'Cluster Name',
      clusterNamespace: 'Namespace',
      status: ClusterStatus.Running,
      createdDate: '08/08/2018'
    };
  }

  getCluster(extraHttpRequestParams?: any): Promise<GetClusterResponse> {
    return new Promise<GetClusterResponse>(resolve => {
      resolve({cluster: this.cluster});
    });
  }

  createCluster(extraHttpRequestParams?: any): Promise<CreateClusterResponse> {
    return new Promise<CreateClusterResponse>(resolve => {
      resolve({cluster: this.cluster});
    });
  }

  deleteCluster(projectName: string, extraHttpRequestParams?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.cluster.status = ClusterStatus.Deleting;
      resolve({});
    });
  }

  localize(projectName: string, req: ClusterLocalizeRequest,
    extraHttpRequestParams?: any): Promise<ClusterLocalizeResponse> {
    return new Promise<ClusterLocalizeResponse>(resolve => {
      resolve({clusterLocalDirectory: 'workspaces/${req.workspaceId}'});
    });
  }
}
