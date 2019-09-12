import {Observable} from 'rxjs/Observable';

import {JupyterContents, Model} from 'notebooks-generated';

export class JupyterServiceStub {

  public postContents(
    googleProject: string, clusterName: string, workspaceDir: string,
    model?: Model, extraHttpRequestParams?: any): Observable<JupyterContents> {
    return new Observable<JupyterContents>(observer => {
      setTimeout(() => {
        observer.next({
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
        observer.complete();
      }, 0);
    });
  }

  public putContents(
    googleProject: string, clusterName: string, workspaceDir: string,
    newName: string, model?: Model, extraHttpRequestParams?: any): Observable<JupyterContents> {
    return new Observable<JupyterContents>(observer => {
      setTimeout(() => {
        observer.next({
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
        observer.complete();
      }, 0);
    });
  }
}

