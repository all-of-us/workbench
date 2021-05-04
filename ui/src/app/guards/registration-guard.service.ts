import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router, RouterStateSnapshot
} from '@angular/router';

import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {profileStore} from 'app/utils/stores';
import {Profile} from 'generated/fetch';

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
