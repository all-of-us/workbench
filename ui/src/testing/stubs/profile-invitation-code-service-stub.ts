import {DataAccessLevel} from 'generated';
import {Profile} from 'generated';
import {Observable} from 'rxjs/Observable';

export class ProfileStubVariables {
  static PROFILE_STUB = {
    invitationKey : 'dummyKey'
  };
}

export class ProfileServiceStub {
  public invitationKey: String;
  constructor() {
    this.invitationKey = ProfileStubVariables.PROFILE_STUB.invitationKey;
  }

  public getMe(): Observable<Profile> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        observer.next(this.invitationKey);
        observer.complete();
      }, 0);
    });
    return observable;
  }
}
