import {UserMetricsApi} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';
import {stubResource} from "./resources-stub";

export class UserMetricsApiStub extends UserMetricsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  getUserRecentResources() {
    return Promise.resolve([stubResource]);
  }
}
