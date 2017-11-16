/**
 * OAuth2 via GAPI sign-in.
 */
import 'rxjs/Rx';

import {Injectable, NgZone} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';

/* tslint:disable-next-line:no-unused-variable */
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

  constructor(private zone: NgZone) {
    this.zone = zone;
    this.auth2 = this.makeAuth2();
    this.user = this.makeUserSubject();
    this.user.subscribe(newUserDetails => {
      window.sessionStorage.setItem(SIGNED_IN_USER, JSON.stringify(newUserDetails));
      if (!newUserDetails.isSignedIn) {
        this.currentAccessToken = null;
        return;
      }
      this.currentAccessToken = newUserDetails.authResponse['access_token'];
    });
  }

  public signIn(): void {
    this.auth2.then(auth2 => auth2.getAuthInstance().signIn());
  }

  public signOut(): void {
    this.auth2.then(auth2 => auth2.getAuthInstance().signOut());
  }

  private makeAuth2(): Promise<any> {
    return new Promise((resolve) => {
      gapi.load('auth2', () => {
        gapi.auth2.init({
            client_id: '602460048110-5uk3vds3igc9qo0luevroc2uc3okgbkt.apps.googleusercontent.com',
            // hosted_domain: 'pmi-ops.org',
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
    const ret = new BehaviorSubject(initialDetails);

    this.auth2.then(auth2 => {
        gapi.auth2.getAuthInstance().currentUser.listen((e: any) => {
          const currentUser = gapi.auth2.getAuthInstance().currentUser.get();
          const details = this.extractSignInDetails(currentUser);

          // Without this, Angular views won't "notice" the externally-triggered
          // event, though the Angular models will update... so the change
          // won't propagate to UI automatically. Calling `zone.run`
          // ensures Angular notices as soon as the change occurs.
          this.zone.run(() => ret.next(details));
        });
    });
    return ret;
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
