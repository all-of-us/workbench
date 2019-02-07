import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from "app/services/server-config.service";
import {Observable} from 'rxjs/Observable';

@Injectable()
export class EraCommonGuard implements CanActivate, CanActivateChild {

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
    let enforceRegistered: boolean;
    this.serverConfigService.getConfig().subscribe((config) => {
      enforceRegistered = config.enforceRegistered;
    });

    return this.profileStorageService.profile$.flatMap((profile) => {
      if ((profile.linkedNihUsername === null || profile.linkedNihUsername === '')
          && enforceRegistered) {
          this.router.navigate(['/']);
          return Observable.from([false]);
      }
      return Observable.from([true]);
    });


  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
  Observable<boolean> | Promise<boolean> | boolean {
    return this.canActivate(route, state);
  }
}
