import {
  clearLastActive,
  getLastActiveEpochMillis,
  INACTIVITY_CONFIG,
  setLastActive,
  setLastActiveNow,
  setLastActiveRaw,
} from './inactivity';

describe('inactivity last-active accessors', () => {
  it('should round-trip', () => {
    // Arrange
    // need initial value for setLastActive to update value
    setLastActiveRaw(1);

    // Act
    setLastActive(123);

    // Assert
    expect(getLastActiveEpochMillis()).toEqual(123);
  });

  it('should clear', () => {
    // Arrange
    setLastActiveRaw(123);

    // Act
    clearLastActive();

    // Assert
    expect(getLastActiveEpochMillis()).toBeNull();
  });

  // implementation detail useful for debugging
  it('should use the LOCAL_STORAGE_KEY_LAST_ACTIVE location in local storage', () => {
    // Arrange
    const initialValue = window.localStorage.getItem(
      INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
    );

    // Act
    setLastActiveRaw(123);
    const firstUpdate = window.localStorage.getItem(
      INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
    );

    clearLastActive();
    const finalValue = window.localStorage.getItem(
      INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE
    );

    // Asssert
    expect(initialValue).toBeNull();
    expect(firstUpdate).toEqual('123'); // epoch millis as a string;
    expect(finalValue).toBeNull();
  });

  it('should use the current time as the value for setLastActiveNow()', () => {
    // Arrange
    setLastActiveRaw(1);
    jest.useFakeTimers();

    // Act
    setLastActiveNow();

    // Assert
    expect(getLastActiveEpochMillis()).toEqual(Date.now());

    jest.useRealTimers();
  });
});
