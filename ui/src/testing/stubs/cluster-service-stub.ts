import {Observable} from 'rxjs/Observable';

import {Cluster, ClusterListResponse, ClusterLocalizeRequest, ClusterStatus} from 'generated';

export class ClusterServiceStub {

  cluster: Cluster;

  constructor() {
    this.cluster = {
      clusterName: 'Cluster Name',
      clusterNamespace: 'Namespace',
      status: ClusterStatus.RUNNING,
      createdDate: '08/08/2018'
    };
  }

  listClusters(extraHttpRequestParams?: any): Observable<ClusterListResponse> {
    return new Observable<ClusterListResponse>(observer => {
      setTimeout(() => {
        observer.next({defaultCluster: this.cluster});
        observer.complete();
      }, 0);
    });
  }

  localize(projectName: string, clusterName: string, req: ClusterLocalizeRequest,
      extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<{}>(observer => {
      setTimeout(() => {
        observer.complete();
      }, 0);
    });
  }
}

