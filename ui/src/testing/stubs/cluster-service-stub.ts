import {Observable} from 'rxjs/Observable';

import {
  Cluster,
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterStatus,
  CreateClusterResponse,
  GetClusterResponse,
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

  getCluster(extraHttpRequestParams?: any): Observable<GetClusterResponse> {
    return new Observable<GetClusterResponse>(observer => {
      setTimeout(() => {
        observer.next({cluster: this.cluster});
        observer.complete();
      }, 0);
    });
  }

  createCluster(extraHttpRequestParams?: any): Observable<CreateClusterResponse> {
    return new Observable<CreateClusterResponse>(observer => {
      setTimeout(() => {
        observer.next({cluster: this.cluster});
        observer.complete();
      }, 0);
    });
  }

  localize(projectName: string, req: ClusterLocalizeRequest,
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

  deleteCluster(projectName: string, extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<{}>(observer => {
      setTimeout(() => {
        observer.next({});
        observer.complete();
      }, 0);
    });
  }
}

