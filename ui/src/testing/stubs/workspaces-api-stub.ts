import {
  CloneWorkspaceRequest,
  CloneWorkspaceResponse,
  DataAccessLevel,
  FileDetail,
  RecentWorkspace,
  RecentWorkspaceResponse,
  ResearchPurposeReviewRequest,
  ResourceType,
  ShareWorkspaceRequest,
  SpecificPopulationEnum,
  UpdateWorkspaceRequest,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceBillingUsageResponse,
  WorkspaceListResponse,
  WorkspaceResourceResponse,
  WorkspaceResponse,
  WorkspaceResponseListResponse,
  WorkspacesApi,
  WorkspaceUserRolesResponse
} from 'generated/fetch';

import * as fp from 'lodash/fp';

import {appendNotebookFileSuffix, dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {CopyRequest, EmptyResponse} from 'generated';
import {CdrVersionsStubVariables} from './cdr-versions-api-stub';
import {cohortReviewStubs} from "./cohort-review-service-stub";
import {exampleCohortStubs} from "./cohorts-api-stub";
import {DataSetApiStub} from "./data-set-api-stub";
import {ConceptSetsApiStub} from "./concept-sets-api-stub";
import {convertToResources} from "../../app/utils/resourceActions";

export class WorkspaceStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  static DEFAULT_WORKSPACE_ID = '1';
  static DEFAULT_WORKSPACE_PERMISSION = WorkspaceAccessLevel.OWNER;
}

function buildWorkspaceStub(suffix): Workspace {
  return {
    name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + suffix,
    id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + suffix,
    namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + suffix,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID + suffix,
    creationTime: new Date().getTime(),
    lastModifiedTime: new Date().getTime(),
    researchPurpose: {
      ancestry: false,
      anticipatedFindings: '',
      commercialPurpose: false,
      controlSet: false,
      disseminateResearchFindingList: [],
      diseaseFocusedResearch: false,
      drugDevelopment: false,
      educational: false,
      intendedStudy: '',
      scientificApproach: '',
      methodsDevelopment: false,
      otherPurpose: false,
      otherPurposeDetails: '',
      populationDetails: [SpecificPopulationEnum.AGEOLDERMORETHAN75, SpecificPopulationEnum.RACENHPI],
      populationHealth: false,
      researchOutcomeList: [],
      ethics: false,
      reviewRequested: false,
      socialBehavioral: false,
      reasonForAllOfUs: '',
    },
    published: false,
    dataAccessLevel: DataAccessLevel.Registered,
    billingAccountName: 'billing-account'
  };
}

export function buildWorkspaceStubs(suffixes: string[]): Workspace[] {
  return suffixes.map(suffix => buildWorkspaceStub(suffix));
}

function buildRecentWorkspaceStub(suffix: string): RecentWorkspace {
  const workspaceStub = buildWorkspaceStub(suffix);
  return {
    workspace: workspaceStub,
    accessLevel: WorkspaceAccessLevel.OWNER
  };
}

export function buildRecentWorkspaceResponseStub(suffixes: string[]): RecentWorkspaceResponse {
  return suffixes.map(suffix => buildRecentWorkspaceStub(suffix));
}

export const workspaceStubs = buildWorkspaceStubs(['']);

export const recentWorkspaceStubs = buildRecentWorkspaceResponseStub(['']);

export const userRolesStub = [
  {
    email: 'sampleuser1@fake-research-aou.org',
    givenName: 'Sample',
    familyName: 'User1',
    role: WorkspaceAccessLevel.OWNER
  },
  {
    email: 'sampleuser2@fake-research-aou.org',
    givenName: 'Sample',
    familyName: 'User2',
    role: WorkspaceAccessLevel.WRITER
  },
  {
    email: 'sampleuser3@fake-research-aou.org',
    givenName: 'Sample',
    familyName: 'User3',
    role: WorkspaceAccessLevel.READER
  },
];

export const workspaceDataStub = {
  ...workspaceStubs[0],
  accessLevel: WorkspaceAccessLevel.OWNER,
};

export class WorkspacesApiStub extends WorkspacesApi {
  public workspaces: Workspace[];
  workspaceAccess: Map<string, WorkspaceAccessLevel>;
  notebookList: FileDetail[];
  workspaceUserRoles: Map< string, UserRole[]>;
  recentWorkspaces: RecentWorkspaceResponse;
  newWorkspaceCount = 0;

