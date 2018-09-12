import {Observable} from 'rxjs/Observable';

import {UserMetricsService} from 'generated/api/userMetrics.service';
import {RecentResourceResponse} from 'generated/model/recentResourceResponse';

export class UserMetricsServiceStub extends UserMetricsService {

  getUserRecentResources(extraHttpRequestParams?: any): Observable<RecentResourceResponse> {
    return new Observable<RecentResourceResponse>(observer => {
      setTimeout(() => {
        observer.complete();
      }, 0);
    });
  }
}
