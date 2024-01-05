import * as React from 'react';

import { environment } from 'environments/environment';
import { withErrorModal } from 'app/components/modals';
import { InactivityModal } from 'app/pages/signed-in/inactivity-modal';
import { AnalyticsTracker } from 'app/utils/analytics';
import { signOut } from 'app/utils/authentication';
import {
  clearLastActive,
  getLastActiveEpochMillis,
  INACTIVITY_CONFIG,
  setLastActiveNow,
} from 'app/utils/inactivity';
import { authStore, useStore } from 'app/utils/stores';

const { useState, useEffect } = React;

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

const getInactivityTimeoutMs = () => {
  return environment.inactivityTimeoutSeconds * 1000;
};

const getInactivityElapsedMs = () => {
  const lastActive = getLastActiveEpochMillis();
  return lastActive && Date.now() - lastActive;
};

const invalidateInactivityCookie = (): void => clearLastActive();

const invalidateInactivityCookieAndSignOut = (continuePath?: string): void => {
  AnalyticsTracker.User.InactivitySignOut();
  invalidateInactivityCookie();
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
  const { authLoaded, isSignedIn } = useStore(authStore);
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
    if (!authLoaded) {
      return;
    }

    if (!isSignedIn) {
      invalidateInactivityCookie();
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
        getDefaultSignOutForInactivityTimeMs() - elapsedMs;

      clearTimeout(logoutTimer);
      logoutTimer = global.setTimeout(
        () => invalidateInactivityCookieAndSignOut('/session-expired'),
        Math.max(0, newSignOutForInactivityTimeMs - Date.now())
      );

      setSignOutForInactivityTimeMs(newSignOutForInactivityTimeMs);
    };

    const startInactivityMonitoring = () => {
      startInactivityTimers();

      const resetTimers = () => {
        resetSignOutForInactivityTime();
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

  const onCloseInactivityModal = () => {
    resetSignOutForInactivityTime();
    // todo: clicking to close the modal will fire a click event, which will reset the timer,
    // however, we should still reset the timers here so we don't rely on that implicit behavior
  };

  if (!authLoaded || !isSignedIn) {
    return null;
  }

  return (
    <InactivityModal
      closeFunction={onCloseInactivityModal}
      currentTimeMs={currentTimeMs}
      signOutForInactivityTimeMs={signOutForInactivityTimeMs}
    />
  );
};
