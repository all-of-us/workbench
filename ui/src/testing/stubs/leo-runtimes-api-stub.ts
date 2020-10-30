import {RuntimesApi} from 'notebooks-generated/fetch';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';

export class LeoRuntimesApiStub extends RuntimesApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw StubImplementationRequired;
    });
  }

  public startRuntime(googleProject: string, runtimeName: string,
    extraHttpRequestParams?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }

}
