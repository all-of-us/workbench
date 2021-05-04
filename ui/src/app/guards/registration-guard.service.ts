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
import {profileStore} from "app/utils/stores";

@Injectable()
export class RegistrationGuard implements CanActivate, CanActivateChild {
  constructor(private router: Router) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    if (!profileStore.get().profile) {
      await profileStore.get().reload();
    }
    return this.profileStoreCallback(profileStore.get().profile);
  }

  profileStoreCallback(profile: Profile): boolean {
    if (hasRegisteredAccess(profile.accessTierShortNames)) {
      return true;
    } else {
      this.router.navigate(['/']);
      return false;
    }
  }

  async canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    return this.canActivate(route, state);
  }
}
