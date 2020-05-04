import {
  AccessBypassRequest,
  AccessModule, CreateAccountRequest,
  DataAccessLevel,
  InstitutionalRole,
  InvitationVerificationRequest,
  NihToken,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import {EmptyResponse} from 'generated/fetch/api';

export class ProfileStubVariables {
  static PROFILE_STUB = <Profile>{
    username: 'testers',
    contactEmail: 'tester@mactesterson.edu><script>alert("hello");</script>',
    dataAccessLevel: DataAccessLevel.Registered,
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    phoneNumber: '999-999-9999',
    pageVisits: [{page: 'test'}],
    eraCommonsLinkedNihUsername: null,
    currentPosition: 'some',
    organization: 'here',
    areaOfResearch: 'things',
    betaAccessBypassTime: 1,
    complianceTrainingCompletionTime: null,
    complianceTrainingBypassTime: null,
    eraCommonsCompletionTime: null,
    eraCommonsBypassTime: null,
    authorities: [],
    freeTierUsage: 1.23,
    freeTierDollarQuota: 34.56,
    address: {
      streetAddress1: 'Main street',
      city: 'Cambridge',
      state: 'MA',
      country: 'USA',
      zipCode: '02142'
    },
    verifiedInstitutionalAffiliation: {
      institutionShortName: 'Broad',
      institutionDisplayName: 'The Broad Institute',
      institutionalRoleEnum: InstitutionalRole.FELLOW
    }
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

  public createAccount(request?: CreateAccountRequest, options?: any): Promise<Profile> {
    return Promise.resolve(this.profile);
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

  public requestBetaAccess(options?: any) {
    return Promise.resolve(this.profile);
  }

  public updatePageVisits(pageVisit) {
    return Promise.resolve(this.profile);
  }

  public syncComplianceTrainingStatus() {
    return Promise.resolve(this.profile);
  }

  public syncEraCommonsStatus() {
    return Promise.resolve(this.profile);
  }

  public syncTwoFactorAuthStatus() {
    return Promise.resolve(this.profile);
  }

  public bypassAccessRequirement(
    userId: number, bypassed?: AccessBypassRequest, options?: any): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      let valueToSet;
      if (bypassed.isBypassed) {
        valueToSet = 1;
      } else {
        valueToSet = null;
      }
      switch (bypassed.moduleName) {
        case AccessModule.COMPLIANCETRAINING:
          this.profile.complianceTrainingBypassTime = valueToSet;
          break;
        case AccessModule.BETAACCESS:
          this.profile.betaAccessBypassTime = valueToSet;
          break;
        case AccessModule.ERACOMMONS:
          this.profile.eraCommonsBypassTime = valueToSet;
          break;
      }
      resolve({});
    });
  }

  public submitDataUseAgreement(dataUseAgreementVersion: number) {
    this.profile.dataUseAgreementSignedVersion = dataUseAgreementVersion;
    return Promise.resolve(this.profile);
  }

  public getUser(userId: number): Promise<Profile> {
    return Promise.resolve(this.profile);
  }

}
