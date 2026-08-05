import {
  WorkspaceAdminApi,
  WorkspaceRecoveryStatus,
  WorkspaceWaitingForRetrievalListResponse,
} from 'generated/fetch';

import { workspaceStubs } from './workspaces';

export class WorkspaceAdminApiStub extends WorkspaceAdminApi {
  constructor(public workspaces = workspaceStubs) {
    super(undefined);
  }

  getWorkspacesWaitingForRetrieval(): Promise<WorkspaceWaitingForRetrievalListResponse> {
    return Promise.resolve({
      items: this.workspaces
        .filter((workspace) => workspace.recoveryState === WorkspaceRecoveryStatus.REQUESTED)
        .map((workspace) => ({
          workspaceNamespace: workspace.namespace,
          workspaceName: workspace.displayName || workspace.name,
          creator: workspace.creator,
          lastModifiedTime: workspace.lastModifiedTime,
        })),
    });
  }
}
