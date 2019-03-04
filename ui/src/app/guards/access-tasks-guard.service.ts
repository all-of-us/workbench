import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {environment} from 'environments/environment';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class AccessTasksGuard implements CanActivate, CanActivateChild {

  constructor(
    private router: Router,
    private profileStorageService: ProfileStorageService,
    private serverConfigService: ServerConfigService,
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
  Observable<boolean> {
    if (route.routeConfig.path === '' ||
        route.routeConfig.path.startsWith('nih-callback')) {
      return Observable.from([true]);
    }
    return this.serverConfigService.getConfig().flatMap((config) => {
      return this.profileStorageService.profile$.flatMap((profile) => {
        if ((profile.eraLinkedNihUsername === null || profile.eraLinkedNihUsername === '')
            && config.enforceRegistered && environment.enableComplianceLockout) {
          this.router.navigate(['/']);
          return Observable.from([false]);
        }
        return Observable.from([true]);
      });
    });



  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
  Observable<boolean> | Promise<boolean> | boolean {
    return this.canActivate(route, state);
  }
}
