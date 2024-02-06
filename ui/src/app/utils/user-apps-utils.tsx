import {
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  DiskType,
  ListAppsResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import cromwellBanner from 'app/assets/user-apps/Cromwell-banner.png';
import cromwellIcon from 'app/assets/user-apps/Cromwell-icon.png';
import jupyterBanner from 'app/assets/user-apps/Jupyter-banner.png';
import jupyterIcon from 'app/assets/user-apps/Jupyter-icon.png';
import rStudioBanner from 'app/assets/user-apps/RStudio-banner.png';
import rStudioIcon from 'app/assets/user-apps/RStudio-icon.png';
import sasBanner from 'app/assets/user-apps/SAS-banner.png';
import sasIcon from 'app/assets/user-apps/SAS-icon.png';
import {
  cromwellConfigIconId,
  rstudioConfigIconId,
  sasConfigIconId,
  SidebarIconId,
} from 'app/components/help-sidebar-icons';
import { appDisplayPath } from 'app/routing/utils';
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi } from 'app/services/swagger-fetch-clients';
import { userAppsStore } from 'app/utils/stores';

import { fetchWithErrorModal } from './errors';
import { getLastActiveEpochMillis, setLastActive } from './inactivity';
import { DEFAULT_MACHINE_NAME } from './machines';
import { sidebarActiveIconStore } from './navigation';

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
  if (userAppLastActive > getLastActiveEpochMillis() ?? 0) {
    setLastActive(userAppLastActive);
  }
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

      userAppsStore.set({
        userApps: listAppsResponse,
        updating: false,
        timeoutID,
      });
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

const localizeUserApp = (
  namespace: string,
  appName: string,
  appType: AppType,
  fileNames: Array<string>,
  playgroundMode: boolean
) =>
  appsApi().localizeApp(namespace, appName, {
    fileNames,
    playgroundMode,
    appType,
  });

// Eventually we will need to align this with the API's AppType
export enum UIAppType {
  JUPYTER = 'Jupyter',
  RSTUDIO = 'RStudio',
  CROMWELL = 'Cromwell',
  SAS = 'SAS',
}

interface AppAssets {
  appType: UIAppType;
  banner: string;
  icon: string;
}

export const appAssets: AppAssets[] = [
  {
    appType: UIAppType.JUPYTER,
    banner: jupyterBanner,
    icon: jupyterIcon,
  },
  {
    appType: UIAppType.RSTUDIO,
    banner: rStudioBanner,
    icon: rStudioIcon,
  },
  {
    appType: UIAppType.CROMWELL,
    banner: cromwellBanner,
    icon: cromwellIcon,
  },
  {
    appType: UIAppType.SAS,
    banner: sasBanner,
    icon: sasIcon,
  },
];
// TODO replace with better defaults?
export const defaultCromwellConfig: CreateAppRequest = {
  appType: AppType.CROMWELL,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: DEFAULT_MACHINE_NAME,
    autoscalingEnabled: false,
  },
  persistentDiskRequest: {
    size: 50,
    diskType: DiskType.STANDARD,
  },
};
// TODO replace with better defaults?
export const defaultRStudioConfig: CreateAppRequest = {
  appType: AppType.RSTUDIO,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: DEFAULT_MACHINE_NAME,
    autoscalingEnabled: false,
  },
  persistentDiskRequest: {
    size: 100,
    diskType: DiskType.STANDARD,
  },
};
// TODO replace with better defaults?
export const defaultSASConfig: CreateAppRequest = {
  appType: AppType.SAS,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: DEFAULT_MACHINE_NAME,
    autoscalingEnabled: false,
  },
  persistentDiskRequest: {
    size: 250,
    diskType: DiskType.STANDARD,
  },
};
const isVisible = (status: AppStatus): boolean =>
  status && status !== AppStatus.DELETED;
export const isAppActive = (app: UserAppEnvironment): boolean =>
  isVisible(app?.status);
// matches Leonardo code
// https://github.com/DataBiosphere/leonardo/blob/eeae99dacf542c45ec528ce97c9fa72c31aae889/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/kubernetesModels.scala#L457
export const isDeletable = (status: AppStatus): boolean =>
  (
    [
      AppStatus.STATUS_UNSPECIFIED,
      AppStatus.RUNNING,
      AppStatus.ERROR,
    ] as Array<AppStatus>
  ).includes(status);
export const canDeleteApp = (app: UserAppEnvironment): boolean =>
  app && isDeletable(app.status);
export const toAppType: Record<UIAppType, AppType | null> = {
  [UIAppType.CROMWELL]: AppType.CROMWELL,
  [UIAppType.RSTUDIO]: AppType.RSTUDIO,
  [UIAppType.SAS]: AppType.SAS,
  [UIAppType.JUPYTER]: null,
};
export const toUIAppType: Record<AppType, UIAppType> = {
  [AppType.CROMWELL]: UIAppType.CROMWELL,
  [AppType.RSTUDIO]: UIAppType.RSTUDIO,
  [AppType.SAS]: UIAppType.SAS,
};
export const helpSidebarConfigIdForUIApp: Record<
  Exclude<UIAppType, UIAppType.JUPYTER>,
  SidebarIconId
> = {
  [UIAppType.SAS]: sasConfigIconId,
  [UIAppType.RSTUDIO]: rstudioConfigIconId,
  [UIAppType.CROMWELL]: cromwellConfigIconId,
};
export const openConfigPanelForUIApp = (appType: UIAppType) =>
  sidebarActiveIconStore.next(helpSidebarConfigIdForUIApp[appType]);
export const findApp = (
  apps: UserAppEnvironment[] | null | undefined,
  appType: UIAppType
): UserAppEnvironment | undefined =>
  apps?.find((app) => app.appType === toAppType[appType]);
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
  fetchWithErrorModal(() =>
    localizeUserApp(
      workspaceNamespace,
      userApp.appName,
      userApp.appType,
      [],
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
