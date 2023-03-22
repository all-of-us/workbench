import { AppStatus, CreateAppRequest, ListAppsResponse } from 'generated/fetch';

import { appsApi } from 'app/services/swagger-fetch-clients';

import { userAppsStore } from './stores';

const appStatusesRequiringUpdates = [
  AppStatus.DELETING,
  AppStatus.PROVISIONING,
  AppStatus.STARTING,
  AppStatus.STOPPING,
];

const doUserAppsRequireUpdates = () => {
  const userApps: ListAppsResponse = userAppsStore.get();
  return userApps
    .map((userApp) => userApp.status)
    .some((appStatus) => appStatusesRequiringUpdates.includes(appStatus));
};

export const updateUserApps = (namespace) => {
  appsApi()
    .listAppsInWorkspace(namespace)
    .then((userApps) => {
      userAppsStore.set(userApps);

      if (doUserAppsRequireUpdates()) {
        setTimeout(() => {
          updateUserApps(namespace);
        }, 10 * 1000);
      }
    });
};

export const createUserApp = (nameSpace, config: CreateAppRequest) => {
  return appsApi()
    .createApp(nameSpace, config)
    .then(() => {
      updateUserApps(nameSpace);
    });
};
