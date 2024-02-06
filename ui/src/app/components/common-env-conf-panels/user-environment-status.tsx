import { AppStatus, RuntimeStatus } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { UIAppType } from 'app/utils/user-apps-utils';

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
