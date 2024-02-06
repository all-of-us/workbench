import * as fp from 'lodash/fp';

import { Runtime, UserAppEnvironment } from 'generated/fetch';

import * as runtimeUtils from 'app/utils/runtime-utils';
import { findApp, isAppActive, UIAppType } from 'app/utils/user-apps-utils';

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
