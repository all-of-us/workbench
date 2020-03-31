import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router, RouterStateSnapshot} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {convertAPIError} from 'app/utils/errors';
import {ErrorCode} from 'generated/fetch';

@Injectable()
export class DisabledGuard implements CanActivate, CanActivateChild {
  constructor(private router: Router, private signInService: SignInService) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    try {
      // The user is not necessarily authenticated at this point - we have to
      // wait for the sign-in signal before making backend requests.
      // Only grab the first element from the observable, otherwise toPromise hangs forever.
      const isSignedIn = await this.signInService.isSignedIn$.first().toPromise();
      if (!isSignedIn) {
        return false;
      }
      await profileApi().getMe();
      return true;
    } catch (e) {
      const errorResponse = await convertAPIError(e);
      if (errorResponse.errorCode === ErrorCode.USERDISABLED) {
        this.router.navigate(['/user-disabled']);
        return false;
      } else {
        return true;
      }
    }
  }

  async canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    return this.canActivate(route, state);
  }
}
