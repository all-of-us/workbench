import * as validate from 'validate.js';

export const setupCustomValidators = () => {
  validate.validators.custom = (value, options, key, attributes) => {
    return options.fn(value, key, attributes);
  };

  validate.validators.truthiness = (actualTruthiness, options) => {
    if (actualTruthiness !== options.expected) {
      return `must be ${options.expected}`;
    } else {
      return undefined;
    }
  };
};
