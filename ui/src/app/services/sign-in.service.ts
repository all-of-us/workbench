/**
 * OAuth2 via GAPI sign-in.
 */
import 'rxjs/Rx';

import {Injectable, NgZone} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {ServerConfigService} from 'app/services/server-config.service';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';

declare const gapi: any;

const SIGNED_IN_USER = 'signedInUser';

interface BasicProfile {
  id: string;
  name: string;
  givenName: string;
  familyName: string;
  imageUrl: string;
  email: string;
}

export interface SignInDetails {
  isSignedIn: boolean;
  id?: string;
  hostedDomain?: string;
  grantedScopes?: Array<string>;
  basicProfile?: BasicProfile;
  authResponse?: object;
}

@Injectable()
export class SignInService {

  // Expose the gapi.auth2 object warpped in a Promise
  public auth2: Promise<any>;
  // Expose "current user details" as an Observable
  public user: Observable<SignInDetails>;
  public currentAccessToken: string = null;
  public clientId = environment.clientId;
  constructor(private zone: NgZone,
      private router: Router,
      serverConfigService: ServerConfigService) {
    this.zone = zone;
    this.user = this.makeUserSubject();
    this.subscribeToUser();
    serverConfigService.getConfig().subscribe((config) => {
      this.auth2 = this.makeAuth2(config);
      this.subscribeToAuth2User();
    });
  }

  public signIn(): void {
    this.auth2.then(auth2 => auth2.getAuthInstance().signIn());
  }

  public signOut(): void {
    this.auth2.then(auth2 => auth2.getAuthInstance().signOut());
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
      });
    });
  }

  private makeUserSubject(): Observable<SignInDetails> {
    const initialDetailsString = window.sessionStorage.getItem(SIGNED_IN_USER);
    const initialDetails: SignInDetails = initialDetailsString ?
      JSON.parse(initialDetailsString) : {isSignedIn: false};
    return new BehaviorSubject(initialDetails);
  }

  private subscribeToAuth2User(): void {
    this.auth2.then(auth2 => {
      gapi.auth2.getAuthInstance().currentUser.listen((e: any) => {
        const currentUser = gapi.auth2.getAuthInstance().currentUser.get();
        const details = this.extractSignInDetails(currentUser);
        // Without this, Angular views won't "notice" the externally-triggered
        // event, though the Angular models will update... so the change
        // won't propagate to UI automatically. Calling `zone.run`
        // ensures Angular notices as soon as the change occurs.
        const user: BehaviorSubject<SignInDetails> = this.user as BehaviorSubject<SignInDetails>;
        this.zone.run(() => user.next(details));
      });
    });
  }

  private subscribeToUser(): void {
    this.user.subscribe(newUserDetails => {
      window.sessionStorage.setItem(SIGNED_IN_USER, JSON.stringify(newUserDetails));
      if (!newUserDetails.isSignedIn) {
        this.currentAccessToken = null;
        return;
      }
      this.currentAccessToken = newUserDetails.authResponse['access_token'];
    });
  }

  private extractSignInDetails(currentUser: any): SignInDetails {
    if (!currentUser.isSignedIn()) {
      return {
        isSignedIn: false
      };
    }

    const basicProfile = currentUser.getBasicProfile();
    return {
      isSignedIn: true,
      id: currentUser.getId(),
      hostedDomain: currentUser.getHostedDomain(),
      grantedScopes: currentUser.getGrantedScopes().split(/\s+/),
      basicProfile: {
        id: basicProfile.getId(),
        name: basicProfile.getName(),
        givenName:  basicProfile.getGivenName(),
        familyName: basicProfile.getFamilyName(),
        imageUrl: basicProfile.getImageUrl(),
        email: basicProfile.getEmail()
      },
      authResponse: currentUser.getAuthResponse()
    };
  }

}
