import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from '@angular/router';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/navigation';
import {WorkspaceAccessLevel, WorkspaceResponse} from 'generated/fetch';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class WorkspaceGuard implements CanActivate, CanActivateChild {
  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    if (serverConfigStore.getValue().enableResearchReviewPrompt && route.routeConfig.path === 'data' ||
        route.routeConfig.path === 'notebooks') {
      workspacesApi().getWorkspace(route.params.ns, route.params.wsid).then((resp: WorkspaceResponse) => {
        if (resp.accessLevel === WorkspaceAccessLevel.OWNER
            && resp.workspace.researchPurpose.needsReviewPrompt) {
          this.router.navigate(
              ['workspaces/' + route.params.ns + '/' + route.params.wsid + '/about']);
        }
      });
    }

    return Observable.from([true]);
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}
