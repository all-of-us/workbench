import { UserMetricsApi } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

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
