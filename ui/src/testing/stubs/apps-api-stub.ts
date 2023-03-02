import {
  AppsApi,
  AppStatus,
  AppType,
  EmptyResponse,
  ListAppsResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

const createRStudioListAppsResponseDefaults: UserAppEnvironment = {
  googleProject: 'terra-vpc-sc-dev-1234',
  appName: 'all-of-us-2-rstudio-1234',
  appType: AppType.RSTUDIO,
  createdDate: '2023-02-28T21:10:28.723328Z',
  status: AppStatus.RUNNING,
  autopauseThreshold: null,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: 'n1-standard-4',
    autoscalingEnabled: false,
  },
  proxyUrls: {
    rstudio:
      'https://leonardo.dsde-dev.broadinstitute.org/proxy/google/v1/apps/terra-vpc-sc-dev-1234/all-of-us-2-rstudio-1234/rstudio',
  },
  diskName: 'all-of-us-pd-2-rstudio-1234',
  dateAccessed: '2023-02-28T21:10:28.723328Z',
  errors: [],
  labels: {
    'created-by': 'fake@fake-research-aou.org',
    'aou-app-type': 'rstudio',
  },
};

export const createListAppsRStudioResponse = (
  overrides: Partial<UserAppEnvironment> = {}
): UserAppEnvironment => {
  return { ...createRStudioListAppsResponseDefaults, ...overrides };
};

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
