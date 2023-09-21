import { RuntimesApi } from 'notebooks-generated/fetch';

export class RuntimesApiStub extends RuntimesApi {
  constructor() {
    super(undefined);
  }

  public startRuntime(): Promise<Response> {
    return new Promise<Response>(() => {});
  }

  public stopRuntime(): Promise<Response> {
    return new Promise<Response>(() => {});
  }
}
