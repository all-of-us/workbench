import {
  clearLastActive,
  getLastActiveEpochMillis,
  INACTIVITY_CONFIG,
  setLastActive,
  setLastActiveNow,
} from './inactivity';

describe('inactivity last-active accessors', () => {
  it('should round-trip', () => {
    setLastActive(123);
    expect(getLastActiveEpochMillis()).toEqual(123);
  });

  it('should clear', () => {
    setLastActive(123);
    clearLastActive();
    expect(getLastActiveEpochMillis()).toBeNull();
  });

  // implementation detail useful for debugging
  it('should use the LOCAL_STORAGE_KEY_LAST_ACTIVE location in local storage', () => {
    expect(
      window.localStorage.getItem(
        INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
      )
    ).toBeNull();

    setLastActive(123);
    expect(
      window.localStorage.getItem(
        INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
      )
    ).toEqual('123'); // epoch millis as a string

    clearLastActive();
    expect(
      window.localStorage.getItem(
        INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
      )
    ).toBeNull();
  });

  it('should use the current time as the value for setLastActiveNow()', () => {
    jest.useFakeTimers();
    setLastActiveNow();
    expect(getLastActiveEpochMillis()).toEqual(Date.now());
    jest.useRealTimers();
  });
});
