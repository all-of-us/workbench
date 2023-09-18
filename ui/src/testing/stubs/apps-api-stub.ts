import {
  AppLocalizeRequest,
  AppLocalizeResponse,
  AppsApi,
  AppStatus,
  AppType,
  EmptyResponse,
  ListAppsResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

const listAppsAppResponseSharedDefaults = {
  googleProject: 'terra-vpc-sc-dev-42c21469',
  createdDate: '2023-03-13T18:16:49.481854Z',
  status: AppStatus.RUNNING,
  autopauseThreshold: null,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: 'n1-standard-4',
    autoscalingEnabled: false,
  },
  dateAccessed: '2023-03-13T18:16:49.481854Z',
  errors: [],
};

const createCromwellListAppsResponseDefaults: UserAppEnvironment = {
  ...listAppsAppResponseSharedDefaults,
  appName: 'all-of-us-2-cromwell-1234',
  appType: AppType.CROMWELL,
  creator: 'fake@fake-research-aou.org',
  proxyUrls: {
    'cromwell-service':
      'https://leonardo.dsde-dev.broadinstitute.org/proxy/google/v1/apps/terra-vpc-sc-dev-1234/all-of-us-2-cromwell-1234/cromwell-service',
  },
  diskName: 'all-of-us-pd-2-cromwell-1234',
  labels: {
    'created-by': 'fake@fake-research-aou.org',
    'aou-app-type': 'cromwell',
  },
};

const createRStudioListAppsResponseDefaults: UserAppEnvironment = {
  ...listAppsAppResponseSharedDefaults,
  appName: 'all-of-us-2-rstudio-1234',
  appType: AppType.RSTUDIO,
  proxyUrls: {
    [GKE_APP_PROXY_PATH_SUFFIX]:
      'https://leonardo.dsde-dev.broadinstitute.org/proxy/google/v1/apps/terra-vpc-sc-dev-1234/all-of-us-2-rstudio-1234/app',
  },
  diskName: 'all-of-us-pd-2-rstudio-1234',
  labels: {
    'created-by': 'fake@fake-research-aou.org',
    'aou-app-type': 'rstudio',
  },
};

export const createListAppsCromwellResponse = (
  overrides: Partial<UserAppEnvironment> = {}
): UserAppEnvironment => {
  return { ...createCromwellListAppsResponseDefaults, ...overrides };
};

export const createListAppsRStudioResponse = (
  overrides: Partial<UserAppEnvironment> = {}
): UserAppEnvironment => {
  return { ...createRStudioListAppsResponseDefaults, ...overrides };
};

export class AppsApiStub extends AppsApi {
  public listAppsResponse: ListAppsResponse;

  constructor() {
    super(undefined);
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
  public localizeApp(
    _workspaceNamespace: string,
    _appName: string,
    _body: AppLocalizeRequest,
    _options: any
  ): Promise<AppLocalizeResponse> {
    return new Promise<AppLocalizeResponse>(() => {});
  }
}
