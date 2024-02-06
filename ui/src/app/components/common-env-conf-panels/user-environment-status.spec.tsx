import { AppStatus, RuntimeStatus } from 'generated/fetch';

import {
  fromRuntimeStatus,
  fromUserAppStatus,
  fromUserAppStatusWithFallback,
  UserEnvironmentStatus,
} from './user-environment-status';

describe('UserEnvironmentStatus', () => {
  test.each([
    [RuntimeStatus.RUNNING, UserEnvironmentStatus.RUNNING],
    [RuntimeStatus.STOPPING, UserEnvironmentStatus.PAUSING],
    [RuntimeStatus.STOPPED, UserEnvironmentStatus.PAUSED],
    [RuntimeStatus.STARTING, UserEnvironmentStatus.RESUMING],
    [RuntimeStatus.CREATING, UserEnvironmentStatus.CREATING],
    [RuntimeStatus.DELETED, UserEnvironmentStatus.DELETED],
    [RuntimeStatus.DELETING, UserEnvironmentStatus.DELETING],
    [RuntimeStatus.ERROR, UserEnvironmentStatus.ERROR],
    [RuntimeStatus.UPDATING, UserEnvironmentStatus.UPDATING],
    [RuntimeStatus.UNKNOWN, UserEnvironmentStatus.UNKNOWN],

    [undefined, UserEnvironmentStatus.UNKNOWN],
    [null, UserEnvironmentStatus.UNKNOWN],
  ])(
    'Should convert RuntimeStatus %s to the correct UserEnvironmentStatus',
    (runtimeStatus: RuntimeStatus, expected: UserEnvironmentStatus) => {
      expect(fromRuntimeStatus(runtimeStatus)).toBe(expected);
    }
  );

  test.each([
    [AppStatus.RUNNING, UserEnvironmentStatus.RUNNING],
    [AppStatus.STOPPING, UserEnvironmentStatus.PAUSING],
    [AppStatus.STOPPED, UserEnvironmentStatus.PAUSED],
    [AppStatus.STARTING, UserEnvironmentStatus.RESUMING],
    // no other AppStatuses are mapped currently
    [AppStatus.DELETED, UserEnvironmentStatus.UNKNOWN],
    [AppStatus.DELETING, UserEnvironmentStatus.UNKNOWN],
    [AppStatus.ERROR, UserEnvironmentStatus.UNKNOWN],
    [AppStatus.PROVISIONING, UserEnvironmentStatus.UNKNOWN],
    [AppStatus.STATUS_UNSPECIFIED, UserEnvironmentStatus.UNKNOWN],

    [undefined, UserEnvironmentStatus.UNKNOWN],
    [null, UserEnvironmentStatus.UNKNOWN],
  ])(
    'Should convert AppStatus %s to the correct UserEnvironmentStatus',
    (userAppStatus: AppStatus, expected: UserEnvironmentStatus) => {
      expect(fromUserAppStatus(userAppStatus)).toBe(expected);
    }
  );

  test.each([
    [AppStatus.RUNNING, UserEnvironmentStatus.RUNNING],
    [AppStatus.STOPPING, UserEnvironmentStatus.PAUSING],
    [AppStatus.STOPPED, UserEnvironmentStatus.PAUSED],
    [AppStatus.STARTING, UserEnvironmentStatus.RESUMING],
    // no other AppStatuses are mapped currently
    [AppStatus.DELETED, AppStatus.DELETED.toString()],
    [AppStatus.DELETING, AppStatus.DELETING.toString()],
    [AppStatus.ERROR, AppStatus.ERROR.toString()],
    [AppStatus.PROVISIONING, AppStatus.PROVISIONING.toString()],
    [AppStatus.STATUS_UNSPECIFIED, AppStatus.STATUS_UNSPECIFIED.toString()],

    [undefined, undefined],
    [null, undefined],
  ])(
    'Should convert AppStatus %s to the correct UserEnvironmentStatus with fallback',
    (userAppStatus: AppStatus, expected: string) => {
      expect(fromUserAppStatusWithFallback(userAppStatus)).toBe(expected);
    }
  );
});
