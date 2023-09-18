import * as fp from 'lodash/fp';

import {
  CloneWorkspaceRequest,
  CloneWorkspaceResponse,
  EmptyResponse,
  RecentWorkspaceResponse,
  ResourceType,
  ShareWorkspaceRequest,
  UpdateWorkspaceRequest,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceBillingUsageResponse,
  WorkspaceCreatorFreeCreditsRemainingResponse,
  WorkspaceListResponse,
  WorkspaceOperation,
  WorkspaceOperationStatus,
  WorkspaceResourceResponse,
  WorkspaceResponse,
  WorkspaceResponseListResponse,
  WorkspacesApi,
  WorkspaceUserRolesResponse,
} from 'generated/fetch';

import { WorkspaceData } from 'app/utils/workspace-data';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

import { CdrVersionsStubVariables } from './cdr-versions-api-stub';
import { cohortReviewStubs } from './cohort-review-service-stub';
import { exampleCohortStubs } from './cohorts-api-stub';
import { ConceptSetsApiStub } from './concept-sets-api-stub';
import { DataSetApiStub } from './data-set-api-stub';
import { convertToResources } from './resources-stub';
import {
  recentWorkspaceStubs,
  userRolesStub,
  workspaceStubs,
  WorkspaceStubVariables,
} from './workspaces';

export class WorkspacesApiStub extends WorkspacesApi {
  public workspaces: Workspace[];
  public workspaceOperations: WorkspaceOperation[];
  public workspaceAccess: Map<string, WorkspaceAccessLevel>;
  workspaceUserRoles: Map<string, UserRole[]>;
  recentWorkspaces: RecentWorkspaceResponse;
  newWorkspaceCount = 0;
  newWorkspaceOperationCount = 0;

  constructor(workspaces?: Workspace[], workspaceUserRoles?: UserRole[]) {
    super(undefined);
    this.workspaces = fp.defaultTo(workspaceStubs, workspaces);
    this.workspaceOperations = [];
    this.workspaceAccess = new Map<string, WorkspaceAccessLevel>();
    this.workspaceUserRoles = new Map<string, UserRole[]>();
    this.workspaceUserRoles.set(
      this.workspaces[0].id,
      fp.defaultTo(userRolesStub, workspaceUserRoles)
    );
    this.recentWorkspaces = recentWorkspaceStubs;
  }

  getWorkspaces(): Promise<WorkspaceResponseListResponse> {
    return new Promise<WorkspaceResponseListResponse>((resolve) => {
      resolve({
        items: this.workspaces.map((workspace) => {
          let accessLevel: WorkspaceAccessLevel =
            WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION;
          if (this.workspaceAccess.has(workspace.id)) {
            accessLevel = this.workspaceAccess.get(workspace.id);
          }
          return {
            workspace: { ...workspace },
            accessLevel: accessLevel,
          };
        }),
      });
    });
  }

  createWorkspace(workspace?: Workspace): Promise<Workspace> {
    return new Promise((resolve) => {
      workspace.id = `created-${++this.newWorkspaceCount}`;
      this.workspaces.push(workspace);
      this.workspaceAccess.set(workspace.id, WorkspaceAccessLevel.OWNER);
      resolve(workspace);
    });
  }

  // imitate sync version by returning with immediate success
  createWorkspaceAsync(workspace?: Workspace): Promise<WorkspaceOperation> {
    return new Promise((resolve) => {
      workspace.id = `created-${++this.newWorkspaceCount}`;
      this.workspaces.push(workspace);
      this.workspaceAccess.set(workspace.id, WorkspaceAccessLevel.OWNER);
      const operation = {
        id: ++this.newWorkspaceOperationCount,
        status: WorkspaceOperationStatus.SUCCESS,
        workspace,
      };
      this.workspaceOperations.push(operation);
      resolve(operation);
    });
  }

  public getWorkspaceOperation(id: number): Promise<WorkspaceOperation> {
    return new Promise((resolve) => {
      resolve(this.workspaceOperations.find((op) => op.id === id));
    });
  }

  updateWorkspace(
    workspaceNamespace: string,
    workspaceId: string,
    body?: UpdateWorkspaceRequest
  ): Promise<Workspace> {
    return new Promise((resolve) => {
      const originalItemIndex = this.workspaces.findIndex(
        (w) => w.namespace === workspaceNamespace && w.id === workspaceId
      );
      if (originalItemIndex === -1) {
        throw new Error(
          `workspace ${workspaceNamespace}/${workspaceId} not found`
        );
      }
      this.workspaces.splice(originalItemIndex, 1);
      this.workspaces.push(body.workspace);
      resolve(body.workspace);
    });
  }

  private duplicateWorkspaceImpl(
    workspaceNamespace: string,
    workspaceId: string,
    body?: CloneWorkspaceRequest
  ): Workspace {
    const fromWorkspace = this.workspaces.find(
      (w) => w.namespace === workspaceNamespace && w.id === workspaceId
    );
    if (!fromWorkspace) {
      throw new Error(
        `workspace ${workspaceNamespace}/${workspaceId} not found`
      );
    }
    const toWorkspace = {
      ...fromWorkspace,
      ...body.workspace,
      id: `cloned-${++this.newWorkspaceCount}`,
    };
    this.workspaces.push(toWorkspace);
    this.workspaceAccess.set(toWorkspace.id, WorkspaceAccessLevel.OWNER);

    return toWorkspace;
  }

  cloneWorkspace(
    workspaceNamespace: string,
    workspaceId: string,
    body?: CloneWorkspaceRequest
  ): Promise<CloneWorkspaceResponse> {
    return new Promise((resolve) => {
      const toWorkspace = this.duplicateWorkspaceImpl(
        workspaceNamespace,
        workspaceId,
        body
      );
      resolve({ workspace: toWorkspace });
    });
  }

