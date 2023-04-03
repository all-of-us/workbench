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

export const maybeStartPollingForUserApps = (namespace) => {
  const { updating } = userAppsStore.get();
  // Prevents multiple update processes from running concurrently.
  if (updating) {
    return;
  }

  userAppsStore.set({ ...userAppsStore.get(), updating: true });
  appsApi()
    .listAppsInWorkspace(namespace)
    .then((listAppsResponse) => {
      userAppsStore.set({ userApps: listAppsResponse, updating: false });
      if (doUserAppsRequireUpdates()) {
        setTimeout(() => {
          maybeStartPollingForUserApps(namespace);
        }, 10 * 1000);
      }
    });
};

export const createUserApp = (namespace, config: CreateAppRequest) =>
  appsApi()
    .createApp(namespace, config)
    .then(() => {
      maybeStartPollingForUserApps(namespace);
    });

export const deleteUserApp = (namespace, appName, deleteDiskWithUserApp) => {
  return appsApi()
    .deleteApp(namespace, appName, deleteDiskWithUserApp)
    .then(() => maybeStartPollingForUserApps(namespace));
};

export const pauseUserApp = (googleProject, appName, namespace) => {
  leoAppsApi()
    .stopApp(googleProject, appName)
    .then(() => maybeStartPollingForUserApps(namespace));
};

export const resumeUserApp = (googleProject, appName, namespace) => {
  leoAppsApi()
    .startApp(googleProject, appName)
    .then(() => maybeStartPollingForUserApps(namespace));
};
