import {NotebooksApi} from 'notebooks-generated/fetch';

export class NotebooksApiStub extends NotebooksApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public setCookie(googleProject: string, clusterName: string,
    options?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
