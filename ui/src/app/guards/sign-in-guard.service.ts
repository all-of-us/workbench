import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router, RouterStateSnapshot
} from '@angular/router';

import {Observable} from 'rxjs/Observable';


import {SignInService} from 'app/services/sign-in.service';

declare const gapi: any;

@Injectable()
export class SignInGuard implements CanActivate {
  constructor(private signInService: SignInService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.signInService.user.pluck('isSignedIn');

  }
}
