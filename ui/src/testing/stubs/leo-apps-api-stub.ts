import { AppsApi } from 'notebooks-generated/fetch';

export class LeoAppsApiStub extends AppsApi {
  constructor() {
    super(undefined);
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
