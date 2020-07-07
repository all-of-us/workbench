import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from '@angular/router';
import {serverConfigStore} from 'app/utils/navigation';
import {WorkspacesService} from 'generated';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class WorkspaceGuard implements CanActivate, CanActivateChild {
  constructor(
    private workspaceService: WorkspacesService,
    private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    if (serverConfigStore.getValue().enableResearchReviewPrompt && route.routeConfig.path === 'data' ||
        route.routeConfig.path === 'notebooks') {
      this.workspaceService.getWorkspace(route.params.ns, route.params.wsid).subscribe(workspace => {
        if (workspace.accessLevel === 'OWNER'
            && workspace.workspace.researchPurpose.needsReviewPrompt) {
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
