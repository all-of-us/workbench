import { ProxyApi } from 'notebooks-generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class LeoProxyApiStub extends ProxyApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public setCookie(_options?: any): Promise<Response> {
    return new Promise<Response>((resolve) => {
      resolve(new Response());
    });
  }
}
