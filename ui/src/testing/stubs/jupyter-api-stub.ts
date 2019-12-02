import {JupyterApi, JupyterContents, Model} from 'notebooks-generated/fetch';

export class JupyterApiStub extends JupyterApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public postContents(googleProject: string, clusterName: string, workspaceDir: string,
    model?: Model, extraHttpRequestParams?: any): Promise<JupyterContents> {
    return new Promise<JupyterContents>(resolve => {
      resolve({
        type: JupyterContents.TypeEnum.Notebook,
        name: 'Untitled.ipynb',
        path: '',
        writable: true,
        created: null,
        lastModified: null,
        mimetype: null,
        content: null,
        format: null
      });
    });
  }

  public putContents(
    googleProject: string, clusterName: string, workspaceDir: string,
    newName: string, model?: Model, extraHttpRequestParams?: any): Promise<JupyterContents> {
    return new Promise<JupyterContents>(resolve => {
      resolve({
        type: JupyterContents.TypeEnum.File,
        name: newName,
        path: '',
        writable: true,
        created: null,
        lastModified: null,
        mimetype: null,
        content: '',
        format: 'text'
      });
    });
  }

}
