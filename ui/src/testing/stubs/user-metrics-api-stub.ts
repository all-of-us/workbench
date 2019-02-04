import {UserMetricsApi} from 'generated/fetch';

export class UserMetricsApiStub extends UserMetricsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getUserRecentResources() {
    return Promise.resolve([]);
  }
}
