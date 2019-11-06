import * as validate from 'validate.js';

export const setupCustomValidators = () => {
  validate.validators.custom = (value, options, key, attributes) => {
    return options.fn(value, key, attributes);
  };

  validate.validators.truthiness = (actualTruthiness, expectedTruthiness) => {
    if (actualTruthiness !== expectedTruthiness) {
      return `must be ${expectedTruthiness}`;
    } else {
      return undefined;
    }
  };
};
