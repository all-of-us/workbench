import { AppsApi } from 'notebooks-generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class LeoAppsApiStub extends AppsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public startApp(
    _googleProject: string,
    _appName: string,
    _options?: any
  ): Promise<Response> {
    return Promise.resolve(new Response());
  }

  public stopApp(
    _googleProject: string,
    _appName: string,
    _options?: any
  ): Promise<Response> {
    return Promise.resolve(new Response());
  }
}
