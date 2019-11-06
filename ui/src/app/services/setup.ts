import * as validate from 'validate.js';

export const setupCustomValidators = () => {
  validate.validators.custom = (value, options, key, attributes) => {
    return options.fn(value, key, attributes);
  };

  validate.validators.truthiness = (value, options) => {
    if (value !== options) {
      return `must be ${options}`;
    } else {
      return undefined;
    }
  };
};
