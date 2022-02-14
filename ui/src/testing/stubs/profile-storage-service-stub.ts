import { Profile } from 'generated/fetch';

import { ReplaySubject } from 'rxjs/ReplaySubject';

import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

export class ProfileStorageServiceStub {
  public profile = new ReplaySubject<Profile>(1);
  public profile$ = this.profile.asObservable();
  constructor() {}

  public reload() {
    this.profile.next(ProfileStubVariables.PROFILE_STUB);
  }
}
