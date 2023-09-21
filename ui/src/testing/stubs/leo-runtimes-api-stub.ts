import { RuntimesApi } from 'notebooks-generated/fetch';

export class LeoRuntimesApiStub extends RuntimesApi {
  constructor() {
    super(undefined);
  }

  public startRuntime(): Promise<Response> {
    return new Promise<Response>((resolve) => {
      resolve(new Response());
    });
  }
}
