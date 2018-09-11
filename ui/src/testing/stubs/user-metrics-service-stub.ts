import {Observable} from 'rxjs/Observable';
import {RecentResource} from "../../generated/model/recentResource";
import {FileDetail} from "../../generated/model/fileDetail";
import {Timestamp} from "rxjs/Rx";
import {ResponseOptions} from "@angular/http";
import {EmptyResponse} from "../../generated/model/emptyResponse";
import {RecentResourceResponse} from "../../generated/model/recentResourceResponse";
import {UserMetricsService} from "../../generated/api/userMetrics.service";

export class UserMetricsStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_FC_NAME = 'defaultFCName';
  static DEFAULT_PERMISSION = 'Owner';
  static DEFAULT_WORKSPACE_ID = 'defaultWorkspaceId';
}

export class UserMetricsServiceStub {

  getUserRecentResources(extraHttpRequestParams?: any): Observable<RecentResourceResponse> {
    return new Observable<RecentResourceResponse>(observer => {
      setTimeout(() => {
        observer.complete();
      }, 0);
    });
  }

  public stubRecentResources(numberOfResources: number): RecentResource[] {
    const currentCache = [];
    while (numberOfResources > 0) {
      const currentResource = {
        workspaceId: numberOfResources,
        workspaceNamespace: UserMetricsStubVariables.DEFAULT_WORKSPACE_NS + numberOfResources,
        workspaceFirecloudName: UserMetricsStubVariables.DEFAULT_FC_NAME + numberOfResources,
        permission: UserMetricsStubVariables.DEFAULT_PERMISSION,
        notebook: {
          'name': 'mockFile' + numberOfResources + '.ipynb',
          'path': 'gs://bucket/notebooks/mockFile.ipynb',
          'lastModifiedTime': 100
        },
        lastModified: Date.now()
      };
      currentCache.push(currentResource);
      numberOfResources--;
    }
    return currentCache;
  };
}
