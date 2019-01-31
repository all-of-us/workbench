import {
  BillingProjectStatus,
  DataAccessLevel,
  NihToken,
  Profile,
  ProfileApi
} from 'generated/fetch';

// TODO: Port functionality from ProfileServiceStub as needed.
export class ProfileApiStub extends ProfileApi {

  profile: Profile;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.profile = <Profile>{
      username: 'testers',
      contactEmail: 'tester@mactesterson.edu🀓⚚><script>alert("hello");</script>',
      freeTierBillingProjectName: 'all-of-us-free-abcdefg',
      freeTierBillingProjectStatus: BillingProjectStatus.Ready,
      dataAccessLevel: DataAccessLevel.Registered,
      givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
      familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
      phoneNumber: '999-999-9999',
      pageVisits: [{page: 'test'}],
    };
  }

  public updateNihToken(token?: NihToken, options?: any) {
    return Promise.resolve(this.profile);
  }

}
