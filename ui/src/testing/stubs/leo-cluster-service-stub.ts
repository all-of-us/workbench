import { Observable } from 'rxjs/Observable';

export class LeoClusterServiceStub {
  public startCluster(): Observable<{}> {
    return new Observable<{}>((observer) => {
      setTimeout(() => {
        observer.next({});
        observer.complete();
      }, 0);
    });
  }
}
