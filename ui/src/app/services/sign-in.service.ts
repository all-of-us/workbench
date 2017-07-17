import 'rxjs/Rx';
import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {User} from 'app/models/user';

declare const gapi: any;

interface BasicProfile {
  id: string,
  name: string,
  givenName: string,
  familyName: string,
  imageUrl: string,
  email: string
}

export interface SignInDetails {
  isSignedIn: boolean;
  id?: string;
  hostedDomain?: string;
  grantedScopes?: Array<string>;
  basicProfile?: BasicProfile;
  authRespones?: object;
}

@Injectable()
export class SignInService {

  // Expose the gapi.auth2 object warpped in a Promise
  public auth2: Promise<any>;
  // Expose "current user details" as an Observable
  public user: Observable<SignInDetails>;

  constructor(private zone: NgZone) {
    this.zone = zone;
    this.auth2 = this.makeAuth2();
    this.user = this.makeUserSubject();
  }

  public signIn(): void {
    this.auth2.then(auth2 => auth2.getAuthInstance().signIn());
  }

  public signOut(): void {
    this.auth2.then(auth2 => auth2.getAuthInstance().signOut());
  }

  private makeAuth2(): Promise<any> {
    const zone = this.zone;
    return new Promise(function(resolve){
      gapi.load('auth2', function(){
        gapi.auth2.init({
            client_id: '887440561153-pb9gmue2cbbs2gbn9nkr35g0ifpvb8g5.apps.googleusercontent.com',
            hosted_domain: 'pmi-ops.org'
        });
        resolve(gapi.auth2);
      });
    });
  }

  private makeUserSubject(): Observable<SignInDetails> {
    const ret = new BehaviorSubject({isSignedIn: false});
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
      }
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
      authRespones: currentUser.getAuthResponse()
    }
  }

}
