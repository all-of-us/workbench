import { AppStatus, RuntimeStatus } from 'generated/fetch';

import {
  fromRuntimeStatus,
  fromUserAppStatus,
  fromUserAppStatusWithFallback,
  UserEnvironmentStatus,
} from './utils';

describe('AppsPanel utils', () => {
  test.each([
    [RuntimeStatus.RUNNING, UserEnvironmentStatus.RUNNING],
    [RuntimeStatus.STOPPING, UserEnvironmentStatus.PAUSING],
    [RuntimeStatus.STOPPED, UserEnvironmentStatus.PAUSED],
    [RuntimeStatus.STARTING, UserEnvironmentStatus.RESUMING],
    [RuntimeStatus.CREATING, UserEnvironmentStatus.CREATING],
    [RuntimeStatus.DELETED, UserEnvironmentStatus.DELETED],
    [RuntimeStatus.DELETING, UserEnvironmentStatus.DELETING],
    [RuntimeStatus.ERROR, UserEnvironmentStatus.ERROR],
    [RuntimeStatus.Updating, UserEnvironmentStatus.UPDATING],
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
    [AppStatus.STATUSUNSPECIFIED, UserEnvironmentStatus.UNKNOWN],

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
    [AppStatus.STATUSUNSPECIFIED, AppStatus.STATUSUNSPECIFIED.toString()],

    [undefined, undefined],
    [null, undefined],
  ])(
    'Should convert AppStatus %s to the correct UserEnvironmentStatus with fallback',
    (userAppStatus: AppStatus, expected: string) => {
      expect(fromUserAppStatusWithFallback(userAppStatus)).toBe(expected);
    }
  );
});
