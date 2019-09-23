import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router, RouterStateSnapshot
} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {hasRegisteredAccess} from 'app/utils';


@Injectable()
export class RegistrationGuard implements CanActivate, CanActivateChild {
  constructor(
    private profileStorageService: ProfileStorageService,
    private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    if (route.routeConfig.path === 'nih-callback' ||
        route.routeConfig.path === '' ||
        route.routeConfig.path.startsWith('admin/') ||
        route.routeConfig.path.startsWith('data-use-agreement')) {
      // Leave /admin unguarded in order to allow bootstrapping of verified users.
      return Observable.from([true]);
    }
    return this.profileStorageService.profile$.flatMap((profile) => {
      if (hasRegisteredAccess(profile.dataAccessLevel)) {
        return Observable.from([true]);
      } else {
        this.router.navigate(['/']);
        return Observable.from([false]);
      }
    });
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}