  constructor(workspaces?: Workspace[], workspaceUserRoles?: UserRole[]) {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.workspaces = fp.defaultTo(workspaceStubs, workspaces);
    this.workspaceAccess = new Map<string, WorkspaceAccessLevel>();
    this.notebookList = WorkspacesApiStub.stubNotebookList();
    this.workspaceUserRoles = new Map<string, UserRole[]>();
    this.workspaceUserRoles
      .set(this.workspaces[0].id, fp.defaultTo(userRolesStub, workspaceUserRoles));
    this.recentWorkspaces = recentWorkspaceStubs;
  }

  static stubNotebookList(): FileDetail[] {
    return [
      {
        'name': 'mockFile.ipynb',
        'path': 'gs://bucket/notebooks/mockFile.ipynb',
        'lastModifiedTime': 100
      }
    ];
  }

  getNoteBookList(workspaceNamespace: string,
    workspaceId: string, extraHttpRequestParams?: any): Promise<Array<FileDetail>> {
    return new Promise<Array<FileDetail>>(resolve => {
      resolve(this.notebookList);
    });
  }

  cloneNotebook(workspaceNamespace: string, workspaceId: string,
    notebookName: string): Promise<any> {
    return new Promise<any>(resolve => {
      const cloneName = appendNotebookFileSuffix(dropNotebookFileSuffix(notebookName) + ' Clone');
      this.notebookList.push({
        'name': cloneName,
        'path': 'gs://bucket/notebooks/' + cloneName,
        'lastModifiedTime': 100
      });
      resolve({});
    });
  }

  copyNotebook(fromWorkspaceNamespace: string, fromWorkspaceId: string, fromNotebookName: String,
    copyRequest: CopyRequest): Promise<any> {
    return new Promise<any>(resolve => {
      resolve({});
    });
  }

  deleteNotebook(workspaceNamespace: string, workspaceId: string,
    notebookName: String): Promise<any> {
    return new Promise<any>(resolve => {
      this.notebookList.pop();
      resolve({});
    });
  }

  getWorkspaces(options?: any): Promise<WorkspaceResponseListResponse> {
    return new Promise<WorkspaceResponseListResponse>(resolve => {
      resolve({
        items: this.workspaces.map(workspace => {
          let accessLevel = WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION;
          if (this.workspaceAccess.has(workspace.id)) {
            accessLevel = this.workspaceAccess.get(workspace.id);
          }
          return {
            workspace: {...workspace},
            accessLevel: accessLevel
          };
        })});
    });
  }

  createWorkspace(workspace?: Workspace, options?: any): Promise<Workspace> {
    return new Promise(resolve => {
      workspace.id = `created-${++this.newWorkspaceCount}`;
      this.workspaces.push(workspace);
      this.workspaceAccess.set(workspace.id, WorkspaceAccessLevel.OWNER);
      resolve(workspace);
    });
  }

  updateWorkspace(workspaceNamespace: string,
    workspaceId: string, body?: UpdateWorkspaceRequest, options?: any): Promise<Workspace> {
    return new Promise(resolve => {
      const originalItemIndex = this.workspaces.findIndex(w => w.namespace === workspaceNamespace && w.id === workspaceId);
      if (originalItemIndex === -1) {
        throw new Error(`workspace ${workspaceNamespace}/${workspaceId} not found`);
      }
      this.workspaces.splice(originalItemIndex, 1);
      this.workspaces.push(body.workspace);
      resolve(body.workspace);
    });
  }

  cloneWorkspace(workspaceNamespace: string,
    workspaceId: string, body?: CloneWorkspaceRequest, options?: any): Promise<CloneWorkspaceResponse> {
    return new Promise(resolve => {
      const fromWorkspace = this.workspaces.find(w => w.namespace === workspaceNamespace && w.id === workspaceId);
      if (!fromWorkspace) {
        throw new Error(`workspace ${workspaceNamespace}/${workspaceId} not found`);
      }
      const toWorkspace = {
        ...fromWorkspace,
        ...body.workspace,
        id: `cloned-${++this.newWorkspaceCount}`
      };
      this.workspaces.push(toWorkspace);
      this.workspaceAccess.set(toWorkspace.id, WorkspaceAccessLevel.OWNER);
      resolve({workspace: toWorkspace});
    });
  }

