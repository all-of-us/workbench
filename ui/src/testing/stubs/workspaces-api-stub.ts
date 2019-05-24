import {
  FileDetail, ShareWorkspaceRequest, ShareWorkspaceResponse,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponseListResponse,
  WorkspacesApi
} from 'generated/fetch';

import * as fp from 'lodash/fp';

import {CopyNotebookRequest, EmptyResponse} from 'generated';

export class WorkspaceStubVariables {
  static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  static DEFAULT_WORKSPACE_ID = '1';
  static DEFAULT_WORKSPACE_DESCRIPTION = 'Stub workspace';
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
  static DEFAULT_WORKSPACE_PERMISSION = WorkspaceAccessLevel.OWNER;
}

export const workspaceStubs = [
  {
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
      methodsDevelopment: false,
      otherPurpose: false,
      otherPurposeDetails: '',
      population: false,
      populationDetails: [],
      populationHealth: false,
      reviewRequested: false,
      socialBehavioral: false,
      softwareChoice: '',
    },
    userRoles: [
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
    ]
  }
];

export const workspaceDataStub = {
  ...workspaceStubs[0],
  accessLevel: WorkspaceAccessLevel.OWNER,
};

export class WorkspacesApiStub extends WorkspacesApi {
  public workspaces: Workspace[];
  workspaceAccess: Map<string, WorkspaceAccessLevel>;
  notebookList: FileDetail[];

  constructor(workspaces?: Workspace[]) {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.workspaces = fp.defaultTo(workspaceStubs, workspaces);
    this.workspaceAccess = new Map<string, WorkspaceAccessLevel>();
    this.notebookList = WorkspacesApiStub.stubNotebookList();
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
    notebookName: String): Promise<any> {
    return new Promise<any>(resolve => {
      const cloneName = notebookName.replace('.ipynb', '') + ' Clone.ipynb';
      this.notebookList.push({
        'name': cloneName,
        'path': 'gs://bucket/notebooks/' + cloneName,
        'lastModifiedTime': 100
      });
      resolve({});
    });
  }

  copyNotebook(fromWorkspaceNamespace: string, fromWorkspaceId: string, fromNotebookName: String,
    copyNotebookRequest: CopyNotebookRequest): Promise<any> {
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

  shareWorkspace(workspaceNamespace: string, workspaceId: string,
    body?: ShareWorkspaceRequest, options?: any): Promise<ShareWorkspaceResponse> {
    return new Promise<ShareWorkspaceResponse>(resolve => {
      const newEtag = fp.defaults(2, (body.workspaceEtag + 1));
      const newItems = fp.defaults([], body.items);
      resolve({
        workspaceEtag: newEtag, items: newItems
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

}
