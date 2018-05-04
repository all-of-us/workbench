import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router, RouterStateSnapshot
} from '@angular/router';

import {Observable} from 'rxjs/Observable';

import {SignInService} from 'app/services/sign-in.service';
import {navigateLogin} from 'app/utils';


declare const gapi: any;

@Injectable()
export class SignInGuard implements CanActivate, CanActivateChild {
  constructor(private signInService: SignInService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.signInService.isSignedIn$.do(
      isSignedIn => isSignedIn || navigateLogin(this.router, state.url));
  }
  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}
