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

// for e2e tests: provide your own oauth token to obviate Google's oauth UI
// flow, thereby avoiding inevitable challenges as Google identifies Puppeteer
// as non-human.
declare global {
  interface Window { setTestAccessTokenOverride: (token: string) => void; }
}

const LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN = 'test-access-token-override';

@Injectable()
export class SignInService {
  // Expose "current user details" as an Observable
  private isSignedIn = new ReplaySubject<boolean>(1);
  public isSignedIn$ = this.isSignedIn.asObservable();
  // Expose "current user details" as an Observable
  public clientId = environment.clientId;
  private testAccessTokenOverride: string;

  constructor(private zone: NgZone,
    serverConfigService: ServerConfigService) {
    this.zone = zone;

    serverConfigService.getConfig().subscribe((config) => {
      // Enable test access token override via global function. Intended to support
      // Puppeteer testing flows. This is handled in the server config callback
      // for signin timing consistency. Normally we cannot sign in until we've
      // loaded the oauth client ID from the config service.
      if (environment.allowTestAccessTokenOverride) {
        window.setTestAccessTokenOverride = (token: string) => {
          if (token) {
            window.localStorage.setItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN, token);
          } else {
            window.localStorage.removeItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
          }
        };
        this.testAccessTokenOverride = window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
        if (this.testAccessTokenOverride) {
          // The client has already configured an access token override. Skip the normal oauth flow.
          authStore.set({...authStore.get(), authLoaded: true, isSignedIn: true});
          this.zone.run(() => {
            this.isSignedIn.next(true);
          });
          return;
        }
      }

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
    if (this.testAccessTokenOverride) {
      return this.testAccessTokenOverride;
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
