import { JupyterApi, JupyterContents } from 'notebooks-generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class JupyterApiStub extends JupyterApi {
  constructor() {
    super(undefined);
  }

  public postContents(): Promise<JupyterContents> {
    return new Promise<JupyterContents>((resolve) => {
      resolve({
        type: JupyterContents.TypeEnum.Notebook,
        name: 'Untitled.ipynb',
        path: '',
        writable: true,
        created: null,
        lastModified: null,
        mimetype: null,
        content: null,
        format: null,
      });
    });
  }

  public putContents(
    googleProject: string,
    clusterName: string,
    workspaceDir: string,
    newName: string
  ): Promise<JupyterContents> {
    return new Promise<JupyterContents>((resolve) => {
      resolve({
        type: JupyterContents.TypeEnum.File,
        name: newName,
        path: '',
        writable: true,
        created: null,
        lastModified: null,
        mimetype: null,
        content: '',
        format: 'text',
      });
    });
  }
}
