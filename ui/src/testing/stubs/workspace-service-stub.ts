import {Observable} from 'rxjs/Observable';

import {
  EmptyResponse,
  ShareWorkspaceRequest,
  ShareWorkspaceResponse,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspaceResponseListResponse,
} from 'generated';

export class WorkspaceStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  static DEFAULT_WORKSPACE_ID = '1';
  static DEFAULT_WORKSPACE_DESCRIPTION = 'Stub workspace';
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
}

export class WorkspacesServiceStub {
  workspaces: Workspace[];
  workspaceResponses: WorkspaceResponse[];

  constructor() {
    const stubWorkspace: Workspace = {
      name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
      id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      description: WorkspaceStubVariables.DEFAULT_WORKSPACE_DESCRIPTION,
      cdrVersionId: WorkspaceStubVariables.DEFAULT_WORKSPACE_CDR_VERSION,
      creationTime: new Date().getTime(),
      lastModifiedTime: new Date().getTime(),
      researchPurpose: {
        diseaseFocusedResearch: false,
        methodsDevelopment: false,
        controlSet: false,
        aggregateAnalysis: false,
        ancestry: false,
        commercialPurpose: false,
        population: false,
        reviewRequested: false
      },
      userRoles: [
        {
          email: 'sampleuser1@fake-research-aou.org',
          role: WorkspaceAccessLevel.OWNER
        },
        {
          email: 'sampleuser2@fake-research-aou.org',
          role: WorkspaceAccessLevel.WRITER
        },
        {
          email: 'sampleuser3@fake-research-aou.org',
          role: WorkspaceAccessLevel.READER
        },
      ]
    };


    this.workspaces = [stubWorkspace];
    this.workspaceResponses = [
      {
        workspace: stubWorkspace,
        accessLevel: WorkspaceAccessLevel.OWNER
      }
    ];
  }

  createWorkspace(newWorkspace: Workspace): Observable<Workspace> {
    return new Observable<Workspace>(observer => {
      setTimeout(() => {
        observer.next(this.workspaces.find(function(workspace: Workspace) {
          if (workspace.id === newWorkspace.id) {
            observer.error(new Error(`Error creating. Workspace with `
                                    + `id: ${workspace.id} already exists.`));
            return true;
          }
        }));
        this.workspaces.push(newWorkspace);
        observer.complete();
      }, 0);
    });
  }

  deleteWorkspace(workspaceNamespace: string, workspaceId: string): Observable<EmptyResponse> {
    return new Observable<EmptyResponse>(observer => {
      setTimeout(() => {
        const deletionIndex = this.workspaces.findIndex(function(workspace: Workspace) {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (deletionIndex === -1) {
          observer.error(new Error(`Error deleting. Workspace with `
            + `id: ${workspaceId} does not exist.`));
        }
        this.workspaces.splice(deletionIndex, 1);
        observer.complete();
      }, 0);
    });
  }

  getWorkspace(workspaceNamespace: string, workspaceId: string): Observable<WorkspaceResponse> {
    return new Observable<WorkspaceResponse>(observer => {
      setTimeout(() => {
        const workspaceReceived = this.workspaces.find(function(workspace: Workspace) {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        const response: WorkspaceResponse = {
          workspace: workspaceReceived,
          accessLevel: WorkspaceAccessLevel.OWNER
        };
        observer.next(response);
        observer.complete();
      }, 0);
    });
  }

  getWorkspaces(): Observable<WorkspaceResponseListResponse> {
    return new Observable<WorkspaceResponseListResponse>(observer => {
      setTimeout(() => {
        this.workspaceResponses = [];
        this.workspaces.forEach((workspace) => {
          this.workspaceResponses.push(
            {
              workspace: workspace,
              accessLevel: WorkspaceAccessLevel.OWNER
            });
        });
        observer.next({items: this.workspaceResponses});
        observer.complete();
      }, 0);
    });
  }

  updateWorkspace(workspaceNamespace: string,
                  workspaceId: string,
                  newWorkspace: Workspace): Observable<Workspace> {
    return new Observable<Workspace>(observer => {
      setTimeout(() => {
        const updateIndex = this.workspaces.findIndex(function(workspace: Workspace) {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (updateIndex === -1) {
          const msg = `Error sharing. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
        }
        this.workspaces.splice(updateIndex, 1, newWorkspace);
        observer.complete();
      }, 0);
    });
  }

  shareWorkspace(workspaceNamespace: string,
                 workspaceId: string,
                 request: ShareWorkspaceRequest): Observable<ShareWorkspaceResponse> {
    return new Observable<ShareWorkspaceResponse>(observer => {
      setTimeout(() => {
        const updateIndex = this.workspaces.findIndex(function(workspace: Workspace) {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (updateIndex === -1) {
          const msg = `Error sharing. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
        }
        this.workspaces[updateIndex].userRoles = request.items;
        observer.next({});
        observer.complete();
      }, 0);
    });
  }
}
