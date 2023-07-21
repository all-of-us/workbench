import {
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  ListAppsResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import { findApp, UIAppType } from 'app/components/apps-panel/utils';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi } from 'app/services/swagger-fetch-clients';

import { setSidebarActiveIconStore } from './navigation';
import { userAppsStore } from './stores';

export const appTypeToString: Record<AppType, string> = {
  [AppType.CROMWELL]: 'Cromwell',
  [AppType.RSTUDIO]: 'RStudio',
};

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
        const timeoutID = setTimeout(() => {
          maybeStartPollingForUserApps(namespace);
        }, 10 * 1000);

        userAppsStore.set({ ...userAppsStore.get(), timeoutID });
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

const localizeUserApp = (
  namespace,
  appName,
  appType: AppType,
  fileNames: Array<string>,
  playgroundMode: boolean
) => {
  appsApi().localizeApp(namespace, appName, {
    fileNames,
    playgroundMode,
    appType,
  });
};

export const resumeUserApp = (googleProject, appName, namespace) => {
  leoAppsApi()
    .startApp(googleProject, appName)
    .then(() => maybeStartPollingForUserApps(namespace));
};

export const findDisk = (disks: Disk[], appType: AppType): Disk | undefined =>
  disks.find((disk) => disk.appType === appType);

export function unattachedDiskExists(
  app: UserAppEnvironment | null | undefined,
  disk: Disk | undefined
) {
  return !app && disk !== undefined;
}

export const openRStudio = (
  workspaceNamespace: string,
  userApp: UserAppEnvironment
) => {
  localizeUserApp(
    workspaceNamespace,
    userApp.appName,
    userApp.appType,
    [],
    false
  );
  window.open(userApp.proxyUrls['rstudio-service'], '_blank').focus();
};

export const openRStudioOrConfigPanel = (
  workspaceNamespace: string,
  userApps: ListAppsResponse
) => {
  const userApp = findApp(userApps, UIAppType.RSTUDIO);
  if (userApp?.status === AppStatus.RUNNING) {
    openRStudio(workspaceNamespace, userApp);
  } else {
    setSidebarActiveIconStore.next(rstudioConfigIconId);
  }
};

// name of the tab for accessing notebooks and runtimes, also used to construct URLs
export const appFilesTabName = 'notebooks';
