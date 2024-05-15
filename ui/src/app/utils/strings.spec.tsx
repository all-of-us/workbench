import { oxfordCommaString } from './strings';

describe(oxfordCommaString.name, () => {
  it('Should return the correct value for 1 element', () => {
    expect(oxfordCommaString(['a'])).toEqual('a');
  });

  it('Should return the correct value for 2 elements', () => {
    expect(oxfordCommaString(['a', 'b'])).toEqual('a and b');
  });

  it('Should return the correct value for 3 elements', () => {
    expect(oxfordCommaString(['a', 'b', 'c'])).toEqual('a, b, and c');
  });

  it('Should return the correct value for more than 3 elements', () => {
    expect(oxfordCommaString(['a', 'b', 'c', 'd'])).toEqual('a, b, c, and d');
  });

  it('Should return an empty string for an empty array', () => {
    expect(oxfordCommaString([])).toEqual('');
  });

  it('Should handle null and undefined inputs gracefully', () => {
    expect(oxfordCommaString(null)).toBeNull();
    expect(oxfordCommaString(undefined)).toBeUndefined();
  });
});
