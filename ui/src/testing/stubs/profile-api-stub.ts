import {
  AccessBypassRequest,
  AccessModule,
  AdminTableUser,
  CreateAccountRequest,
  InstitutionalRole,
  NihToken,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import {AccessTierShortNames} from 'app/utils/access-tiers';
import {AdminUserListResponse, EmptyResponse} from 'generated/fetch/api';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export class ProfileStubVariables {
  static PROFILE_STUB = <Profile>{
    username: 'tester@fake-research-aou.org',
    contactEmail: 'tester@mactesterson.edu><script>alert("hello");</script>',
    accessTierShortNames: [AccessTierShortNames.Registered],
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    pageVisits: [{page: 'test'}],
    eraCommonsLinkedNihUsername: null,
    areaOfResearch: 'things',
    complianceTrainingCompletionTime: null,
    complianceTrainingBypassTime: null,
    eraCommonsCompletionTime: null,
    eraCommonsBypassTime: null,
    rasLinkLoginGovBypassTime: null,
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
    },
    accessModules : {
      modules: [{
        moduleName: AccessModule.COMPLIANCETRAINING,
        expirationEpochMillis: undefined
      }, {
        moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
        expirationEpochMillis: undefined
      }, {
        moduleName: AccessModule.PROFILECONFIRMATION,
        expirationEpochMillis: undefined
      }, {
        moduleName: AccessModule.PUBLICATIONCONFIRMATION,
        expirationEpochMillis: undefined
      }],
      anyModuleHasExpired: false
    }
  };
  static ADMIN_TABLE_USER_STUB = <AdminTableUser>{...ProfileStubVariables.PROFILE_STUB};
}

// TODO: Port functionality from ProfileServiceStub as needed.
export class ProfileApiStub extends ProfileApi {

  profile: Profile;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
    this.profile = ProfileStubVariables.PROFILE_STUB;
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

  public confirmProfile(options?: any) {
    return Promise.resolve(new Response('', {status: 200}));
  }

  public confirmPublications(options?: any) {
    return Promise.resolve(new Response('', {status: 200}));
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
    return new Promise<EmptyResponse>(() => {});
  }

  public submitDUCC(duccVersion: number) {
    this.profile.dataUseAgreementSignedVersion = duccVersion;
    return Promise.resolve(this.profile);
  }

  public getUser(userId: number): Promise<Profile> {
    return Promise.resolve(this.profile);
  }

  public getAllUsers(): Promise<AdminUserListResponse> {
    return Promise.resolve({users: [{
      userId: 1,
      username: ProfileStubVariables.PROFILE_STUB.username,
    }]});
  }
}
