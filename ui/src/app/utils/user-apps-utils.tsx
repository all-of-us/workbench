import {
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  ListAppsResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import {
  appMinDiskSize,
  findApp,
  openConfigPanelForUIApp,
  toUIAppType,
  UIAppType,
} from 'app/components/apps-panel/utils';
import { appDisplayPath } from 'app/routing/utils';
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { userAppsStore } from 'app/utils/stores';

import { MAX_GKE_APP_DISK_SIZE } from './constants';
import { fetchWithErrorModal } from './errors';
import { getLastActiveEpochMillis, setLastActive } from './inactivity';
import { currentWorkspaceStore } from './navigation';

// the polling timeout to use when waiting for a transition (e.g. from Running to Paused)
const transitionPollingTimeoutMs = 10e3; // 10 sec
// when we are not waiting for a transition, we still need to poll for activity
const activityPollingTimeoutMs = 5 * 60e3; // 5 min

export const appTypeToString: Record<AppType, string> = {
  [AppType.CROMWELL]: 'Cromwell',
  [AppType.RSTUDIO]: 'RStudio',
  [AppType.SAS]: 'SAS',
};

const transitionalAppStatuses: Array<AppStatus> = [
  AppStatus.DELETING,
  AppStatus.PROVISIONING,
  AppStatus.STARTING,
  AppStatus.STOPPING,
];

const doUserAppsRequireUpdates = (userApps: ListAppsResponse) =>
  !userApps ||
  userApps
    .map((userApp) => userApp.status)
    .some((appStatus) => transitionalAppStatuses.includes(appStatus));

// all integer dates in this function are milliseconds since epoch
export const updateLastActive = (userApps: ListAppsResponse) => {
  const userAppLastActive: number = Math.max(
    ...userApps.map((app) =>
      app.dateAccessed ? new Date(app.dateAccessed).valueOf() : 0
    )
  );
  if (userAppLastActive > (getLastActiveEpochMillis() ?? 0)) {
    setLastActive(userAppLastActive);
  }
};

const localizeUserApp = (
  namespace: string,
  appName: string,
  appType: AppType,
  fileNames: Array<string>,
  playgroundMode: boolean,
  localizeAllFile: boolean
) =>
  appsApi().localizeApp(namespace, appName, localizeAllFile, {
    fileNames,
    playgroundMode,
    appType,
  });

const appJustTurnedRunningFromProvisioning = (listAppsResponse) => {
  // Note: We do not call localize for CROMWELL
  // We want app that are transitioning from PROVISIONING to RUNNING
  const appsJustStartedRunning = userAppsStore
    .get()
    .userApps.filter((userApp) => {
      if (
        userApp.status === AppStatus.PROVISIONING &&
        userApp.appType !== AppType.CROMWELL
      ) {
        const runningAppFromApi = listAppsResponse.filter(
          (app) =>
            app.appType === userApp.appType && app.status === AppStatus.RUNNING
        );
        return !!runningAppFromApi && runningAppFromApi.length > 0;
      }
      return false;
    });
  return appsJustStartedRunning;
};

const callLocalizeIfApplicable = (listAppsResponse) => {
  // If userAppsStore is not updated lets wait for it to be updated before checking
  if (!!userAppsStore.get() && userAppsStore.get().userApps === undefined) {
    return null;
  }

  // Get the list of Apps that are in PROVISIONING state in store but RUNNING in list of Apps from api response
  // We want to call Localize only ONCE just as soon as they are running
  const appsTransitionToRunningNow =
    appJustTurnedRunningFromProvisioning(listAppsResponse);

  if (appsTransitionToRunningNow.length === 0) {
    return null;
  }

  appsTransitionToRunningNow.forEach((app) => {
    fetchWithErrorModal(
      async () =>
        await localizeUserApp(
          currentWorkspaceStore.getValue().namespace,
          app.appName,
          app.appType,
          [],
          false,
          true
        )
    );
  });
};

export const maybeStartPollingForUserApps = (namespace: string) => {
  const { updating } = userAppsStore.get();
  // Prevents multiple update processes from running concurrently.
  if (updating) {
    return;
  }

  userAppsStore.set({ ...userAppsStore.get(), updating: true });
  appsApi()
    .listAppsInWorkspace(namespace)
    .then((listAppsResponse) => {
      if (listAppsResponse) {
        updateLastActive(listAppsResponse);
      }

      const timeoutID = setTimeout(
        () => {
          maybeStartPollingForUserApps(namespace);
        },
        doUserAppsRequireUpdates(listAppsResponse)
          ? transitionPollingTimeoutMs
          : activityPollingTimeoutMs
      );

      callLocalizeIfApplicable(listAppsResponse);

      userAppsStore.set({
        userApps: listAppsResponse,
        updating: false,
        timeoutID,
      });
    });
};

export const createUserApp = (namespace: string, config: CreateAppRequest) =>
  appsApi()
    .createApp(namespace, config)
    .then(() => {
      maybeStartPollingForUserApps(namespace);
    });

export const deleteUserApp = (
  namespace: string,
  appName: string,
  deleteDiskWithUserApp: boolean
) =>
  appsApi()
    .deleteApp(namespace, appName, deleteDiskWithUserApp)
    .then(() => maybeStartPollingForUserApps(namespace));

export const pauseUserApp = (
  googleProject: string,
  appName: string,
  namespace: string
) =>
  leoAppsApi()
    .stopApp(googleProject, appName)
    .then(() => maybeStartPollingForUserApps(namespace));

export const resumeUserApp = (
  googleProject: string,
  appName: string,
  namespace: string
) =>
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

// does this app have a UI that the user can interact with?
export const isInteractiveUIApp = (appType: UIAppType) =>
  (
    [UIAppType.JUPYTER, UIAppType.RSTUDIO, UIAppType.SAS] as UIAppType[]
  ).includes(appType);

export const isInteractiveUserApp = (appType: AppType) =>
  isInteractiveUIApp(toUIAppType[appType]);

export const openAppInIframe = (
  workspaceNamespace: string,
  workspaceId: string,
  userApp: UserAppEnvironment,
  navigate: (commands: any, extras?: any) => void
) => {
  const url = decodeURI(window.location.href);
  const urlRoute = url.substring(url.lastIndexOf('/') + 1);
  const fileNameRegex = /[^\\]*\.(\w+)$/;
  const fileName = urlRoute.match(fileNameRegex);
  const localizeFileList =
    !!fileName && fileName.length > 0 ? [fileName[0]] : [];

  fetchWithErrorModal(() =>
    localizeUserApp(
      workspaceNamespace,
      userApp.appName,
      userApp.appType,
      localizeFileList,
      false,
      false
    )
  );
  navigate([
    appDisplayPath(
      workspaceNamespace,
      workspaceId,
      toUIAppType[userApp.appType]
    ),
  ]);
};

export const isDiskSizeValid = (appRequest) =>
  appRequest.persistentDiskRequest.size <= MAX_GKE_APP_DISK_SIZE &&
  appRequest.persistentDiskRequest.size >= appMinDiskSize[appRequest.appType];

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
    openConfigPanelForUIApp(requestedApp);
  }
};
