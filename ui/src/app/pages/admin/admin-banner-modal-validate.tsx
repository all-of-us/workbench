import validate from 'validate.js';

import { StatusAlert } from 'generated/fetch';

import { requiredString, zodToValidateJS } from 'app/utils/zod-validators';
import { z } from 'zod';

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
  return validationResult as { [key: string]: Array<string> } | undefined;
};

// V2 with zod ================================================================

// Banner validation schema
export const bannerValidationSchema = z.object({
  title: requiredString('Please enter a banner title'),
  message: requiredString('Please enter a banner message'),
  startTimeEpochMillis: z.number({
    required_error: 'Please enter a start time',
  }),
  alertLocation: requiredString('Please select a banner location'),
});

export const validateBannerAlertV2 = (data: StatusAlert) =>
  zodToValidateJS(() => bannerValidationSchema.parse(data));
