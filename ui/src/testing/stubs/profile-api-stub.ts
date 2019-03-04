import {
  BillingProjectStatus,
  DataAccessLevel,
  InvitationVerificationRequest,
  NihToken,
  Profile,
  ProfileApi
} from 'generated/fetch';

export class ProfileStubVariables {
  static PROFILE_STUB = <Profile>{
    username: 'testers',
    contactEmail: 'tester@mactesterson.edu><script>alert("hello");</script>',
    freeTierBillingProjectName: 'all-of-us-free-abcdefg',
    freeTierBillingProjectStatus: BillingProjectStatus.Ready,
    dataAccessLevel: DataAccessLevel.Registered,
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999',
    pageVisits: [{page: 'test'}],
    eraLinkedNihUsername: null,
    currentPosition: 'some',
    organization: 'here',
    areaOfResearch: 'things',
    trainingCompletionTime: null,
  };
}

// TODO: Port functionality from ProfileServiceStub as needed.
export class ProfileApiStub extends ProfileApi {

  profile: Profile;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.profile = ProfileStubVariables.PROFILE_STUB;
  }

  public invitationKeyVerification(request?: InvitationVerificationRequest, options?: any) {
    if (request.invitationKey === 'dummy') {
      const mockResponse = new Response(
          JSON.stringify({result: 'valid'}), {status: 200});
      return Promise.resolve(mockResponse);
    } else {
      const err = new Error('Invalid invitation code');
      return Promise.reject(response => { throw err; });
    }
  }

  public updateNihToken(token?: NihToken, options?: any) {
    return Promise.resolve(this.profile);
  }

  public updateProfile(updatedProfile?: Profile, options?: any) {
    this.profile = updatedProfile;
    return Promise.resolve(new Response('', {status: 200}));
  }

  public getMe(options?: any) {
    return Promise.resolve(this.profile);
  }

  public updatePageVisits(pageVisit) {
    return Promise.resolve(this.profile);
  }

  public syncTrainingStatus() {
    return Promise.resolve(this.profile);
  }

  public syncEraCommonsStatus() {
    return Promise.resolve(this.profile);
  }
}
