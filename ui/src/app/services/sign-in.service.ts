/**
 * OAuth2 via GAPI sign-in.
 */
import {Injectable, NgZone} from '@angular/core';
import {ServerConfigService} from 'app/services/server-config.service';
import {setLoggedInState} from 'app/utils/analytics';
import {signInStore} from 'app/utils/navigation';
import {authStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated';
import {ReplaySubject} from 'rxjs/ReplaySubject';
import 'rxjs/Rx';


declare const gapi: any;

// for e2e tests: use the SA's token to sign in here instead of interacting with Google's Capcha
declare global {
  interface Window { clientSuppliedToken: string; useToken: (token: string) => void; }
}

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

    window.useToken = (token: string) => {
      window.clientSuppliedToken = token;
      this.isSignedIn.next(true);
    };

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
          + (config.enableBillingUpgrade ? ' https://www.googleapis.com/auth/cloud-billing' : '')
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
    authStore.set({...authStore.get(), authLoaded: true, isSignedIn: gapi.auth2.getAuthInstance().isSignedIn.get()});
    this.zone.run(() => {
      const isSignedIn = gapi.auth2.getAuthInstance().isSignedIn.get();
      this.isSignedIn.next(isSignedIn);
      if (isSignedIn) {
        this.nextSignInStore();
        this.clearIdToken();
      }
      setLoggedInState(isSignedIn);
    });
    gapi.auth2.getAuthInstance().isSignedIn.listen((isSignedIn: boolean) => {
      authStore.set({...authStore.get(), isSignedIn});
      this.zone.run(() => {
        this.isSignedIn.next(isSignedIn);
        if (isSignedIn) {
          this.nextSignInStore();
          this.clearIdToken();
        }
      });
    });
  }

  public get currentAccessToken() {
    if (window.clientSuppliedToken) {
      return window.clientSuppliedToken;
    } else if (!gapi.auth2) {
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
