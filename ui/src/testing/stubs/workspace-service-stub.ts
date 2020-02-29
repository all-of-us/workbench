import {Observable} from 'rxjs/Observable';

import {
  CloneWorkspaceRequest,
  CloneWorkspaceResponse,
  EmptyResponse,
  FileDetail,
  NotebookRename,
  ShareWorkspaceRequest,
  UpdateWorkspaceRequest,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceListResponse,
  WorkspaceResponse,
  WorkspaceResponseListResponse,
  WorkspaceUserRolesResponse
} from 'generated';
import {UserServiceStub} from './user-service-stub';

import {WorkspaceAccessLevel as WorkspaceAccessLevelFetch} from 'generated/fetch';

import {WorkspaceData} from 'app/utils/workspace-data';

export class WorkspaceStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  static DEFAULT_WORKSPACE_ID = '1';
  static DEFAULT_WORKSPACE_DESCRIPTION = 'Stub workspace';
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
}

export class WorkspacesServiceStub {
  workspaces: Workspace[];
  userService: UserServiceStub;
  // By default, access is OWNER.
  workspaceAccess: Map<string, WorkspaceAccessLevel>;
  workspacesForReview: Workspace[];
  sharingProfilesList: UserRole[] = [
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
    {
      email: 'sampleuser4@fake-research-aou.org',
      givenName: 'Sample',
      familyName: 'User4',
      role: WorkspaceAccessLevel.WRITER
    }
  ];
  notebookList: FileDetail[];
  workspaceUserRoles: Map< string, UserRole[]>;

  constructor() {
    this.userService = new UserServiceStub();
    this.workspaces = [WorkspacesServiceStub.stubWorkspace()];
    this.workspaceAccess = new Map<string, WorkspaceAccessLevel>();
    this.workspacesForReview = WorkspacesServiceStub.stubWorkspacesForReview();
    this.notebookList = WorkspacesServiceStub.stubNotebookList();
    this.workspaceUserRoles = new Map<string, UserRole[]>();
    this.workspaceUserRoles
      .set(this.workspaces[0].id, WorkspacesServiceStub.stubWorkspaceUserRoles());
  }

  static stubWorkspace(): Workspace {
    return {
      name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
      id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      cdrVersionId: WorkspaceStubVariables.DEFAULT_WORKSPACE_CDR_VERSION,
      creationTime: new Date().getTime(),
      lastModifiedTime: new Date().getTime(),
      researchPurpose: {
        ancestry: false,
        anticipatedFindings: '',
        commercialPurpose: false,
        controlSet: false,
        diseaseFocusedResearch: false,
        drugDevelopment: false,
        educational: false,
        intendedStudy: '',
        scientificApproach: '',
        methodsDevelopment: false,
        otherPurpose: false,
        otherPurposeDetails: '',
        population: false,
        populationDetails: [],
        populationHealth: false,
        ethics: false,
        reviewRequested: false,
        socialBehavioral: false,
        reasonForAllOfUs: '',
      },
    };
  }

