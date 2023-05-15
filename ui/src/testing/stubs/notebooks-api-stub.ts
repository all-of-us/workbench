import {
  FileDetail,
  KernelTypeEnum,
  KernelTypeResponse,
  NotebooksApi,
} from 'generated/fetch';

import {
  appendJupyterNotebookFileSuffix,
  dropJupyterNotebookFileSuffix,
} from 'app/pages/analysis/util';

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
        lastModifiedBy: 'stubUser@fake-researcher.aou',
      },
    ];
  }

  getNoteBookList(): Promise<Array<FileDetail>> {
    return new Promise<Array<FileDetail>>((resolve) => {
      resolve(this.notebookList);
    });
  }

  getNotebookKernel(): Promise<KernelTypeResponse> {
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
      const cloneName = appendJupyterNotebookFileSuffix(
        dropJupyterNotebookFileSuffix(notebookName) + ' Clone'
      );
      this.notebookList.push({
        name: cloneName,
        path: 'gs://bucket/notebooks/' + cloneName,
        lastModifiedTime: 100,
      });
      resolve({});
    });
  }

  copyNotebook(): Promise<any> {
    return new Promise<any>((resolve) => {
      resolve({});
    });
  }

  deleteNotebook(): Promise<any> {
    return new Promise<any>((resolve) => {
      this.notebookList.pop();
      resolve({});
    });
  }
}
