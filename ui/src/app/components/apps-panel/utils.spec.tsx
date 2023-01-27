import { AppStatus, RuntimeStatus } from 'generated/fetch';

import {
  fromRuntimeStatus,
  fromUserAppStatus,
  fromUserAppStatusWithFallback,
  UserEnvironmentStatus,
} from './utils';

describe('AppsPanel utils', () => {
  test.each([
    [RuntimeStatus.Running, 'Running'],
    [RuntimeStatus.Stopping, 'Pausing'],
    [RuntimeStatus.Stopped, 'Paused'],
    [RuntimeStatus.Starting, 'Resuming'],
    [RuntimeStatus.Creating, 'Creating'],
    [RuntimeStatus.Deleted, 'Deleted'],
    [RuntimeStatus.Deleting, 'Deleting'],
    [RuntimeStatus.Error, 'Error'],
    [RuntimeStatus.Updating, 'Updating'],
    [RuntimeStatus.Unknown, 'UNKNOWN'],

    [undefined, 'UNKNOWN'],
    [null, 'UNKNOWN'],
  ])(
    'Should convert RuntimeStatus %s to the correct UserEnvironmentStatus',
    (runtimeStatus: RuntimeStatus, expected: UserEnvironmentStatus) => {
      expect(fromRuntimeStatus(runtimeStatus)).toBe(expected);
    }
  );

  test.each([
    [AppStatus.RUNNING, 'Running'],
    [AppStatus.STOPPING, 'Pausing'],
    [AppStatus.STOPPED, 'Paused'],
    [AppStatus.STARTING, 'Resuming'],

    // no other AppStatuses are mapped currently

    [AppStatus.DELETED, 'UNKNOWN'],
    [AppStatus.DELETING, 'UNKNOWN'],
    [AppStatus.ERROR, 'UNKNOWN'],
    [AppStatus.PROVISIONING, 'UNKNOWN'],
    [AppStatus.STATUSUNSPECIFIED, 'UNKNOWN'],

    [undefined, 'UNKNOWN'],
    [null, 'UNKNOWN'],
  ])(
    'Should convert AppStatus %s to the correct UserEnvironmentStatus',
    (userAppStatus: AppStatus, expected: UserEnvironmentStatus) => {
      expect(fromUserAppStatus(userAppStatus)).toBe(expected);
    }
  );

  test.each([
    [AppStatus.RUNNING, 'Running'],
    [AppStatus.STOPPING, 'Pausing'],
    [AppStatus.STOPPED, 'Paused'],
    [AppStatus.STARTING, 'Resuming'],

    // no other AppStatuses are mapped currently

    [AppStatus.DELETED, AppStatus.DELETED],
    [AppStatus.DELETING, AppStatus.DELETING],
    [AppStatus.ERROR, AppStatus.ERROR],
    [AppStatus.PROVISIONING, AppStatus.PROVISIONING],
    [AppStatus.STATUSUNSPECIFIED, AppStatus.STATUSUNSPECIFIED],

    [undefined, undefined],
    [null, undefined],
  ])(
    'Should convert AppStatus %s to the correct UserEnvironmentStatus with fallback',
    (userAppStatus: AppStatus, expected: string) => {
      expect(fromUserAppStatusWithFallback(userAppStatus)).toBe(expected);
    }
  );
});
