import { inRange } from './numbers';

describe('inRange', () => {
  it('should return true when value is within range', () => {
    expect(inRange(5, 1, 10)).toBe(true);
    expect(inRange(1, 1, 10)).toBe(true);  // Edge case: value equals lower bound
    expect(inRange(10, 1, 10)).toBe(true); // Edge case: value equals upper bound
  });

  it('should return false when value is outside range', () => {
    expect(inRange(0, 1, 10)).toBe(false);
    expect(inRange(11, 1, 10)).toBe(false);
  });

  it('should handle undefined bounds properly', () => {
    expect(inRange(5)).toBe(true); // No bounds specified, should always be true
    expect(inRange(5, 1)).toBe(true); // Only lower bound specified
    expect(inRange(5, undefined, 10)).toBe(true); // Only upper bound specified
    expect(inRange(-100, undefined, 10)).toBe(true); // Only upper bound specified, negative value
    expect(inRange(100, 1, undefined)).toBe(true); // Only lower bound specified, large value
  });

  it('should handle edge cases with bounds', () => {
    expect(inRange(Number.MAX_SAFE_INTEGER, 0)).toBe(true);
    expect(inRange(Number.MIN_SAFE_INTEGER, undefined, 0)).toBe(true);
  });

  it('should return false for null or undefined values', () => {
    // @ts-ignore: Testing runtime behavior with invalid types
    expect(inRange(null, 1, 10)).toBe(false);
    // @ts-ignore: Testing runtime behavior with invalid types
    expect(inRange(undefined, 1, 10)).toBe(false);
  });
});