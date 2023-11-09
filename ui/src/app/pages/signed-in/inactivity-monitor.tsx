import * as React from 'react';

import { environment } from 'environments/environment';
import { withErrorModal } from 'app/components/modals';
import { InactivityModal } from 'app/pages/signed-in/inactivity-modal';
import { AnalyticsTracker } from 'app/utils/analytics';
import { signOut } from 'app/utils/authentication';

const { useState, useEffect } = React;

// Returns a function which will execute `action` at most once every `sensitivityMs` milliseconds
// if the returned function has been invoked within the last `sensitivityMs` milliseconds
export function debouncer(action, sensitivityMs) {
  let t = Date.now();

  const timer = global.setInterval(() => {
    if (Date.now() - t < sensitivityMs) {
      action();
    }
  }, sensitivityMs);

  return {
    invoke: () => {
      t = Date.now();
    },
    getTimer: () => timer,
  };
}

/*
 * The user's last known active timestamp is stored in localStorage with the key of
 * INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE. This value is checked whenever
 * the application is reloaded. If the difference between the time at reload and the
 * value in local storage is greater than the inactivity timeout period, the user will
 * be signed out of all Google accounts.
 *
 * If the localStorage value is null for whatever reason, we defer to the more secure
 * solution of logging out the user. This should not affect new users since the logout
 * flow is ignored if there is no user session.
 */
export const INACTIVITY_CONFIG = {
  TRACKED_EVENTS: ['mousedown', 'keypress', 'scroll', 'click'],
  LOCAL_STORAGE_KEY_LAST_ACTIVE: 'LAST_ACTIVE_TIMESTAMP_EPOCH_MS',
  MESSAGE_KEY: 'USER_ACTIVITY_DETECTED',
};

const getInactivityTimeoutMs = () => {
  return environment.inactivityTimeoutSeconds * 1000;
};

const getInactivityElapsedMs = () => {
  const lastActive = window.localStorage.getItem(
    INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
  );
  if (!lastActive) {
    return null;
  }
  return Date.now() - parseInt(lastActive, 10);
};

const invalidateInactivityCookieAndSignOut = (continuePath?: string): void => {
  AnalyticsTracker.User.InactivitySignOut();
  window.localStorage.setItem(
    INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE,
    null
  );
  withErrorModal(
    {
      title: 'Sign Out Error',
      message: 'There was an error signing you out for inactivity.',
    },
    () => signOut(continuePath)
  )();
};

const getDefaultSignOutForInactivityTimeMs = () =>
  Date.now() + getInactivityTimeoutMs();

export const InactivityMonitor = () => {
  // todo: use this value in non-modal logic
  const [signOutForInactivityTimeMs, setSignOutForInactivityTimeMs] =
    useState<number>(getDefaultSignOutForInactivityTimeMs());

  // Using Date.now() will not trigger a re-render, so we use React state
  const [currentTimeMs, setCurrentTimeMs] = useState<number>(Date.now());
  useEffect(() => {
    setInterval(() => {
      setCurrentTimeMs(Date.now());
    }, 1000);
  }, []);

  const resetSignOutForInactivityTime = () => {
    setSignOutForInactivityTimeMs(getDefaultSignOutForInactivityTimeMs());
  };

  function signOutIfLocalStorageInactivityElapsed(continuePath?: string): void {
    const elapsedMs = getInactivityElapsedMs();
    if (elapsedMs && elapsedMs > getInactivityTimeoutMs()) {
      invalidateInactivityCookieAndSignOut(continuePath);
    }
  }

  // Signal user activity.
  useEffect(() => {
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
      clearTimeout(logoutTimer);
      logoutTimer = global.setTimeout(
        () => invalidateInactivityCookieAndSignOut('/session-expired'),
        Math.max(0, getInactivityTimeoutMs() - elapsedMs)
      );

      setSignOutForInactivityTimeMs(
        Date.now() + getInactivityTimeoutMs() - elapsedMs
      );
    };

    const startInactivityMonitoring = () => {
      startInactivityTimers();

      const resetTimers = () => {
        resetSignOutForInactivityTime();
        startInactivityTimers();
      };

      localStorage.setItem(
        INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE,
        Date.now().toString()
      );
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

          window.localStorage.setItem(
            INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE,
            Date.now().toString()
          );
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
  }, []);

  const onCloseInactivityModal = () => {
    resetSignOutForInactivityTime();
    // todo: clicking to close the modal will fire a click event, which will reset the timer,
    // however, we should still reset the timers here so we don't rely on that implicit behavior
  };

  return (
    <InactivityModal
      closeFunction={onCloseInactivityModal}
      currentTimeMs={currentTimeMs}
      inactivityWarningBeforeMs={
        environment.inactivityWarningBeforeSeconds * 1000
      }
      signOutForInactivityTimeMs={signOutForInactivityTimeMs}
    />
  );
};
