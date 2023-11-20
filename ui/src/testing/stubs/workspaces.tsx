import {
  BillingStatus,
  RecentWorkspace,
  RecentWorkspaceResponse,
  SpecificPopulationEnum,
  Workspace,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';

import { CdrVersionsStubVariables } from './cdr-versions-api-stub';

export class WorkspaceStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  static DEFAULT_WORKSPACE_ID = '1';
  static DEFAULT_WORKSPACE_PERMISSION = WorkspaceAccessLevel.OWNER;
  static DEFAULT_GOOGLE_PROJECT_ID = 'terra-vpc-sc-test';
  static DEFAULT_GOOGLE_BUCKET_NAME = 'fc-secure-bucket';
}

export function buildWorkspaceStub(suffix = ''): Workspace {
  return {
    name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + suffix,
    id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + suffix,
    googleProject: WorkspaceStubVariables.DEFAULT_GOOGLE_PROJECT_ID + suffix,
    googleBucketName:
      WorkspaceStubVariables.DEFAULT_GOOGLE_BUCKET_NAME + suffix,
    namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + suffix,
    cdrVersionId:
      CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID + suffix,
    accessTierShortName: AccessTierShortNames.Registered,
    creationTime: new Date().getTime(),
    lastModifiedTime: new Date().getTime(),
    researchPurpose: {
      ancestry: false,
      anticipatedFindings: '',
      commercialPurpose: false,
      controlSet: false,
      disseminateResearchFindingList: [],
      diseaseFocusedResearch: false,
      drugDevelopment: true,
      educational: true,
      intendedStudy: '',
      scientificApproach: '',
      methodsDevelopment: false,
      otherPurpose: false,
      otherPurposeDetails: '',
      populationDetails: [
        SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75,
        SpecificPopulationEnum.RACE_NHPI,
      ],
      populationHealth: true,
      researchOutcomeList: [],
      ethics: true,
      reviewRequested: false,
      socialBehavioral: false,
      reasonForAllOfUs: '',
    },
    published: false,
    billingAccountName: 'billing-account',
    billingStatus: BillingStatus.ACTIVE,
    adminLocked: false,
  };
}

export function buildWorkspaceStubs(suffixes: string[]): Workspace[] {
  return suffixes.map((suffix) => buildWorkspaceStub(suffix));
}

function buildRecentWorkspaceStub(suffix: string): RecentWorkspace {
  const workspaceStub = buildWorkspaceStub(suffix);
  return {
    workspace: workspaceStub,
    accessLevel: WorkspaceAccessLevel.OWNER,
  };
}

export function buildRecentWorkspaceResponseStub(
  suffixes: string[]
): RecentWorkspaceResponse {
  return suffixes.map((suffix) => buildRecentWorkspaceStub(suffix));
}

export const workspaceStubs = buildWorkspaceStubs(['']);

export const recentWorkspaceStubs = buildRecentWorkspaceResponseStub(['']);

export const workspaceDataStub = {
  ...workspaceStubs[0],
  accessLevel: WorkspaceAccessLevel.OWNER,
};

export const userRolesStub = [
  {
    email: 'sampleuser1@fake-research-aou.org',
    givenName: 'Sample',
    familyName: 'User1',
    role: WorkspaceAccessLevel.OWNER,
  },
  {
    email: 'sampleuser2@fake-research-aou.org',
    givenName: 'Sample',
    familyName: 'User2',
    role: WorkspaceAccessLevel.WRITER,
  },
  {
    email: 'sampleuser3@fake-research-aou.org',
    givenName: 'Sample',
    familyName: 'User3',
    role: WorkspaceAccessLevel.READER,
  },
];
