import {Profile} from 'generated';
import {Observable} from 'rxjs/Observable';

class ProfileStub implements Profile {
  email: string;

  contactEmail: string;

  enabledInFireCloud: boolean;

  freeTierBillingProjectName: string;

  dataAccessLevel: Profile.DataAccessLevelEnum;

  fullName: string;

  givenName: string;

  familyName: string;

  phoneNumber: string;

  constructor(profile: Profile) {
    this.email = profile.email;
    this.contactEmail = profile.contactEmail;
    this.enabledInFireCloud = profile.enabledInFireCloud;
    this.freeTierBillingProjectName = profile.freeTierBillingProjectName;
    this.dataAccessLevel = profile.dataAccessLevel;
    this.fullName = profile.fullName;
    this.givenName = profile.givenName;
    this.familyName = profile.familyName;
    this.phoneNumber = profile.phoneNumber;
  }
}

export class ProfileStubVariables {
  static PROFILE_STUB = {
    email: 'cool-cat-testers@researchallofus.org',
    contactEmail: 'tester@mactesterson.edu',
    enabledInFireCloud: true,
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    dataAccessLevel: Profile.DataAccessLevelEnum.Registered,
    fullName:  "Tester MacTesterson",
    givenName: 'Tester',
    familyName: 'MacTesterson',
    phoneNumber: '999-999-9999'
  }
}

export class ProfileServiceStub {
  public profileStub: ProfileStub;
  constructor() {
    this.profileStub = ProfileStubVariables.PROFILE_STUB;
  }

  public getMe(): Observable<Profile> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        observer.next(this.profileStub);
        observer.complete();
      }, 0);
    });
    return observable;
  }
}
