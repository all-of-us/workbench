import {
  AppStatus,
  AppType,
  CreateAppRequest,
  DiskType,
  RuntimeStatus,
  UserAppEnvironment,
} from 'generated/fetch';

import { cond, switchCase } from 'app/utils';
import { DEFAULT_MACHINE_NAME } from 'app/utils/machines';
import cromwellLogo from 'assets/images/Cromwell.png';
import cromwellIcon from 'assets/images/Cromwell-icon.png';
import jupyterLogo from 'assets/images/Jupyter.png';
import jupyterIcon from 'assets/images/Jupyter-icon.png';
import rStudioLogo from 'assets/images/RStudio.png';
import rStudioIcon from 'assets/images/RStudio-icon.png';

// Eventually we will need to align this with the API's AppType
export enum UIAppType {
  JUPYTER = 'Jupyter',
  RSTUDIO = 'RStudio',
  CROMWELL = 'Cromwell',
}

interface AppAssets {
  appType: UIAppType;
  logo: string;
  icon: string;
}
export const appAssets: AppAssets[] = [
  {
    appType: UIAppType.JUPYTER,
    logo: jupyterLogo,
    icon: jupyterIcon,
  },
  {
    appType: UIAppType.RSTUDIO,
    logo: rStudioLogo,
    icon: rStudioIcon,
  },
  {
    appType: UIAppType.CROMWELL,
    logo: cromwellLogo,
    icon: cromwellIcon,
  },
];

// TODO replace with better defaults
export const defaultCromwellConfig: CreateAppRequest = {
  appType: AppType.CROMWELL,
  kubernetesRuntimeConfig: {
    numNodes: 1,
    machineType: DEFAULT_MACHINE_NAME,
    autoscalingEnabled: false,
  },
  persistentDiskRequest: {
    size: 50,
    diskType: DiskType.Standard,
  },
};

export const isVisible = (status: AppStatus): boolean =>
  status && status !== AppStatus.DELETED;

export const shouldShowApp = (app: UserAppEnvironment): boolean =>
  isVisible(app?.status);

// TODO what about ERROR?
export const canCreateApp = (app: UserAppEnvironment): boolean =>
  !isVisible(app?.status);

export const isDeletable = (status: AppStatus): boolean =>
  [AppStatus.RUNNING, AppStatus.ERROR].includes(status);

export const canDeleteApp = (app: UserAppEnvironment): boolean =>
  app && isDeletable(app.status);

// TODO reconcile with API AppType and LeonardoMapper

export const toAppType = (type: UIAppType): AppType =>
  switchCase(
    type,
    [UIAppType.CROMWELL, () => AppType.CROMWELL],
    [UIAppType.RSTUDIO, () => AppType.RSTUDIO]
  );

export const findApp = (
  apps: UserAppEnvironment[],
  appType: UIAppType
): UserAppEnvironment =>
  apps?.find((app) => app.appType === toAppType(appType));

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
    [status === RuntimeStatus.Creating, () => UserEnvironmentStatus.CREATING],
    [status === RuntimeStatus.Deleted, () => UserEnvironmentStatus.DELETED],
    [status === RuntimeStatus.Deleting, () => UserEnvironmentStatus.DELETING],
    [status === RuntimeStatus.Error, () => UserEnvironmentStatus.ERROR],
    [status === RuntimeStatus.Running, () => UserEnvironmentStatus.RUNNING],
    [status === RuntimeStatus.Stopping, () => UserEnvironmentStatus.PAUSING],
    [status === RuntimeStatus.Stopped, () => UserEnvironmentStatus.PAUSED],
    [status === RuntimeStatus.Starting, () => UserEnvironmentStatus.RESUMING],
    [status === RuntimeStatus.Updating, () => UserEnvironmentStatus.UPDATING],
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
  cond<UserEnvironmentStatus>(
    [
      appType === UIAppType.CROMWELL,
      () => fromUserAppStatus(status as AppStatus),
    ],
    [
      appType === UIAppType.JUPYTER,
      () => fromRuntimeStatus(status as RuntimeStatus),
    ],
    () => UserEnvironmentStatus.UNKNOWN
  );
