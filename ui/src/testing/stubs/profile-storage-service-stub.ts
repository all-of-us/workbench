import {ReplaySubject} from 'rxjs/ReplaySubject';

import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';

import {Profile} from 'generated';

export class ProfileStorageServiceStub {
  private profile = new ReplaySubject<Profile>(1);
  public profile$ = this.profile.asObservable();
  constructor() {}

  public reload() {
    this.profile.next(ProfileStubVariables.PROFILE_STUB);
  }
}
