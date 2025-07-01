import { Profile } from 'generated/fetch';

import { isBlank } from 'app/utils';
import { Country, STATE_CODE_MAPPING } from 'app/utils/constants';
import {
  formatZodErrors,
  httpUrl,
  refinedObject,
  refineFields,
  requiredString,
} from 'app/utils/zod-validators';
import { string, z } from 'zod';

export const stateCodeErrorMessage =
  'State must be a valid 2-letter code (CA, TX, etc.)';

export type AccountCreationFields = Profile & {
  usernameWithEmail: string;
};

export const addressSchema = refinedObject<AccountCreationFields['address']>(
  (fields, ctx) => {
    return refineFields(fields, ctx, {
      streetAddress1: requiredString('Street address cannot be blank').max(95, {
        message: 'Street address must be 95 characters or fewer',
      }),
      streetAddress2: z
        .string()
        .max(95, { message: 'Street address 2 must be 95 characters or fewer' })
        .optional(),
      city: requiredString('City cannot be blank').max(95, {
        message: 'City must be 95 characters or fewer',
      }),
      state: requiredString('State/province/region cannot be blank')
        .max(95, 'State/province/region must be 95 characters or fewer')
        .refine(
          (val) => {
            // Only validate state code for US addresses
            if (!(fields.country === Country.US)) {
              return true;
            }
            return Object.values(STATE_CODE_MAPPING).includes(
              val.toUpperCase()
            );
          },
          { message: 'State must be a valid 2-letter code (CA, TX, etc.)' }
        ),
      zipCode: requiredString('Zip/postal code cannot be blank').max(10, {
        message: 'Zip/postal code must be 10 characters or fewer',
      }),
      country: requiredString('Country cannot be blank').max(95, {
        message: 'Country must be 95 characters or fewer',
      }),
    });
  }
);

// Account creation schema
export const accountCreationSchema = refinedObject<AccountCreationFields>(
  (fields, ctx) => {
    const noop = undefined;
    const errors = refineFields(fields, ctx, {
      username: requiredString('Username cannot be blank')
        .min(4, { message: 'Username must be at least 4 characters' })
        .max(64, {
          message: 'Username is too long (maximum is 64 characters)',
        }),
      usernameWithEmail: !isBlank(fields.username)
        ? z.string().email({ message: `Username contains invalid characters` })
        : noop,
      givenName: requiredString('First name cannot be blank'),
      familyName: requiredString('Last name cannot be blank'),
      areaOfResearch: requiredString(
        'Research description cannot be blank'
      ).max(2000, {
        message: 'Research description must be 2000 characters or fewer',
      }),
      address: addressSchema,
      professionalUrl: z.string().optional().default(''),
    });
    const moreErrors = refineFields({ website: fields.professionalUrl }, ctx, {
      website: !isBlank(fields.professionalUrl)
        ? string().superRefine((url, ctx2) => {
            const tryUrl = httpUrl().safeParse(url);
            console.log('tryUrl ====================', tryUrl);
            if (!tryUrl.success) {
              ctx2.addIssue({
                code: z.ZodIssueCode.custom,
                message: `Professional URL ${url} is not a valid URL`,
              });
            }
          })
        : noop,
    });
    return {
      ...errors,
      ...moreErrors,
    };
  }
);

// Helper function to validate account creation data
export const validateAccountCreation = (fields: AccountCreationFields) => {
  try {
    accountCreationSchema.parse(fields);
  } catch (error) {
    if (error instanceof z.ZodError) {
      return formatZodErrors(error);
    }
    throw error;
  }
};
