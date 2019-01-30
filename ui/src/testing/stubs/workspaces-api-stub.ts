import {
  FileDetail,
  WorkspacesApi
} from 'generated/fetch';

export class WorkspacesApiStub extends WorkspacesApi {
  notebookList: FileDetail[];

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });

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
      setTimeout(() => {
        resolve(this.notebookList);
      }, 0);
    });
  }

  cloneNotebook(workspaceNamespace: string, workspaceId: string,
    notebookName: String): Promise<any> {
    return new Promise<any>(resolve => {
      setTimeout(() => {
        const cloneName = notebookName.replace('.ipynb', '') + ' Clone.ipynb';
        this.notebookList.push({
          'name': cloneName,
          'path': 'gs://bucket/notebooks/' + cloneName,
          'lastModifiedTime': 100
        });
        resolve({});
      });
    });
  }

  deleteNotebook(workspaceNamespace: string, workspaceId: string,
    notebookName: String): Promise<any> {
    return new Promise<any>(resolve => {
      setTimeout(() => {
        this.notebookList.pop();
      });
      resolve({});
    });
  }

}
