import {Observable} from 'rxjs/Observable';

import {
  Cluster,
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterStatus,
  DefaultClusterResponse,
} from 'generated';

export class ClusterServiceStub {

  cluster: Cluster;

  constructor() {
    this.cluster = {
      clusterName: 'Cluster Name',
      clusterNamespace: 'Namespace',
      status: ClusterStatus.Running,
      createdDate: '08/08/2018'
    };
  }

  listClusters(extraHttpRequestParams?: any): Observable<DefaultClusterResponse> {
    return new Observable<DefaultClusterResponse>(observer => {
      setTimeout(() => {
        observer.next({defaultCluster: this.cluster});
        observer.complete();
      }, 0);
    });
  }

  localize(projectName: string, clusterName: string, req: ClusterLocalizeRequest,
    extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<ClusterLocalizeResponse>(observer => {
      setTimeout(() => {
        observer.next({
          clusterLocalDirectory: 'workspaces/${req.workspaceId}'
        });
        observer.complete();
      }, 0);
    });
  }

  deleteCluster(projectName: string, clusterName: string,
    extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<{}>(observer => {
      setTimeout(() => {
        observer.next({});
        observer.complete();
      }, 0);
    });
  }
}

