/**
 * OAuth2 via GAPI sign-in.
 */
import {Injectable, NgZone} from '@angular/core';
import {ServerConfigService} from 'app/services/server-config.service';
import {signInStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated';
import {ReplaySubject} from 'rxjs/ReplaySubject';
import 'rxjs/Rx';


declare const gapi: any;

@Injectable()
export class SignInService {
  // Expose "current user details" as an Observable
  private isSignedIn = new ReplaySubject<boolean>(1);
  public isSignedIn$ = this.isSignedIn.asObservable();
  // Expose "current user details" as an Observable
  public clientId = environment.clientId;

  constructor(private zone: NgZone,
    serverConfigService: ServerConfigService) {
    this.zone = zone;

    serverConfigService.getConfig().subscribe((config) => {
      this.makeAuth2(config);
    });
  }

  public signIn(): void {
    gapi.auth2.getAuthInstance().signIn({
      'prompt': 'select_account',
      'ux_mode': 'redirect',
      'redirect_uri': `${window.location.protocol}//${window.location.host}`
    });
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

  private clearIdToken(): void {
    // Using the full page redirect puts a long "id_token" parameter in the
    // window hash; clear this after gapi has consumed it.
    window.location.hash = '';
  }

  private subscribeToAuth2User(): void {
    // The listen below only fires on changes, so we need an initial
    // check to handle the case where the user is already signed in.
    this.zone.run(() => {
      const isSignedIn = gapi.auth2.getAuthInstance().isSignedIn.get();
      this.isSignedIn.next(isSignedIn);
      this.nextSignInStore();
      if (isSignedIn) {
        this.clearIdToken();
      }
    });
    gapi.auth2.getAuthInstance().isSignedIn.listen((isSignedIn: boolean) => {
      this.zone.run(() => {
        this.isSignedIn.next(isSignedIn);
        this.nextSignInStore();
        if (isSignedIn) {
          this.clearIdToken();
        }
      });
    });
  }

  public get currentAccessToken() {
    if (!gapi.auth2) {
      return null;
    } else {
      const authResponse = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse(true);
      if (authResponse !== null) {
        return authResponse.access_token;
      } else {
        return null;
      }
    }
  }

  public get profileImage() {
    if (!gapi.auth2) {
      return null;
    } else {
      return gapi.auth2.getAuthInstance().currentUser.get().getBasicProfile().getImageUrl();
    }
  }

  nextSignInStore() {
    signInStore.next({
      signOut: () => this.signOut(),
      profileImage: this.profileImage,
    });
  }
}
