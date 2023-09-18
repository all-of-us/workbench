import { ProxyApi } from 'notebooks-generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class ProxyApiStub extends ProxyApi {
  constructor() {
    super(undefined);
  }

  public setCookie(): Promise<Response> {
    return new Promise<Response>((resolve) => {
      resolve(new Response());
    });
  }

  public connectToTerminal(): Promise<Response> {
    return new Promise<Response>((resolve) => {
      resolve(new Response());
    });
  }
}
