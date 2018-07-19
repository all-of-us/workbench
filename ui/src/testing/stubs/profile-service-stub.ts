import {DataAccessLevel} from 'generated';
import {BillingProjectStatus, Profile} from 'generated';
import {Observable} from 'rxjs/Observable';
import {InvitationVerificationRequest} from '../../generated/model/invitationVerificationRequest';

export class ProfileStubVariables {
  static PROFILE_STUB = {
    username: 'testers',
    contactEmail: 'tester@mactesterson.edu🀓⚚><script>alert("hello");</script>',
    enabledInFireCloud: true,
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    freeTierBillingProjectStatus: BillingProjectStatus.Ready,
    dataAccessLevel: DataAccessLevel.Registered,
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999',
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

  isUsernameTaken(username: string): Observable<boolean> {
    if (username === ProfileStubVariables.PROFILE_STUB.username) {
      return new Observable(observer => { observer.next(true); });
    } else {
      return new Observable(observer => { observer.next(false); });
    }
  }

  private now(): number {
    return Math.floor(new Date().getTime() / 1000);
  }

  public submitIdVerification(extraHttpRequestParams?: any): Observable<Profile> {
    this.profile.requestedIdVerification = true;
    return Observable.from([this.profile]);
  }

  public submitTermsOfService(extraHttpRequestParams?: any): Observable<Profile> {
    this.profile.termsOfServiceCompletionTime = this.now();
    return Observable.from([this.profile]);
  }

  public submitDemographicsSurvey(extraHttpRequestParams?: any): Observable<Profile> {
    this.profile.demographicSurveyCompletionTime = this.now();
    return Observable.from([this.profile]);
  }

  public completeEthicsTraining(extraHttpRequestParams?: any): Observable<Profile> {
    this.profile.ethicsTrainingCompletionTime = this.now();
    return Observable.from([this.profile]);
  }
}
