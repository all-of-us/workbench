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

// visible/actionable logic is copied from runtime-utils

export const isVisible = (status: AppStatus): boolean =>
  status && ![AppStatus.DELETED, AppStatus.ERROR].includes(status);

export const shouldShowApp = (app: UserAppEnvironment): boolean =>
  isVisible(app?.status);

export const canCreateApp = (app: UserAppEnvironment): boolean =>
  !isVisible(app?.status);

export const isActionable = (status: AppStatus): boolean =>
  [AppStatus.RUNNING, AppStatus.STOPPED].includes(status);

export const canDeleteApp = (app: UserAppEnvironment): boolean =>
  app && isActionable(app.status);

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
export type UserEnvironmentStatus =
  | 'Creating'
  | 'Deleted'
  | 'Deleting'
  | 'Error'
  | 'UNKNOWN'
  | 'Running'
  | 'Pausing'
  | 'Paused'
  | 'Resuming'
  | 'Updating';

export const fromRuntimeStatus = (
  status: RuntimeStatus
): UserEnvironmentStatus =>
  cond(
    [status === RuntimeStatus.Creating, () => 'Creating'],
    [status === RuntimeStatus.Deleted, () => 'Deleted'],
    [status === RuntimeStatus.Deleting, () => 'Deleting'],
    [status === RuntimeStatus.Error, () => 'Error'],
    [status === RuntimeStatus.Running, () => 'Running'],
    [status === RuntimeStatus.Stopping, () => 'Pausing'],
    [status === RuntimeStatus.Stopped, () => 'Paused'],
    [status === RuntimeStatus.Starting, () => 'Resuming'],
    [status === RuntimeStatus.Updating, () => 'Updating'],
    () => 'UNKNOWN'
  );

export const fromUserAppStatus = (status: AppStatus): UserEnvironmentStatus =>
  cond(
    [status === AppStatus.RUNNING, () => 'Running'],
    [status === AppStatus.STOPPING, () => 'Pausing'],
    [status === AppStatus.STOPPED, () => 'Paused'],
    [status === AppStatus.STARTING, () => 'Resuming'],
    () => 'UNKNOWN'
  );

// if the status is mappable to a UserEnvironmentStatus, return that
// else return the original status
export const fromUserAppStatusWithFallback = (status: AppStatus): string => {
  const mappedStatus = fromUserAppStatus(status);
  return mappedStatus === 'UNKNOWN' && status
    ? status?.toString()
    : mappedStatus;
};