  // imitate sync version by returning with immediate success
  duplicateWorkspaceAsync(
    workspaceNamespace: string,
    workspaceId: string,
    body?: CloneWorkspaceRequest
  ): Promise<WorkspaceOperation> {
    return new Promise((resolve) => {
      const toWorkspace = this.duplicateWorkspaceImpl(
        workspaceNamespace,
        workspaceId,
        body
      );
      const operation = {
        id: ++this.newWorkspaceOperationCount,
        status: WorkspaceOperationStatus.SUCCESS,
        workspace: toWorkspace,
      };
      this.workspaceOperations.push(operation);
      resolve(operation);
    });
  }

  shareWorkspacePatch(
    workspaceNamespace: string,
    workspaceId: string,
    body?: ShareWorkspaceRequest
  ): Promise<WorkspaceUserRolesResponse> {
    return new Promise<WorkspaceUserRolesResponse>((resolve) => {
      const newEtag = fp.defaults(2, body.workspaceEtag + 1);
      const newItems = fp.defaults([], body.items);
      resolve({
        workspaceEtag: newEtag,
        items: newItems,
      });
    });
  }

  getWorkspace(
    workspaceNamespace: string,
    workspaceId: string
  ): Promise<WorkspaceResponse> {
    return new Promise((resolve) => {
      const ws = this.workspaces.find(
        (w) => w.namespace === workspaceNamespace && w.id === workspaceId
      );
      if (!ws) {
        throw new Error(
          `workspace ${workspaceNamespace}/${workspaceId} not found`
        );
      }
      resolve({
        workspace: ws,
        accessLevel:
          this.workspaceAccess.get(workspaceId) ||
          WorkspaceAccessLevel.NO_ACCESS,
      });
    });
  }

  deleteWorkspace(
    workspaceNamespace: string,
    workspaceId: string
  ): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve) => {
      const deletionIndex = this.workspaces.findIndex(
        (workspace: Workspace) => {
          if (workspace.id === workspaceId) {
            return true;
          }
        }
      );
      if (deletionIndex === -1) {
        throw new Error(
          'Error deleting. Workspace with ' +
            `id: ${workspaceId} does not exist.`
        );
      }
      this.workspaces.splice(deletionIndex, 1);
      resolve({});
    });
  }

  getFirecloudWorkspaceUserRoles(
    workspaceNamespace: string,
    workspaceId: string
  ): Promise<WorkspaceUserRolesResponse> {
    return new Promise<WorkspaceUserRolesResponse>((resolve) => {
      resolve({ items: this.workspaceUserRoles.get(workspaceId) });
    });
  }

  getPublishedWorkspaces(): Promise<WorkspaceResponseListResponse> {
    return new Promise<WorkspaceResponseListResponse>((resolve) => {
      const publishedWorkspaces = this.workspaces.filter(
        (w) => w.published === true
      );
      resolve({
        items: publishedWorkspaces.map((workspace) => {
          let accessLevel: WorkspaceAccessLevel =
            WorkspaceStubVariables.DEFAULT_WORKSPACE_PERMISSION;
          if (this.workspaceAccess.has(workspace.id)) {
            accessLevel = this.workspaceAccess.get(workspace.id);
          }
          return {
            workspace: { ...workspace },
            accessLevel: accessLevel,
          };
        }),
      });
    });
  }

  getWorkspacesForReview(): Promise<WorkspaceListResponse> {
    return new Promise<WorkspaceListResponse>((resolve) => {
      resolve({
        items: this.workspaces,
      });
    });
  }

  reviewWorkspace(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve) => {
      resolve({});
    });
  }

  getUserRecentWorkspaces(): Promise<RecentWorkspaceResponse> {
    return new Promise<RecentWorkspaceResponse>((resolve) => {
      resolve(recentWorkspaceStubs);
    });
  }

  updateRecentWorkspaces(): Promise<RecentWorkspaceResponse> {
    return new Promise<RecentWorkspaceResponse>((resolve) => {
      resolve(recentWorkspaceStubs);
    });
  }

  getBillingUsage(): Promise<WorkspaceBillingUsageResponse> {
    return new Promise<WorkspaceBillingUsageResponse>((resolve) => {
      resolve({ cost: 5.5 });
    });
  }

  getWorkspaceResourcesV2(
    workspaceNamespace: string,
    workspaceId: string
  ): Promise<WorkspaceResourceResponse> {
    return new Promise<WorkspaceResourceResponse>((resolve) => {
      const workspace: WorkspaceData = {
        namespace: workspaceNamespace,
        id: workspaceId,
        name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
        accessLevel: WorkspaceAccessLevel.OWNER,
        cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      };
      const workspaceResources = convertToResources(
        cohortReviewStubs,
        ResourceType.COHORT_REVIEW,
        workspace
      )
        .concat(
          convertToResources(exampleCohortStubs, ResourceType.COHORT, workspace)
        )
        .concat(
          convertToResources(
            DataSetApiStub.stubDataSets(),
            ResourceType.DATASET,
            workspace
          )
        )
        .concat(
          convertToResources(
            ConceptSetsApiStub.stubConceptSets(),
            ResourceType.CONCEPT_SET,
            workspace
          )
        );
      resolve(workspaceResources);
    });
  }

  getWorkspaceCreatorFreeCreditsRemaining(): Promise<WorkspaceCreatorFreeCreditsRemainingResponse> {
    return new Promise<WorkspaceCreatorFreeCreditsRemainingResponse>(
      (resolve) => {
        resolve({ freeCreditsRemaining: 123.4 });
      }
    );
  }

  notebookTransferComplete(): Promise<boolean> {
    return new Promise<boolean>((resolve) => resolve(true));
  }
}
