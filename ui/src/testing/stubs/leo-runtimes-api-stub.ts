import { RuntimesApi } from 'notebooks-generated/fetch';
import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class LeoRuntimesApiStub extends RuntimesApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public startRuntime(
    googleProject: string,
    runtimeName: string,
    extraHttpRequestParams?: any
  ): Promise<Response> {
    return new Promise<Response>((resolve) => {
      resolve(new Response());
    });
  }
}
