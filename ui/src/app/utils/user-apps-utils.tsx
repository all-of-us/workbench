import { AppStatus, CreateAppRequest } from 'generated/fetch';

import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
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
    userAppsStore.set({ ...userAppsStore.get(), updating: true });
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

export const createUserApp = (namespace, config: CreateAppRequest) => {
  return appsApi()
    .createApp(namespace, config)
    .then(() => {
      const { updating } = userAppsStore.get();
      if (!updating) {
        getUserApps(namespace);
      }
    });
};

export const deleteUserApp = (namespace, appName, deleteDiskWithUserApp) => {
  return appsApi()
    .deleteApp(namespace, appName, deleteDiskWithUserApp)
    .then(() => getUserApps(namespace));
};

export const pauseUserApp = (googleProject, appName, namespace) => {
  leoAppsApi()
    .stopApp(googleProject, appName)
    .then(() => getUserApps(namespace));
};

export const resumeUserApp = (googleProject, appName, namespace) => {
  leoAppsApi()
    .startApp(googleProject, appName)
    .then(() => getUserApps(namespace));
};
