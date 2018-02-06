import {Observable} from 'rxjs/Observable';

import {Cluster, FileDetail} from 'generated';

export class ClusterServiceStub {

  cluster: Cluster;

  constructor() {
    const stubCluster: Cluster = <Cluster>{
      clusterName: 'Cluster Name',
      clusterNamespace: 'Namespace',
      status: 'Running',
      createdDate: '08/08/2018',
      labels: null
    };
    this.cluster = stubCluster;
  }

  createCluster(workspaceNamespace: string, workspaceId: string, extraHttpRequestParams?: any)
  : Observable<Cluster> {
    return new Observable<Cluster>(observer => {
      setTimeout(() => {
        observer.next(this.cluster);
        observer.complete();
      }, 0);
    });
  }

  getCluster(workspaceNamespace: string, workspaceId: string, extraHttpRequestParams?: any)
  : Observable<Cluster> {
    return new Observable<Cluster>(observer => {
      setTimeout(() => {
        observer.next(this.cluster);
        observer.complete();
      }, 0);
    });
  }

  localizeNotebook(workspaceNamespace: string, workspaceId: string, fileList: Array<FileDetail>,
      extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<{}>(observer => {
      setTimeout(() => {
        observer.next(fileList);
        observer.complete();
      }, 0);
    });
  }
}

