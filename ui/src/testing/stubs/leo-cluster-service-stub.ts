import {Observable} from 'rxjs/Observable';

export class LeoClusterServiceStub {

  public startCluster(googleProject: string, clusterName: string,
    extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<{}>(observer => {
      setTimeout(() => {
        observer.next({});
        observer.complete();
      }, 0);
    });
  }
}
