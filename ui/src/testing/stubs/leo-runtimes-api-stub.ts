import {RuntimesApi} from 'notebooks-generated/fetch';


export class LeoRuntimesApiStub extends RuntimesApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public startRuntime(googleProject: string, runtimeName: string,
    extraHttpRequestParams?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }

}
