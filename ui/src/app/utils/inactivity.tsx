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

export const getLastActiveEpochMillis = (): number | undefined => {
  const lastActive: string = window.localStorage.getItem(
    INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
  );
  return lastActive && parseInt(lastActive, 10);
};

/**
 * start tracking last active timestamp
 */
export const startLastActive = (): void => {
  console.log('START');
  setLastActiveNow();
};


/**
 * will update last active timestamp as long as startLastActive has been called, and clearLastActive has not been called yet.
 * @param epochMillis
 */
export const setLastActive = (epochMillis: number): void => {
  // only update last active timestamp if we have started tracking (empty value means we haven't started)
  if (getLastActiveEpochMillis()) {
    console.log('ACTIVE: ' + epochMillis);
    window.localStorage.setItem(
      INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE,
      epochMillis.toString()
    );
  } else {
    console.log('IGNORE: ' + epochMillis);
  }
};

/**
 * will update last active timestamp to "now" as long as startLastActive has been called, and clearLastActive has not been called yet.
 */
export const setLastActiveNow = () => setLastActive(Date.now());

/**
 * stop tracking last active timestamp
 */
export const clearLastActive = (): void => {
  console.log('CLEAR');
  window.localStorage.removeItem(
    INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
  );
};
