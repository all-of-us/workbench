import { StatusAlert } from 'generated/fetch';

import { requiredString, zodToValidateJS } from 'app/utils/zod-validators';
import { z } from 'zod';

// Banner validation schema
export const bannerValidationSchema = z.object({
  title: requiredString('Please enter a banner title'),
  message: requiredString('Please enter a banner message'),
  startTimeEpochMillis: z.number({
    required_error: 'Please enter a start time',
  }),
  alertLocation: requiredString('Please select a banner location'),
});

export const validateBannerAlert = (data: StatusAlert) =>
  zodToValidateJS(() => bannerValidationSchema.parse(data));
