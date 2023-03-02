import { RuntimesApi } from 'notebooks-generated/fetch';

import { stubNotImplementedError } from './stub-utils';

export class RuntimesApiStub extends RuntimesApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public startRuntime(): Promise<Response> {
    return new Promise<Response>(() => {});
  }

  public stopRuntime(): Promise<Response> {
    return new Promise<Response>(() => {});
  }
}