  shareWorkspace(workspaceNamespace: string, workspaceId: string,
    body?: ShareWorkspaceRequest, options?: any): Promise<WorkspaceUserRolesResponse> {
    return new Promise<WorkspaceUserRolesResponse>(resolve => {
      const newEtag = fp.defaults(2, (body.workspaceEtag + 1));
      const newItems = fp.defaults([], body.items);
      resolve({
        workspaceEtag: newEtag, items: newItems
      });
    });
  }

  getWorkspace(workspaceNamespace: string, workspaceId: string, options: any = {}): Promise<WorkspaceResponse> {
    return new Promise(resolve => {
      const ws = this.workspaces.find(w => w.namespace === workspaceNamespace && w.id === workspaceId);
      if (!ws) {
        throw new Error(`workspace ${workspaceNamespace}/${workspaceId} not found`);
      }
      resolve({
        workspace: ws,
        accessLevel: this.workspaceAccess.get(workspaceId) || WorkspaceAccessLevel.NOACCESS
      });
    });
  }

  deleteWorkspace(workspaceNamespace: string, workspaceId: string): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      const deletionIndex = this.workspaces.findIndex((workspace: Workspace) => {
        if (workspace.id === workspaceId) {
          return true;
        }
      });
      if (deletionIndex === -1) {
        throw new Error(`Error deleting. Workspace with `
          + `id: ${workspaceId} does not exist.`);
      }
      this.workspaces.splice(deletionIndex, 1);
      resolve({});
    });
  }

  getFirecloudWorkspaceUserRoles(workspaceNamespace: string, workspaceId: string):
  Promise<WorkspaceUserRolesResponse> {
    return new Promise<WorkspaceUserRolesResponse>(resolve => {
      resolve({items: this.workspaceUserRoles.get(workspaceId)});
    });
  }

  getPublishedWorkspaces(options?: any): Promise<WorkspaceResponseListResponse> {
    return new Promise<WorkspaceResponseListResponse>(resolve => {
      const publishedWorkspaces = this.workspaces.filter(w => w.published === true);
      resolve({
        items: publishedWorkspaces.map(workspace => {
          let accessLevel = WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION;
          if (this.workspaceAccess.has(workspace.id)) {
            accessLevel = this.workspaceAccess.get(workspace.id);
          }
          return {
            workspace: {...workspace},
            accessLevel: accessLevel
          };
        })
      });
    });
  }

  getWorkspacesForReview(options?: any): Promise<WorkspaceListResponse> {
    return new Promise<WorkspaceListResponse>(resolve => {
      resolve({
        items: this.workspaces
      });
    });
  }

  reviewWorkspace(workspaceNamespace: string, workspaceId: string,
    review?: ResearchPurposeReviewRequest): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      resolve({});
    });
  }

  getUserRecentWorkspaces(options?: any): Promise<RecentWorkspaceResponse> {
    return new Promise<RecentWorkspaceResponse>(resolve => {
      resolve(recentWorkspaceStubs);
    });
  }

  updateRecentWorkspaces(workspaceNamespace: string, workspaceId: string, options?: any): Promise<RecentWorkspaceResponse> {
    return new Promise<RecentWorkspaceResponse>(resolve => {
      resolve(recentWorkspaceStubs);
    });
  }

  getBillingUsage(workspaceNamespace: string, workspaceId: string): Promise<WorkspaceBillingUsageResponse> {
    return new Promise<WorkspaceBillingUsageResponse>(resolve => {
      resolve({cost: 5.5});
    });
  }

  getDataPageItems(workspaceNamespace: string, workspaceId: string): Promise<WorkspaceResourceResponse> {
    return new Promise<WorkspaceResourceResponse>(resolve => {
      const workspaceResources = convertToResources(
        cohortReviewStubs,
        workspaceNamespace,
        workspaceId,
        WorkspaceAccessLevel.OWNER,
        ResourceType.COHORTREVIEW)
        .concat(convertToResources(
          exampleCohortStubs, workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER, ResourceType.COHORT))
        .concat(convertToResources(
          DataSetApiStub.stubDataSets(), workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER, ResourceType.DATASET))
        .concat(convertToResources(
          ConceptSetsApiStub.stubConceptSets(), workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER, ResourceType.CONCEPTSET));
      resolve(workspaceResources);
    });
  }
}
