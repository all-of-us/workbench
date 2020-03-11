import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router, RouterStateSnapshot} from '@angular/router';

import {profileApi} from 'app/services/swagger-fetch-clients';
import {convertAPIError} from 'app/utils/errors';
import {ErrorCode} from 'generated/fetch';

@Injectable()
export class DisabledGuard implements CanActivate, CanActivateChild {
  constructor(private router: Router) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    return profileApi().getMe().then(_ => true).catch(error => {
      return convertAPIError(error).then(errorResponse => {
        if (errorResponse.errorCode === ErrorCode.USERDISABLED) {
          this.router.navigate(['/user-disabled']);
          return false;
        } else {
          return true;
        }
      });
    });
  }

  async canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    return this.canActivate(route, state);
  }
}
