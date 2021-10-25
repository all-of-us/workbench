import {ProxyApi} from 'notebooks-generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export class ProxyApiStub extends ProxyApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public setCookie(googleProject: string, runtimeName: string, options?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }

  public connectToTerminal(googleProject: string, runtimeName: string, terminalName: string): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }
}
