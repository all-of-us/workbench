import {ReplaySubject} from 'rxjs/ReplaySubject';

import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

import {Profile} from 'generated/fetch';

export class ProfileStorageServiceStub {
  public profile = new ReplaySubject<Profile>(1);
  public profile$ = this.profile.asObservable();
  constructor() {}

  public reload() {
    this.profile.next(ProfileStubVariables.PROFILE_STUB);
  }
}
