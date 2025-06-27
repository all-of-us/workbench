import validate from 'validate.js';

import { StatusAlert } from 'generated/fetch';

const BANNER_VALIDATION_CONSTRAINTS = {
  title: {
    presence: {
      allowEmpty: false,
      message: 'Please enter a banner title',
    },
  },
  message: {
    presence: {
      allowEmpty: false,
      message: 'Please enter a banner message',
    },
  },
  startTimeEpochMillis: {
    presence: {
      allowEmpty: false,
      message: 'Please enter a start time',
    },
  },
  alertLocation: {
    presence: {
      message: 'Please select a banner location',
    },
  },
};

export const validateBannerAlert = (banner: StatusAlert) => {
  const validationResult = validate(banner, BANNER_VALIDATION_CONSTRAINTS, {
    fullMessages: false,
  });
  return validationResult as Record<string, string[]> | undefined;
};
