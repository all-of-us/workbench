import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router, RouterStateSnapshot
} from '@angular/router';

import {Observable} from 'rxjs/Observable';


import {SignInService} from 'app/services/sign-in.service';

declare const gapi: any;

@Injectable()
export class SignInGuard implements CanActivate, CanActivateChild {
  constructor(private signInService: SignInService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return new Observable((observer) => {
      this.signInService.user.subscribe(user => {

        observer.next(user.isSignedIn);
      });
    });
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.canActivate(route, state);
  }
}
