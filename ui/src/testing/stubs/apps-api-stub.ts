import { AppsApi, EmptyResponse, ListAppsResponse } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class AppsApiStub extends AppsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public createApp(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(() => {});
  }

  public listAppsInWorkspace(): Promise<ListAppsResponse> {
    return new Promise<ListAppsResponse>(() => {});
  }

  public deleteApp(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(() => {});
  }
}
