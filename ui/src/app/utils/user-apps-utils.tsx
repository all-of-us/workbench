import {
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  ListAppsResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import {
  findApp,
  helpSidebarConfigIdForUIApp,
  toUIAppType,
  UIAppType,
} from 'app/components/apps-panel/utils';
import { appDisplayPath } from 'app/routing/utils';
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import { userAppsStore } from 'app/utils/stores';

import { GKE_APP_PROXY_PATH_SUFFIX } from './constants';
import { fetchWithErrorModal } from './errors';

export const appTypeToString: Record<AppType, string> = {
  [AppType.CROMWELL]: 'Cromwell',
  [AppType.RSTUDIO]: 'RStudio',
  [AppType.SAS]: 'SAS',
};

const appStatusesRequiringUpdates: Array<AppStatus> = [
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

export const deleteUserApp = (namespace, appName, deleteDiskWithUserApp) =>
  appsApi()
    .deleteApp(namespace, appName, deleteDiskWithUserApp)
    .then(() => maybeStartPollingForUserApps(namespace));

export const pauseUserApp = (googleProject, appName, namespace) =>
  leoAppsApi()
    .stopApp(googleProject, appName)
    .then(() => maybeStartPollingForUserApps(namespace));

const localizeUserApp = (
  namespace,
  appName,
  appType: AppType,
  fileNames: Array<string>,
  playgroundMode: boolean
) =>
  appsApi().localizeApp(namespace, appName, {
    fileNames,
    playgroundMode,
    appType,
  });

export const resumeUserApp = (googleProject, appName, namespace) =>
  leoAppsApi()
    .startApp(googleProject, appName)
    .then(() => maybeStartPollingForUserApps(namespace));

export const findDisk = (disks: Disk[], appType: AppType): Disk | undefined =>
  disks.find((disk) => disk.appType === appType);

export function unattachedDiskExists(
  app: UserAppEnvironment | null | undefined,
  disk: Disk | undefined
) {
  return !app && disk !== undefined;
}

export const localizeRStudioApp = (
  workspaceNamespace: string,
  userApp: UserAppEnvironment
) => {
  fetchWithErrorModal(() =>
    localizeUserApp(
      workspaceNamespace,
      userApp.appName,
      userApp.appType,
      [],
      false
    )
  );
};

export const openSAS = (
  workspaceNamespace: string,
  userApp: UserAppEnvironment
) => {
  fetchWithErrorModal(() =>
    localizeUserApp(
      workspaceNamespace,
      userApp.appName,
      userApp.appType,
      [],
      false
    )
  );
  window.open(userApp.proxyUrls[GKE_APP_PROXY_PATH_SUFFIX], '_blank').focus();
};

export const openAppInIframe = (
  workspaceNamespace: string,
  workspaceId: string,
  userApp: UserAppEnvironment,
  navigate: (commands: any, extras?: any) => void
) => {
  localizeRStudioApp(workspaceNamespace, userApp);
  navigate([
    appDisplayPath(
      workspaceNamespace,
      workspaceId,
      toUIAppType[userApp.appType]
    ),
  ]);
};

export const openAppOrConfigPanel = (
  workspaceNamespace: string,
  workspaceId: string,
  userApps: ListAppsResponse,
  requestedApp: UIAppType,
  navigate: (commands: any, extras?: any) => void
) => {
  const userApp = findApp(userApps, requestedApp);
  if (userApp?.status === AppStatus.RUNNING) {
    openAppInIframe(workspaceNamespace, workspaceId, userApp, navigate);
  } else {
    setSidebarActiveIconStore.next(helpSidebarConfigIdForUIApp(requestedApp));
  }
};

export const openRStudioOrConfigPanel = (
  workspaceNamespace: string,
  workspaceId: string,
  userApps: ListAppsResponse,
  navigate: (commands: any, extras?: any) => void
) => {
  openAppOrConfigPanel(
    workspaceNamespace,
    workspaceId,
    userApps,
    UIAppType.RSTUDIO,
    navigate
  );
};

export const openSASOrConfigPanel = (
  workspaceNamespace: string,
  workspaceId: string,
  userApps: ListAppsResponse,
  navigate: (commands: any, extras?: any) => void
) => {
  openAppOrConfigPanel(
    workspaceNamespace,
    workspaceId,
    userApps,
    UIAppType.SAS,
    navigate
  );
};
