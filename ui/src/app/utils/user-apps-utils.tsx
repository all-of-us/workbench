import { AppStatus, CreateAppRequest } from 'generated/fetch';

import { appsApi } from 'app/services/swagger-fetch-clients';

import { userAppsStore } from './stores';
import * as userAppsUtils from './user-apps-utils';

const appStatusesRequiringUpdates = [
  AppStatus.DELETING,
  AppStatus.PROVISIONING,
  AppStatus.STARTING,
  AppStatus.STOPPING,
];

const doUserAppsRequireUpdates = () => {
  const { userApps } = userAppsStore.get();
  return (
    !userApps ||
    userApps
      .map((userApp) => userApp.status)
      .some((appStatus) => appStatusesRequiringUpdates.includes(appStatus))
  );
};

export const getUserApps = (namespace) => {
  const { updating } = userAppsStore.get();

  // Prevents multiple update processes from running concurrently.
  if (!updating) {
    userAppsStore.set({ updating: true });
    appsApi()
      .listAppsInWorkspace(namespace)
      .then((listAppsResponse) => {
        userAppsStore.set({ userApps: listAppsResponse, updating: false });
        if (doUserAppsRequireUpdates()) {
          setTimeout(() => {
            getUserApps(namespace);
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
        userAppsUtils.getUserApps(nameSpace);
      }
    });
};
