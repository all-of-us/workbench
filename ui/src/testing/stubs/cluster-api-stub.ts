import {
  Cluster,
  ClusterApi,
  ClusterListResponse,
  ClusterStatus
} from 'generated/fetch';

// TODO: Port functionality from ClusterServiceStub as needed.
export class ClusterApiStub extends ClusterApi {
  cluster: Cluster;

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
      setTimeout(() => {
        resolve({defaultCluster: this.cluster});
      }, 0);
    });
  }

  deleteCluster(projectName: string, clusterName: string,
                extraHttpRequestParams?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      setTimeout(() => {
        this.cluster.status = ClusterStatus.Deleting;
        resolve({});
      }, 0);
    });
  }
}
