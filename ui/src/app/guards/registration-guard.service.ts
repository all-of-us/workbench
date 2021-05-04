import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router, RouterStateSnapshot
} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {Profile} from "generated/fetch";
import {ProfileStore, profileStore} from "app/utils/stores";

@Injectable()
export class RegistrationGuard implements CanActivate, CanActivateChild {
  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    if (profileStore.get().profile) {
      return this.profileStoreCallback(profileStore.get().profile);
    } else {
      profileStore.subscribe((newValue: ProfileStore) => {
        return this.profileStoreCallback(newValue.profile);
      });
    }
  }

  profileStoreCallback(profile: Profile): Observable<boolean> {
    if (hasRegisteredAccess(profile.accessTierShortNames)) {
      return Observable.from([true]);
    } else {
      this.router.navigate(['/']);
      return Observable.from([false]);
    }
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}
