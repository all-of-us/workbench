import { AppStatus, CreateAppRequest } from 'generated/fetch';

import { appsApi } from 'app/services/swagger-fetch-clients';

import { userAppsStore } from './stores';

const appStatusesRequiringUpdates = [
  AppStatus.DELETING,
  AppStatus.PROVISIONING,
  AppStatus.STARTING,
  AppStatus.STOPPING,
];

const doUserAppsRequireUpdates = () => {
  const { userApps } = userAppsStore.get();
  return userApps
    .map((userApp) => userApp.status)
    .some((appStatus) => appStatusesRequiringUpdates.includes(appStatus));
};

export const updateUserApps = (namespace) => {
  const { updating } = userAppsStore.get();

  // Prevents multiple update processes from running concurrently.
  if (!updating) {
    appsApi()
      .listAppsInWorkspace(namespace)
      .then((listAppsResponse) => {
        userAppsStore.set({ userApps: listAppsResponse });
        if (doUserAppsRequireUpdates()) {
          userAppsStore.set({ updating: true });
          setTimeout(() => {
            updateUserApps(namespace);
          }, 10 * 1000);
        }
      });
  }
};

export const createUserApp = (nameSpace, config: CreateAppRequest) => {
  return appsApi()
    .createApp(nameSpace, config)
    .then(() => {
      const { updating } = userAppsStore.get();
      if (!updating) {
        userAppsStore.set({ updating: true });
        updateUserApps(nameSpace);
      }
    });
};
