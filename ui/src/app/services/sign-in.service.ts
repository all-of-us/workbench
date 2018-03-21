/**
 * OAuth2 via GAPI sign-in.
 */
import 'rxjs/Rx';

import {Injectable, NgZone} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {ServerConfigService} from 'app/services/server-config.service';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Observable';

declare const gapi: any;

const SIGNED_IN_USER = 'signedInUser';


@Injectable()
export class SignInService {
  // Expose "current user details" as an Observable
  private isSignedIn = new Subject<boolean>();
  public $isSignedIn: Observable<boolean>;
  // Expose "current user details" as an Observable
  private gapiInitialized = new Subject<boolean>();
  public $gapiInitialized: Observable<boolean>;
  public clientId = environment.clientId;
  constructor(private zone: NgZone,
      private router: Router,
      serverConfigService: ServerConfigService) {
    this.zone = zone;
    this.$isSignedIn = this.isSignedIn.asObservable();
    this.$gapiInitialized = this.gapiInitialized.asObservable();
    serverConfigService.getConfig().subscribe((config) => {
      this.makeAuth2(config).then(() => {
        this.subscribeToAuth2User();
      });
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
        });
        resolve(gapi.auth2);
        this.gapiInitialized.next(true);
      });
    });
  }

  private subscribeToAuth2User(): void {
    if (gapi.auth2.getAuthInstance().currentUser.get().isSignedIn()) {
      this.isSignedIn.next(true);
    }
    gapi.auth2.getAuthInstance().isSignedIn.listen((isSignedIn: boolean) => {
      this.isSignedIn.next(isSignedIn);
    });
  }

  public get currentAccessToken() {
    if (!gapi.auth2) {
      return null;
    } else {
      return gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse(true).access_token;
    }
  }
}
