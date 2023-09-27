import * as fp from 'lodash/fp';

import {
  AppStatus,
  AppType,
  CreateAppRequest,
  DiskType,
  Runtime,
  RuntimeStatus,
  UserAppEnvironment,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { DEFAULT_MACHINE_NAME, findMachineByName } from 'app/utils/machines';
import * as runtimeUtils from 'app/utils/runtime-utils';
import { AnalysisConfig } from 'app/utils/runtime-utils';
import cromwellBanner from 'assets/user-apps/Cromwell-banner.png';
import cromwellIcon from 'assets/user-apps/Cromwell-icon.png';
import jupyterBanner from 'assets/user-apps/Jupyter-banner.png';
import jupyterIcon from 'assets/user-apps/Jupyter-icon.png';
import rStudioBanner from 'assets/user-apps/RStudio-banner.png';
import rStudioIcon from 'assets/user-apps/RStudio-icon.png';
import sasBanner from 'assets/user-apps/SAS-banner.png';
import sasIcon from 'assets/user-apps/SAS-icon.png';

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
    size: 500,
    diskType: DiskType.STANDARD,
  },
};

export const createAppRequestToAnalysisConfig = (
  createAppRequest: CreateAppRequest
): Partial<AnalysisConfig> => {
  return {
    machine: findMachineByName(
      createAppRequest.kubernetesRuntimeConfig.machineType
    ),
    diskConfig: {
      size: createAppRequest.persistentDiskRequest.size,
      detachable: true,
      detachableType: createAppRequest.persistentDiskRequest.diskType,
      existingDiskName: null,
    },
    numNodes: createAppRequest.kubernetesRuntimeConfig.numNodes,
  };
};

const isVisible = (status: AppStatus): boolean =>
  status && status !== AppStatus.DELETED;

export const isAppActive = (app: UserAppEnvironment): boolean =>
  isVisible(app?.status);

// TODO what about ERROR?
export const canCreateApp = (app: UserAppEnvironment): boolean =>
  !isVisible(app?.status);

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

// TODO reconcile with API AppType and LeonardoMapper

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

export const findApp = (
  apps: UserAppEnvironment[] | null | undefined,
  appType: UIAppType
): UserAppEnvironment | undefined =>
  apps?.find((app) => app.appType === toAppType[appType]);

// used as a generic equivalence for certain states of RuntimeStatus and AppStatus
export enum UserEnvironmentStatus {
  CREATING = 'Creating',
  DELETED = 'Deleted',
  DELETING = 'Deleting',
  ERROR = 'Error',
  RUNNING = 'Running',
  PAUSING = 'Pausing',
  PAUSED = 'Paused',
  RESUMING = 'Resuming',
  UPDATING = 'Updating',
  UNKNOWN = 'UNKNOWN',
}

export const fromRuntimeStatus = (
  status: RuntimeStatus
): UserEnvironmentStatus =>
  cond(
    [status === RuntimeStatus.CREATING, () => UserEnvironmentStatus.CREATING],
    [status === RuntimeStatus.DELETED, () => UserEnvironmentStatus.DELETED],
    [status === RuntimeStatus.DELETING, () => UserEnvironmentStatus.DELETING],
    [status === RuntimeStatus.ERROR, () => UserEnvironmentStatus.ERROR],
    [status === RuntimeStatus.RUNNING, () => UserEnvironmentStatus.RUNNING],
    [status === RuntimeStatus.STOPPING, () => UserEnvironmentStatus.PAUSING],
    [status === RuntimeStatus.STOPPED, () => UserEnvironmentStatus.PAUSED],
    [status === RuntimeStatus.STARTING, () => UserEnvironmentStatus.RESUMING],
    [status === RuntimeStatus.UPDATING, () => UserEnvironmentStatus.UPDATING],
    () => UserEnvironmentStatus.UNKNOWN
  );

export const fromUserAppStatus = (status: AppStatus): UserEnvironmentStatus =>
  cond(
    [status === AppStatus.RUNNING, () => UserEnvironmentStatus.RUNNING],
    [status === AppStatus.STOPPING, () => UserEnvironmentStatus.PAUSING],
    [status === AppStatus.STOPPED, () => UserEnvironmentStatus.PAUSED],
    [status === AppStatus.STARTING, () => UserEnvironmentStatus.RESUMING],
    () => UserEnvironmentStatus.UNKNOWN
  );

// if the status is mappable to a UserEnvironmentStatus, return that
// else return the original status
export const fromUserAppStatusWithFallback = (status: AppStatus): string => {
  const mappedStatus = fromUserAppStatus(status);
  return mappedStatus === UserEnvironmentStatus.UNKNOWN
    ? status?.toString()
    : mappedStatus;
};

export const toUserEnvironmentStatusByAppType = (
  status: AppStatus | RuntimeStatus,
  appType: UIAppType
): UserEnvironmentStatus =>
  appType === UIAppType.JUPYTER
    ? fromRuntimeStatus(status as RuntimeStatus)
    : fromUserAppStatus(status as AppStatus);

export interface AppDisplayState {
  appType: UIAppType;
  active: boolean;
}

const getAppDisplayState = (
  runtime: Runtime | null | undefined,
  userApps: UserAppEnvironment[],
  appType: UIAppType
): AppDisplayState => {
  return {
    appType,
    active:
      appType === UIAppType.JUPYTER
        ? runtimeUtils.isVisible(runtime?.status)
        : isAppActive(findApp(userApps, appType)),
  };
};

export const getAppsByDisplayGroup = (
  runtime: Runtime,
  userApps: UserAppEnvironment[],
  appsToDisplay: UIAppType[]
): AppDisplayState[][] => {
  const getAppDisplayStateWithContext = fp.partial(getAppDisplayState, [
    runtime,
    userApps,
  ]);
  return fp.flow(
    fp.map(getAppDisplayStateWithContext),
    // Partition function will result in an array of grouped elements based
    // on their app.active value.  True values come first.
    fp.partition((app: AppDisplayState) => app.active)
  )(appsToDisplay);
};
