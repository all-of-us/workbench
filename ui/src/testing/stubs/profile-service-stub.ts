import {DataAccessLevel} from 'generated';
import {BillingProjectStatus, Profile} from 'generated';
import {Observable} from 'rxjs/Observable';
import {InvitationVerificationRequest} from '../../generated/model/invitationVerificationRequest';

export class ProfileStubVariables {
  static PROFILE_STUB = {
    username: 'testers!@#$%^&*()><script>alert("hello");</script>',
    contactEmail: 'tester@mactesterson.edu🀓⚚><script>alert("hello");</script>',
    enabledInFireCloud: true,
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    freeTierBillingProjectStatus: BillingProjectStatus.Ready,
    dataAccessLevel: DataAccessLevel.Registered,
    fullName:  'Tester MacTesterson><script>alert("hello");</script>',
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999',
    invitationKey: 'dummyKey'
  };
}

export class ProfileServiceStub {
  public profile: Profile;
  public accountCreates = 0;

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

  createAccount(): Observable<Profile> {
    this.accountCreates++;
    return new Observable<Profile>(observer => {
      setTimeout(() => {
        observer.next(undefined);
        observer.complete();
      }, 0);
    });
  }

  invitationKeyVerification(request?: InvitationVerificationRequest): Observable<{}> {
    if (request.invitationKey === 'dummy') {
      return new Observable(observer => { observer.next(this.profile); });
    } else {
      const err = new Error('Invalid invitation code');
      return new Observable(observer => { observer.error(err); });
    }
  }
}
