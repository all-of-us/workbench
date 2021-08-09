import Timeout = NodeJS.Timeout;
import {TextModal} from 'app/components/text-modal';
import {debouncer} from 'app/utils';
import {signOut} from 'app/utils/authentication';
import {navigateSignOut} from 'app/utils/navigation';
import {environment} from 'environments/environment';

import * as React from 'react';

const {useState, useEffect} = React;

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
  MESSAGE_KEY: 'USER_ACTIVITY_DETECTED'
};

const getInactivityElapsedMs = () => {
  const lastActive = window.localStorage.getItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE);
  if (!lastActive) {
    return null;
  }
  return Date.now() - parseInt(lastActive, 10);
};

const secondsToText = (seconds: number) => {
  return seconds % 60 === 0 && seconds > 60 ?
      `${seconds / 60} minutes` : `${seconds} seconds`;
};

const invalidateInactivityCookieAndSignOut = (continuePath?: string): void => {
  window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, null);
  signOut(continuePath);
};

export const InactivityMonitor = () => {
  const [showModal, setShowModal] = useState(false);

  function signOutIfLocalStorageInactivityElapsed(continuePath?: string): void {
    const elapsedMs = getInactivityElapsedMs();
    if (elapsedMs && elapsedMs > environment.inactivityTimeoutSeconds * 1000) {
      invalidateInactivityCookieAndSignOut(continuePath);
    }
  }

  // Signal user activity.
  useEffect(() => {
    let getUserActivityTimer: () => Timeout;
    let inactivityInterval: Timeout;
    let logoutTimer: Timeout;
    let inactivityModalTimer: Timeout;

    const startUserActivityTracker = () => {
      const signalUserActivity = debouncer(() => {
        window.postMessage(INACTIVITY_CONFIG.MESSAGE_KEY, '*');
      }, 1000);
      getUserActivityTimer = signalUserActivity.getTimer;

      INACTIVITY_CONFIG.TRACKED_EVENTS.forEach(eventName => {
        window.addEventListener(eventName, () => signalUserActivity.invoke(), false);
      });
    };

    const startInactivityTimers = (elapsedMs: number = 0) => {
      clearTimeout(logoutTimer);
      logoutTimer = setTimeout(
        () => invalidateInactivityCookieAndSignOut('/session-expired'),
        Math.max(0, environment.inactivityTimeoutSeconds * 1000 - elapsedMs));

      clearTimeout(inactivityModalTimer);
      inactivityModalTimer = setTimeout(
        () => setShowModal(true),
        Math.max(0, 1000 * (environment.inactivityTimeoutSeconds - environment.inactivityWarningBeforeSeconds) - elapsedMs));
    };

    const startInactivityMonitoring = () => {
      startInactivityTimers();

      const resetTimers = () => {
        setShowModal(false);
        startInactivityTimers();
      };

      localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, Date.now().toString());
      resetTimers();

      // setTimeout does not necessary track real wall-time. Periodically
      // clear/restart the timers so that they reflect the time which has elapsed
      // since we last saw activity, as tracked in local storage.
      inactivityInterval = setInterval(() => {
        startInactivityTimers(getInactivityElapsedMs());
      }, 60 * 1000);

      window.addEventListener('message', (e) => {
        if (e.data !== INACTIVITY_CONFIG.MESSAGE_KEY) {
          return;
        }

        // setTimeout is not a reliable mechanism for forcing signout as it doesn't
        // model actual wall-time, for example a sleeping machine does not progress
        // a setTimeout timer. Always check whether the user's time has already
        // elapsed before updating our inactivity time tracker.
        signOutIfLocalStorageInactivityElapsed();

        window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, Date.now().toString());
        resetTimers();
      }, false);

      window.addEventListener('storage', (e) => {
        if (e.key === INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE && e.newValue !== null) {
          signOutIfLocalStorageInactivityElapsed();
          resetTimers();
        }
      });
    };

    signOutIfLocalStorageInactivityElapsed('/sign-in-again');
    startUserActivityTracker();
    startInactivityMonitoring();

    return () => {
      clearInterval(inactivityInterval);
      [
        getUserActivityTimer(),
        logoutTimer,
        inactivityModalTimer
      ].forEach((t) => clearTimeout(t));
    };
  }, []);

  const secondsBeforeDisplayingModal =
    environment.inactivityTimeoutSeconds - environment.inactivityWarningBeforeSeconds;

  return <React.Fragment>
    {showModal &&
     <TextModal
       closeFunction={() => setShowModal(false)}
       title='Your session is about to expire'
       body={`You have been idle for over ${secondsToText(secondsBeforeDisplayingModal)}. ` +
        'You can choose to extend your session by clicking the button below. You will be automatically logged ' +
        `out if there is no action in the next ${secondsToText(environment.inactivityWarningBeforeSeconds)}.`}
     />}
  </React.Fragment>;
};
