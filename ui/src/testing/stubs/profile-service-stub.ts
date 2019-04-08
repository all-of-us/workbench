import {Http} from '@angular/http';
import {InvitationVerificationRequest} from 'generated/model/invitationVerificationRequest';
import {Observable} from 'rxjs/Observable';

import {
  BillingProjectStatus,
  DataAccessLevel,
  PageVisit,
  Profile,
  ProfileService,
  UserListResponse,
  UsernameTakenResponse
} from 'generated';

export class ProfileStubVariables {
  static PROFILE_STUB = {
    username: 'testers',
    contactEmail: 'tester@mactesterson.edu🀓⚚><script>alert("hello");</script>',
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    freeTierBillingProjectStatus: BillingProjectStatus.Ready,
    dataAccessLevel: DataAccessLevel.Registered,
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999',
    pageVisits: [{page: 'test'}],
    linkedNihUsername: null,
    currentPosition: 'some',
    organization: 'here',
    areaOfResearch: 'things',
  };
}

export class ProfileServiceStub extends ProfileService {
  public profile: Profile;
  public accountCreates = 0;

  /**
   * Http can optionally be provided e.g. with a fake/mock. If not specified,
   * methods which are not overridden by this stub will throw errors.
   */
  constructor(http?: Http) {
    super(http, null, null);
    this.profile = ProfileStubVariables.PROFILE_STUB;
  }

  public getMe(): Observable<Profile> {
    return new Observable<Profile>(observer => {
      setTimeout(() => {
        observer.next(this.profile);
        observer.complete();
      }, 0);
    });
  }

  public createAccount(): Observable<Profile> {
    this.accountCreates++;
    return new Observable<Profile>(observer => {
      setTimeout(() => {
        observer.next(undefined);
        observer.complete();
      }, 0);
    });
  }

  public invitationKeyVerification(request?: InvitationVerificationRequest): Observable<{}> {
    if (request.invitationKey === 'dummy') {
      return new Observable(observer => { observer.next(this.profile); });
    } else {
      const err = new Error('Invalid invitation code');
      return new Observable(observer => { observer.error(err); });
    }
  }

  public isUsernameTaken(username: string, extraHttpRequestParams?: any):
  Observable<UsernameTakenResponse> {
    return new Observable(observer => {
      observer.next({
        isTaken: username === ProfileStubVariables.PROFILE_STUB.username
      });
    });
  }

  public updatePageVisits(pageVisit: PageVisit, extraHttpRequestParams?: any): Observable<Profile> {
    return Observable.from([this.profile]);
  }

  public getAllUsers(): Observable<UserListResponse> {
    return new Observable(observer => {
      observer.next({
        profileList: [ProfileStubVariables.PROFILE_STUB]
      });
    });
  }

  private now(): number {
    return Math.floor(new Date().getTime() / 1000);
  }

  public submitTermsOfService(extraHttpRequestParams?: any): Observable<Profile> {
    this.profile.termsOfServiceCompletionTime = this.now();
    return Observable.from([this.profile]);
  }

  public submitDemographicsSurvey(extraHttpRequestParams?: any): Observable<Profile> {
    this.profile.demographicSurveyCompletionTime = this.now();
    return Observable.from([this.profile]);
  }

}
