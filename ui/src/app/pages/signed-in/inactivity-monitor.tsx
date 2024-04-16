import * as React from 'react';

import { Profile } from 'generated/fetch';

import { environment } from 'environments/environment';
import { withErrorModal } from 'app/components/modals';
import { AccessTierShortNames, hasTierAccess } from 'app/utils/access-tiers';
import { AnalyticsTracker } from 'app/utils/analytics';
import { signOut } from 'app/utils/authentication';
import {
  clearLastActive,
  getLastActiveEpochMillis,
  INACTIVITY_CONFIG,
  setLastActiveNow,
} from 'app/utils/inactivity';
import { authStore, profileStore, useStore } from 'app/utils/stores';

const { useEffect } = React;

// Returns a function which will execute `action` at most once every `sensitivityMs` milliseconds
// if the returned function has been invoked within the last `sensitivityMs` milliseconds
export function debouncer(action, sensitivityMs) {
  let lastInvokeTimeMs = Date.now();

  const timer = global.setInterval(() => {
    const timeSinceLastInvokeMs = Date.now() - lastInvokeTimeMs;
    if (timeSinceLastInvokeMs < sensitivityMs) {
      action();
    }
  }, sensitivityMs);

  return {
    invoke: () => {
      lastInvokeTimeMs = Date.now();
    },
    getTimer: () => timer,
  };
}

const getInactivityTimeoutMs = (profile: Profile) => {
  if (hasTierAccess(profile, AccessTierShortNames.Controlled)) {
    return environment.inactivityTimeoutSecondsCt * 1000;
  } else {
    return environment.inactivityTimeoutSecondsRt * 1000;
  }
};

const getInactivityElapsedMs = () => {
  const lastActive = getLastActiveEpochMillis();
  return lastActive && Date.now() - lastActive;
};

const invalidateInactivityCookieAndSignOut = (continuePath?: string): void => {
  AnalyticsTracker.User.InactivitySignOut();
  clearLastActive();
  withErrorModal(
    {
      title: 'Sign Out Error',
      message: 'There was an error signing you out for inactivity.',
    },
    () => signOut(continuePath)
  )();
};

const getDefaultSignOutForInactivityTimeMs = (profile: Profile) =>
  Date.now() + getInactivityTimeoutMs(profile);

export const InactivityMonitor = () => {
  const { authLoaded, isSignedIn } = useStore(authStore);
  const { profile } = useStore(profileStore);

  function signOutIfLocalStorageInactivityElapsed(continuePath?: string): void {
    const elapsedMs = getInactivityElapsedMs();
    if (elapsedMs && elapsedMs > getInactivityTimeoutMs(profile)) {
      invalidateInactivityCookieAndSignOut(continuePath);
    }
  }

  // Signal user activity.
  useEffect(() => {
    if (!authLoaded) {
      return;
    }

    if (!isSignedIn) {
      clearLastActive();
      return;
    }

    let getUserActivityTimer: () => NodeJS.Timeout;
    let inactivityInterval: NodeJS.Timeout;
    let logoutTimer: NodeJS.Timeout;

    const startUserActivityTracker = () => {
      const signalUserActivity = debouncer(() => {
        window.postMessage(INACTIVITY_CONFIG.MESSAGE_KEY, '*');
      }, 1000);
      getUserActivityTimer = signalUserActivity.getTimer;

      INACTIVITY_CONFIG.TRACKED_EVENTS.forEach((eventName) => {
        window.addEventListener(
          eventName,
          () => signalUserActivity.invoke(),
          false
        );
      });
    };

    const startInactivityTimers = (elapsedMs: number = 0) => {
      const newSignOutForInactivityTimeMs =
        getDefaultSignOutForInactivityTimeMs(profile) - elapsedMs;

      clearTimeout(logoutTimer);
      logoutTimer = global.setTimeout(
        () => invalidateInactivityCookieAndSignOut('/session-expired'),
        Math.max(0, newSignOutForInactivityTimeMs - Date.now())
      );
    };

    const startInactivityMonitoring = () => {
      startInactivityTimers();

      const resetTimers = () => {
        startInactivityTimers();
      };

      setLastActiveNow();
      resetTimers();

      // setTimeout does not necessary track real wall-time. Periodically
      // clear/restart the timers so that they reflect the time which has elapsed
      // since we last saw activity, as tracked in local storage.
      inactivityInterval = global.setInterval(() => {
        startInactivityTimers(getInactivityElapsedMs());
      }, 60 * 1000);

      window.addEventListener(
        'message',
        (e) => {
          if (e.data !== INACTIVITY_CONFIG.MESSAGE_KEY) {
            return;
          }

          // setTimeout is not a reliable mechanism for forcing signout as it doesn't
          // model actual wall-time, for example a sleeping machine does not progress
          // a setTimeout timer. Always check whether the user's time has already
          // elapsed before updating our inactivity time tracker.
          signOutIfLocalStorageInactivityElapsed();

          setLastActiveNow();
          resetTimers();
        },
        false
      );

      window.addEventListener('storage', (e) => {
        if (
          e.key === INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE &&
          e.newValue !== null
        ) {
          signOutIfLocalStorageInactivityElapsed();
          resetTimers();
        }
      });
    };

    signOutIfLocalStorageInactivityElapsed('/login');
    startUserActivityTracker();
    startInactivityMonitoring();

    return () => {
      clearInterval(inactivityInterval);
      [getUserActivityTimer(), logoutTimer].forEach((t) => clearTimeout(t));
    };
  }, [authLoaded, isSignedIn]);

  if (!authLoaded || !isSignedIn) {
    return null;
  }

  return null;
};
