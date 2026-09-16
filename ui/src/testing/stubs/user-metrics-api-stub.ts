import { UserMetricsApi } from 'generated/fetch';

import { stubResource } from './resources-stub';

export const userMetricsApiStubResources = [stubResource];
export class UserMetricsApiStub extends UserMetricsApi {
  constructor() {
    super(undefined);
  }

  getUserRecentResources() {
    return Promise.resolve(userMetricsApiStubResources);
  }
}
