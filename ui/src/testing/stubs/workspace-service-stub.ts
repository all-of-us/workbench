import {
  ShareWorkspaceRequest,
  ShareWorkspaceResponse,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceListResponse,
  WorkspaceResponse
} from 'generated';
import {Observable} from 'rxjs/Observable';

export class WorkspaceStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  static DEFAULT_WORKSPACE_ID = '1';
  static DEFAULT_WORKSPACE_DESCRIPTION = 'Stub workspace';
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
}

export class WorkspacesServiceStub {
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
  public workspaces: Workspace[];
  public workspaceResponses: WorkspaceResponse[];

  public createWorkspace(newWorkspace: Workspace): Observable<Workspace> {
    const observable = new Observable(observer => {
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
    return observable;
  }

  public deleteWorkspace(workspaceNamespace: string, workspaceId: string): Observable<{}> {
    const observable = new Observable(observer => {
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
    return observable;
  }

  public getWorkspace(workspaceNamespace: string, workspaceId: string): Observable<Workspace> {
    const observable = new Observable(observer => {
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
    return observable;
  }

  public getWorkspaces(): Observable<WorkspaceListResponse> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        observer.next({items: this.workspaceResponses});
        observer.complete();
      }, 0);
    });
    return observable;
  }

  public updateWorkspace(workspaceNamespace: string,
      workspaceId: string,
      newWorkspace: Workspace): Observable<Workspace> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const updateIndex = this.workspaces.findIndex(function(workspace: Workspace) {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (updateIndex === -1) {
          observer.error(new Error(`Error updating. Workspace with `
                                  + `id: ${workspaceId} does not exist.`));
        }
        this.workspaces.splice(updateIndex, 1, newWorkspace);
        observer.complete();
      }, 0);
    });
    return observable;
  }

  public shareWorkspace(workspaceNamespace: string,
      workspaceId: string,
      request: ShareWorkspaceRequest): Observable<ShareWorkspaceResponse> {
        const observable = new Observable(observer => {
          setTimeout(() => {
            const updateIndex = this.workspaces.findIndex(function(workspace: Workspace) {
              if (workspace.id === workspaceId) {
                return true;
              }
            });
            if (updateIndex === -1) {
              observer.error(new Error(`Error sharing. Workspace with `
                                      + `id: ${workspaceId} does not exist.`));
            }
            this.workspaces[updateIndex].userRoles = request.items;
            observer.next({});
            observer.complete();
          }, 0);
        });
        return observable;
  }
}
