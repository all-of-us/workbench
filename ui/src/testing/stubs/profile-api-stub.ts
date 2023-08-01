import {
  AccessModule,
  AdminTableUser,
  InstitutionalRole,
  Profile,
  ProfileApi,
} from 'generated/fetch';
import { AdminUserListResponse } from 'generated/fetch/api';

import { AccessTierShortNames } from 'app/utils/access-tiers';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

import { BROAD } from './institution-api-stub';

export class ProfileStubVariables {
  static PROFILE_STUB = <Profile>{
    userId: 123,
    username: 'tester@fake-research-aou.org',
    contactEmail: 'tester@mactesterson.edu><script>alert("hello");</script>',
    accessTierShortNames: [AccessTierShortNames.Registered],
    givenName: 'Tester!@#$%^&*()><script>alert("hello");</script>',
    familyName: 'MacTesterson!@#$%^&*()><script>alert("hello");</script>',
    pageVisits: [{ page: 'test' }],
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
    duccSignedVersion: 4,
    duccSignedInitials: 'abc',
    duccCompletionTimeEpochMillis: 1,
    address: {
      streetAddress1: 'Main street',
      city: 'Cambridge',
      state: 'MA',
      country: 'USA',
      zipCode: '02142',
    },
    verifiedInstitutionalAffiliation: {
      institutionShortName: BROAD.shortName,
      institutionDisplayName: BROAD.displayName,
      institutionalRoleEnum: InstitutionalRole.FELLOW,
    },
    accessModules: {
      modules: [
        {
          moduleName: AccessModule.COMPLIANCETRAINING,
          expirationEpochMillis: undefined,
        },
        {
          moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
          expirationEpochMillis: undefined,
        },
        {
          moduleName: AccessModule.PROFILECONFIRMATION,
          expirationEpochMillis: undefined,
        },
        {
          moduleName: AccessModule.PUBLICATIONCONFIRMATION,
          expirationEpochMillis: undefined,
        },
      ],
    },
    tierEligibilities: [
      {
        accessTierShortName: AccessTierShortNames.Registered,
        eraRequired: true,
      },
      {
        accessTierShortName: AccessTierShortNames.Controlled,
        eraRequired: false,
      },
    ],
  };
  static ADMIN_TABLE_USER_STUB = <AdminTableUser>{
    ...ProfileStubVariables.PROFILE_STUB,
  };
}

// TODO: Port functionality from ProfileServiceStub as needed.
export class ProfileApiStub extends ProfileApi {
  profile: Profile;

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
    this.profile = ProfileStubVariables.PROFILE_STUB;
  }

  public createAccount(): Promise<Profile> {
    return Promise.resolve(this.profile);
  }

  public updateNihToken() {
    return Promise.resolve(this.profile);
  }

  public updateProfile(updatedProfile?: Profile) {
    this.profile = updatedProfile;
    return Promise.resolve(new Response('', { status: 200 }));
  }

  public getMe() {
    return Promise.resolve(this.profile);
  }

  public confirmProfile() {
    return Promise.resolve(new Response('', { status: 200 }));
  }

  public confirmPublications() {
    return Promise.resolve(new Response('', { status: 200 }));
  }

  public updatePageVisits() {
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

  public submitDUCC(duccVersion: number) {
    this.profile.duccSignedVersion = duccVersion;
    return Promise.resolve(this.profile);
  }

  public getAllUsers(): Promise<AdminUserListResponse> {
    return Promise.resolve({
      users: [
        {
          userId: 1,
          username: ProfileStubVariables.PROFILE_STUB.username,
        },
      ],
    });
  }
}
