/**
 * OAuth2 via GAPI sign-in.
 */
import 'rxjs/Rx';

import {Injectable, NgZone} from '@angular/core';
import {ActivatedRouteSnapshot, NavigationEnd, Router} from '@angular/router';
import {ServerConfigService} from 'app/services/server-config.service';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';


declare const gapi: any;

const SIGNED_IN_USER = 'signedInUser';


@Injectable()
export class SignInService {
  // Expose "current user details" as an Observable
  private isSignedIn = new ReplaySubject<boolean>(1);
  public isSignedIn$ = this.isSignedIn.asObservable();
  // Expose "current user details" as an Observable
  public clientId = environment.clientId;
  constructor(private zone: NgZone,
      private router: Router,
      serverConfigService: ServerConfigService) {
    this.zone = zone;

    serverConfigService.getConfig().subscribe((config) => {
      this.makeAuth2(config);
    });
  }

  public signIn(): void {
    gapi.auth2.getAuthInstance().signIn();
  }

  public signOut(): void {
    gapi.auth2.getAuthInstance().signOut();
  }

  private makeAuth2(config: ConfigResponse): Promise<any> {
    return new Promise((resolve) => {
      gapi.load('auth2', () => {
        gapi.auth2.init({
            client_id: this.clientId,
            hosted_domain: config.gsuiteDomain,
            scope: 'https://www.googleapis.com/auth/plus.login openid profile'
        }).then(() => {
          this.subscribeToAuth2User();
        });
        resolve(gapi.auth2);
      });
    });
  }

  private subscribeToAuth2User(): void {
    // The listen below only fires on changes, so we need an initial
    // check to handle the case where the user is already signed in.
    this.zone.run(() => {
      this.isSignedIn.next(gapi.auth2.getAuthInstance().isSignedIn.get());
    });
    gapi.auth2.getAuthInstance().isSignedIn.listen((isSignedIn: boolean) => {
      this.zone.run(() => {
        this.isSignedIn.next(isSignedIn);
      });
    });
  }

  public get currentAccessToken() {
    if (!gapi.auth2) {
      return null;
    } else {
      return gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse(true).access_token;
    }
  }

  public get profileImage() {
    if (!gapi.auth2) {
      return null;
    } else {
      return gapi.auth2.getAuthInstance().currentUser.get().getBasicProfile().Paa;
    }
  }
}
