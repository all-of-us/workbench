import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from "@angular/router";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs/Observable";
import {WorkspacesService} from "../../generated";

@Injectable()
export class WorkspaceGuard implements CanActivate, CanActivateChild {
  constructor(
    private workspaceService: WorkspacesService,
    private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    if (route.routeConfig.path === 'data' ||
        route.routeConfig.path === 'notebooks') {
    this.workspaceService.getWorkspace(route.params.ns, route.params.wsid).subscribe(workspace => {
      if (workspace.accessLevel === 'OWNER' && !workspace.workspace.researchPurpose.researchPurposeReviewed) {
        this.router.navigate(['workspaces/' + route.params.ns + '/' + route.params.wsid + '/about']);
      }
    });

    }

    return Observable.from([true]);
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}