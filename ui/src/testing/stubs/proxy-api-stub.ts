import {ProxyApi} from 'notebooks-generated/fetch';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';

export class ProxyApiStub extends ProxyApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw StubImplementationRequired;
    });
  }

  public setCookie(googleProject: string, runtimeName: string, options?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
