import { UserMetricsApi } from 'generated/fetch';

import { cohortStub } from './cohort-builder-service-stub';
import { stubResource } from './resources-stub';

export const userMetricsApiStubResources = [
  { ...stubResource, cohort: cohortStub },
];
export class UserMetricsApiStub extends UserMetricsApi {
  constructor() {
    super(undefined);
  }

  getUserRecentResources() {
    return Promise.resolve(userMetricsApiStubResources);
  }
}
