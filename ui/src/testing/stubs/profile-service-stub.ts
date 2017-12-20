import {DataAccessLevel} from 'generated';
import {Profile} from 'generated';
import {Observable} from 'rxjs/Observable';

export class ProfileStubVariables {
  static PROFILE_STUB = {
    username: 'testers!@#$%^&*()><script>alert("hello");</script>',
    contactEmail: 'tester@mactesterson.edu🀓⚚><script>alert("hello");</script>',
    enabledInFireCloud: true,
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    dataAccessLevel: DataAccessLevel.Registered,
    fullName:  'Tester MacTesterson><script>alert("hello");</script>',
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999'
  };
}

export class ProfileServiceStub {
  public profile: Profile;

  constructor() {
    this.profile = ProfileStubVariables.PROFILE_STUB;
  }

  getMe(): Observable<Profile> {
    return new Observable<Profile>(observer => {
      setTimeout(() => {
        observer.next(this.profile);
        observer.complete();
      }, 0);
    });
  }
}
