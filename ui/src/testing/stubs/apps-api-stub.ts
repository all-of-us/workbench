import { AppsApi, EmptyResponse, ListAppsResponse } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class AppsApiStub extends AppsApi {
  public listAppsResponse: ListAppsResponse;

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  public createApp(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(() => {});
  }

  public listAppsInWorkspace(): Promise<ListAppsResponse> {
    return new Promise((resolve) => resolve(this.listAppsResponse));
  }

  public deleteApp(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(() => {});
  }
}
