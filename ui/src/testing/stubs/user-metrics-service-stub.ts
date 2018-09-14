import {Observable} from 'rxjs/Observable';

import {RecentResourceResponse} from 'generated/model/recentResourceResponse';

export class UserMetricsServiceStub {

  getUserRecentResources(extraHttpRequestParams?: any): Observable<RecentResourceResponse> {
    return new Observable<RecentResourceResponse>(observer => {
      setTimeout(() => {
        observer.complete();
      }, 0);
    });
  }
}
