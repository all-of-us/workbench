import {
  Cluster,
  ClusterApi,
  ClusterListResponse,
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterStatus,
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

  listClusters(extraHttpRequestParams?: any): Promise<ClusterListResponse> {
    return new Promise<ClusterListResponse>(resolve => {
      console.log(this.cluster);
      resolve({defaultCluster: this.cluster});
    });
  }

  deleteCluster(projectName: string, clusterName: string,
    extraHttpRequestParams?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.cluster.status = ClusterStatus.Deleting;
      resolve({});
    });
  }

  localize(projectName: string, clusterName: string, req: ClusterLocalizeRequest,
    extraHttpRequestParams?: any): Promise<ClusterLocalizeResponse> {
    return new Promise<ClusterLocalizeResponse>(resolve => {
      resolve({clusterLocalDirectory: 'workspaces/${req.workspaceId}'});
    });
  }
}
