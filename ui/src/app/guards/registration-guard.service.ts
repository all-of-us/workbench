import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router, RouterStateSnapshot
} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {hasRegisteredAccess} from 'app/utils';


@Injectable()
export class RegistrationGuard implements CanActivate, CanActivateChild {
  constructor(
    private serverConfigService: ServerConfigService,
    private profileStorageService: ProfileStorageService,
    private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    if (route.routeConfig.path === 'unregistered' ||
        route.routeConfig.path.startsWith('admin/')) {
      // Leave /admin unguarded in order to allow bootstrapping of verified users.
      return Observable.from([true]);
    }
    return this.serverConfigService.getConfig()
      .flatMap((config) => {
        if (!config.enforceRegistered) {
          return Observable.from([true]);
        }
        return this.profileStorageService.profile$
          .first()
          .map(profile => hasRegisteredAccess(profile.dataAccessLevel));
      })
      .do((ok) => {
        if (ok) {
          return;
        }
        const params = {};
        if (state.url && state.url !== '/' && !state.url.startsWith('/unregistered')) {
          params['from'] = state.url;
        }
        this.router.navigate(['/unregistered', params]);
      });
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}
