import {Observable} from 'rxjs/Observable';

import {RecentResource} from 'generated/model/recentResource';
import {RecentResourceRequest} from 'generated/model/recentResourceRequest';
import {RecentResourceResponse} from 'generated/model/recentResourceResponse';

export class UserMetricsServiceStub {

  getUserRecentResources(extraHttpRequestParams?: any): Observable<RecentResourceResponse> {
    return new Observable<RecentResourceResponse>(observer => {
      setTimeout(() => {
        observer.complete();
      }, 0);
    });
  }

  updateRecentResource(workspaceNamespace: string, workspaceId: string,
                       recentResourceRequest: RecentResourceRequest,
                       extraHttpRequestParams?: any): Observable<RecentResource> {
    return new Observable<RecentResource>(observer => {
      setTimeout(() => {
        observer.complete();
      }, 0);
    });
  }
}