  static stubWorkspaceUserRoles(): UserRole[] {
    return [
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
  }

  static stubWorkspaceData(): WorkspaceData {
    return {
      name: WorkspacesServiceStub.stubWorkspace().name,
      accessLevel: WorkspaceAccessLevelFetch.OWNER
    };
  }

  static stubWorkspacesForReview(): Workspace[] {
    const stubWorkspace = this.stubWorkspace();
    stubWorkspace.researchPurpose.reviewRequested = true;
    return [stubWorkspace];
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

  private duplicate(w: Workspace): Workspace {
    if (w == null) {
      return w;
    }
    return JSON.parse(JSON.stringify(w));
  }

  createWorkspace(newWorkspace: Workspace): Observable<Workspace> {
    return new Observable<Workspace>(observer => {
      setTimeout(() => {
        if (this.workspaces.find(w => w.name === newWorkspace.name)) {
          observer.error({message: 'Error', status: 409});
          return;
        } else if (this.workspaces.find(w => w.id === newWorkspace.id)) {
          observer.error(new Error(`Error creating. Workspace with `
                                   + `id: ${newWorkspace.id} already exists.`));
          return;
        }
        this.workspaces.push(this.duplicate(newWorkspace));
        observer.next(this.duplicate(newWorkspace));
        observer.complete();
      }, 0);
    });
  }

  deleteWorkspace(workspaceNamespace: string, workspaceId: string): Observable<EmptyResponse> {
    return new Observable<EmptyResponse>(observer => {
      setTimeout(() => {
        const deletionIndex = this.workspaces.findIndex((workspace: Workspace) => {
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
        const workspaceReceived = this.workspaces.find((workspace: Workspace) => {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        let accessLevel = WorkspaceAccessLevel.OWNER;
        if (this.workspaceAccess.has(workspaceId)) {
          accessLevel = this.workspaceAccess.get(workspaceId);
        }
        const response: WorkspaceResponse = {
          workspace: this.duplicate(workspaceReceived),
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
              workspace: this.duplicate(workspace),
              accessLevel: accessLevel
            };
          })
        });
        observer.complete();
      }, 0);
    });
  }

  getWorkspacesForReview(): Observable<WorkspaceListResponse> {
    return new Observable(observer => {
      observer.next({
        items: this.workspacesForReview
      });
    });
  }

  updateWorkspace(workspaceNamespace: string,
    workspaceId: string,
    newWorkspace: UpdateWorkspaceRequest): Observable<Workspace> {
    return new Observable<Workspace>(observer => {
      setTimeout(() => {
        const updateIndex = this.workspaces.findIndex((workspace: Workspace) => {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (updateIndex === -1) {
          const msg = `Error updating. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
          return;
        }
        this.workspaces.splice(updateIndex, 1, this.duplicate(newWorkspace.workspace));
        observer.complete();
      }, 0);
    });
  }

  cloneWorkspace(workspaceNamespace: string,
    workspaceId: string,
    cloneReq: CloneWorkspaceRequest): Observable<CloneWorkspaceResponse> {
    return new Observable<CloneWorkspaceResponse>(observer => {
      setTimeout(() => {
        if (cloneReq.workspace.name === WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME) {
          const msg = 'Workspace with name already exist';
          observer.error({error: new Error(msg), status: 409});
          return;
        }
        const source = this.workspaces.find(w => w.id === workspaceId);
        if (!source) {
          const msg = `Error Cloning. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
          return;
        }
        const cloned = this.duplicate(cloneReq.workspace);
        cloned.id = 'id-' + cloned.name;
        this.workspaces.push(cloned);
        observer.next({workspace: cloned});
        observer.complete();
      }, 0);
    });
  }

  shareWorkspace(workspaceNamespace: string,
    workspaceId: string,
    request: ShareWorkspaceRequest): Observable<WorkspaceUserRolesResponse> {
    return new Observable<WorkspaceUserRolesResponse>(observer => {
      setTimeout(() => {
        const updateIndex = this.workspaces.findIndex((workspace: Workspace) => {
          if (workspace.id === workspaceId) {
            return true;
          }
        });
        if (updateIndex === -1) {
          const msg = `Error sharing. Workspace with id: ${workspaceId} does not exist.`;
          observer.error(new Error(msg));
          return;
        }
        let responseItems: UserRole[] = [];
        responseItems = request.items.map(
          userRole => this.sharingProfilesList.find(
            current => userRole.email === current.email));

        observer.next({
          workspaceEtag: request.workspaceEtag,
          items: responseItems
        });
        observer.complete();
      }, 0);
    });
  }

  getNoteBookList(workspaceNamespace: string,
    workspaceId: string, extraHttpRequestParams?: any): Observable<Array<FileDetail>> {
    return new Observable<Array<FileDetail>>(observer => {
      setTimeout(() => {
        observer.next(this.notebookList);
        observer.complete();
      }, 0);
    });
  }

  renameNotebook(workspaceNamespace: string, workspaceId: string,
    rename: NotebookRename): Observable<FileDetail> {
    return new Observable<FileDetail>(observer => {
      setTimeout(() => {
        const responseItems: FileDetail = {
          'name': rename.newName,
          'path': 'gs://bucket/notebooks/' + rename.newName,
          'lastModifiedTime': 100
        };
        observer.next(responseItems);
        observer.complete();
      });
    });
  }

  cloneNotebook(workspaceNamespace: string, workspaceId: string,
    notebookName: String): Observable<any> {
    return new Observable<any>(observer => {
      setTimeout(() => {
        const cloneName = 'Duplicate of ' + notebookName;
        this.notebookList.push({
          'name': cloneName,
          'path': 'gs://bucket/notebooks/' + cloneName,
          'lastModifiedTime': 100
        });
        observer.complete();
      });
    });
  }

  deleteNotebook(workspaceNamespace: string, workspaceId: string,
    notebookName: String): Observable<any> {
    return new Observable<any>(observer => {
      setTimeout(() => {
        this.notebookList.pop();
      });
      observer.next();
      observer.complete();
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

  getFirecloudWorkspaceUserRoles(workspaceNamespace: string, workspaceId: string): Observable<any> {
    return new Observable<any>(observer => {
      setTimeout(() => {
        observer.next(this.workspaceUserRoles.get(workspaceId));
        observer.complete();
      });
    });
  }
}
