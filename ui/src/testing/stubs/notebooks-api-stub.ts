import {
  appendNotebookFileSuffix,
  dropNotebookFileSuffix,
} from 'app/pages/analysis/util';
import {
  CopyRequest,
  FileDetail,
  KernelTypeEnum,
  KernelTypeResponse,
  NotebooksApi,
} from 'generated/fetch';
import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class NotebooksApiStub extends NotebooksApi {
  notebookList: FileDetail[];
  public notebookKernel: KernelTypeEnum;

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
    this.notebookList = NotebooksApiStub.stubNotebookList();
  }

  static stubNotebookList(): FileDetail[] {
    return [
      {
        name: 'mockFile.ipynb',
        path: 'gs://bucket/notebooks/mockFile.ipynb',
        lastModifiedTime: 100,
      },
    ];
  }

  getNoteBookList(
    workspaceNamespace: string,
    workspaceId: string,
    extraHttpRequestParams?: any
  ): Promise<Array<FileDetail>> {
    return new Promise<Array<FileDetail>>((resolve) => {
      resolve(this.notebookList);
    });
  }

  getNotebookKernel(
    workspaceNamespace: string,
    workspaceId: string,
    notebookName: string,
    options?: any
  ): Promise<KernelTypeResponse> {
    return new Promise<KernelTypeResponse>((resolve) => {
      resolve({
        kernelType: this.notebookKernel,
      });
    });
  }

  cloneNotebook(
    workspaceNamespace: string,
    workspaceId: string,
    notebookName: string
  ): Promise<any> {
    return new Promise<any>((resolve) => {
      const cloneName = appendNotebookFileSuffix(
        dropNotebookFileSuffix(notebookName) + ' Clone'
      );
      this.notebookList.push({
        name: cloneName,
        path: 'gs://bucket/notebooks/' + cloneName,
        lastModifiedTime: 100,
      });
      resolve({});
    });
  }

  copyNotebook(
    fromWorkspaceNamespace: string,
    fromWorkspaceId: string,
    fromNotebookName: String,
    copyRequest: CopyRequest
  ): Promise<any> {
    return new Promise<any>((resolve) => {
      resolve({});
    });
  }

  deleteNotebook(
    workspaceNamespace: string,
    workspaceId: string,
    notebookName: String
  ): Promise<any> {
    return new Promise<any>((resolve) => {
      this.notebookList.pop();
      resolve({});
    });
  }
}
