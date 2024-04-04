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
  openConfigPanelForUIApp,
  toUIAppType,
  UIAppType,
} from 'app/components/apps-panel/utils';
import { appDisplayPath } from 'app/routing/utils';
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { userAppsStore } from 'app/utils/stores';

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

export const maybeStartPollingForUserApps = (namespace: string) => {
  const { updating } = userAppsStore.get();
  // Prevents multiple update processes from running concurrently.
  if (updating) {
    return;
  }
  const checkIfAppJustTurnedRunning = (listAppsResponse) => {
    if (!!userAppsStore.get() && userAppsStore.get().userApps === undefined) {
      if (
        !!listAppsResponse &&
        listAppsResponse.size > 0 &&
        listAppsResponse.some((app) => app.status === 'RUNNING')
      ) {
        return listAppsResponse.some((app) => app.status === 'RUNNING').appType;
      }
      return null;
    }

    const storeProvisioningApp = userAppsStore
      .get()
      .userApps.filter((userApp) => {
        return userApp.status === AppStatus.PROVISIONING;
      });

    // The assumption here is that Only 1 app is in provision state at a time
    if (storeProvisioningApp.length === 0) {
      return null;
    }
    const app = listAppsResponse.filter(
      (app) => app.appType === storeProvisioningApp[0].appType
    )[0];
    console.log(app.status);
    if (app.status === AppStatus.RUNNING) {
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
    }
    console.log(storeProvisioningApp);
  };

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

      checkIfAppJustTurnedRunning(listAppsResponse);

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
  // Confirm with yonghao
  // fetchWithErrorModal(() =>
  //   localizeUserApp(
  //     workspaceNamespace,
  //     userApp.appName,
  //     userApp.appType,
  //     [],
  //       false,
  //     false
  //   )
  // );
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
    openConfigPanelForUIApp(requestedApp);
  }
};
