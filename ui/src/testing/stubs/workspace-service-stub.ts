import {Observable} from 'rxjs/Observable';

import {
  CloneWorkspaceRequest,
  CloneWorkspaceResponse,
  EmptyResponse,
  FileDetail,
  ShareWorkspaceRequest,
  ShareWorkspaceResponse,
  UpdateWorkspaceRequest,
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
  // By default, access is OWNER.
  workspaceAccess: Map<string, WorkspaceAccessLevel>;

  constructor() {

    this.workspaces = [WorkspacesServiceStub.stubWorkspace()];
    this.workspaceAccess = new Map<string, WorkspaceAccessLevel>();
  }

  static stubWorkspace(): Workspace {
    return {
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
        reviewRequested: false,
        containsUnderservedPopulation: false
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
  }

  private clone(w: Workspace): Workspace {
    if (w == null) {
      return w;
    }
    return JSON.parse(JSON.stringify(w));
  }

  createWorkspace(newWorkspace: Workspace): Observable<Workspace> {
    return new Observable<Workspace>(observer => {
      setTimeout(() => {
        if (this.workspaces.find(w => w.id === newWorkspace.id)) {
          observer.error(new Error(`Error creating. Workspace with `
                                   + `id: ${newWorkspace.id} already exists.`));
          return;
        }
        this.workspaces.push(this.clone(newWorkspace));
        observer.next(this.clone(newWorkspace));
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
          return;
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
        let accessLevel = WorkspaceAccessLevel.OWNER;
        if (this.workspaceAccess.has(workspaceId)) {
          accessLevel = this.workspaceAccess.get(workspaceId);
        }
        const response: WorkspaceResponse = {
          workspace: this.clone(workspaceReceived),
          accessLevel: accessLevel
        };
        observer.next(response);
        observer.complete();
      }, 0);
    });
  }

  getWorkspaces(): Observable<WorkspaceResponseListResponse> {
    return new Observable<WorkspaceResponseListResponse>(observer => {
      setTimeout(() => {
        observer.next({
          items: this.workspaces.map(workspace => {
            let accessLevel = WorkspaceAccessLevel.OWNER;
            if (this.workspaceAccess.has(workspace.id)) {
              accessLevel = this.workspaceAccess.get(workspace.id);
            }
            return {
              workspace: this.clone(workspace),
              accessLevel: accessLevel
            };
          })
        });
        observer.complete();
      }, 0);
    });
  }

  updateWorkspace(workspaceNamespace: string,
                  workspaceId: string,
                  newWorkspace: UpdateWorkspaceRequest): Observable<Workspace> {
    return new Observable<Workspace>(observer => {
      setTimeout(() => {
        const updateIndex = this.workspaces.findIndex(function(workspace: Workspace) {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (updateIndex === -1) {
          const msg = `Error updating. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
          return;
        }
        this.workspaces.splice(updateIndex, 1, this.clone(newWorkspace.workspace));
        observer.complete();
      }, 0);
    });
  }

  cloneWorkspace(workspaceNamespace: string,
                 workspaceId: string,
                 cloneReq: CloneWorkspaceRequest): Observable<CloneWorkspaceResponse> {
    return new Observable<CloneWorkspaceResponse>(observer => {
      setTimeout(() => {
        const source = this.workspaces.find(w => w.id === workspaceId);
        if (!source) {
          const msg = `Error Cloning. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
          return;
        }
        const cloned = this.clone(cloneReq.workspace);
        cloned.id = 'id-' + cloned.name;
        this.workspaces.push(cloned);
        observer.next({workspace: cloned});
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
          return;
        }
        this.workspaces[updateIndex].userRoles = request.items;
        observer.next({});
        observer.complete();
      }, 0);
    });
  }

  getNoteBookList(workspaceNamespace: string,
      workspaceId: string, extraHttpRequestParams?: any): Observable<Array<FileDetail>> {
    return new Observable<Array<FileDetail>>(observer => {
      setTimeout(() => {
        const fileDetailsList =
            [{'name': 'FileDetails', 'path': 'gs://bucket/notebooks/mockFile'}];
        observer.next(fileDetailsList);
        observer.complete();
      }, 0);
    });
  }

  localizeAllFiles(workspaceNamespace: string, workspaceId: string,
      extraHttpRequestParams?: any): Observable<{}> {
    return new Observable<{}>(observer => {
      setTimeout(() => {
        observer.next(null);
        observer.complete();
      }, 0);
    });

  }
}
