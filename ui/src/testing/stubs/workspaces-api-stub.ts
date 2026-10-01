import * as fp from 'lodash/fp';

import {
  RecentWorkspaceResponse,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceOperation,
  WorkspaceResponseListResponse,
  WorkspacesApi,
} from 'generated/fetch';

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

  constructor(workspaces?: Workspace[], workspaceUserRoles?: UserRole[]) {
    super(undefined);
    this.workspaces = fp.defaultTo(workspaceStubs, workspaces);
    this.workspaceOperations = [];
    this.workspaceAccess = new Map<string, WorkspaceAccessLevel>();
    this.workspaceUserRoles = new Map<string, UserRole[]>();
    this.workspaceUserRoles.set(
      this.workspaces[0].terraName,
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
          if (this.workspaceAccess.has(workspace.terraName)) {
            accessLevel = this.workspaceAccess.get(workspace.terraName);
          }
          return {
            workspace: { ...workspace },
            accessLevel: accessLevel,
          };
        }),
      });
    });
  }

  getWorkspacesWaitingForRetrieval(): Promise<WorkspaceResponseListResponse> {
    return new Promise<WorkspaceResponseListResponse>((resolve) => {
      resolve({
        items: this.workspaces
          .filter((workspace) => workspace.recoveryState === 'REQUESTED')
          .map((workspace) => ({
            workspace: { ...workspace },
            accessLevel: WorkspaceAccessLevel.OWNER,
          })),
      });
    });
  }
}
