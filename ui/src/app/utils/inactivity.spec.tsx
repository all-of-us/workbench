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
    expect(getLastActiveEpochMillis()).toBeUndefined();
  });

  it('should store values as epoch milli strings', () => {
    setLastActive(123);
    expect(
      window.localStorage.getItem(
        INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
      )
    ).toEqual('12345');
  });

  it('should set the current value', () => {
    setLastActiveNow();
    expect(getLastActiveEpochMillis()).toEqual(Date.now());
  });
});
