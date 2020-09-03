import {ProxyApi} from 'notebooks-generated/fetch';

export class ProxyApiStub extends ProxyApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public setCookie(googleProject: string, runtimeName: string, options?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
