import * as fp from 'lodash/fp';
import validate from 'validate.js';
import { isBlank } from 'app/utils';
import { Country, STATE_CODE_MAPPING } from 'app/utils/constants';
import { canonicalizeUrl } from 'app/utils/urls';
import { Profile } from 'generated/fetch';

import { z } from 'zod';
import { formatZodErrors, refinedObject, refineFields, requiredString } from 'app/utils/zod-validators';


export const stateCodeErrorMessage =
  'State must be a valid 2-letter code (CA, TX, etc.)';

export type AccountCreationFields = Profile & {
    usernameWithEmail: string;
}

export const validateAccountCreation = (fields: AccountCreationFields): { [key: string]: string } => {
    // const { gsuiteDomain } = serverConfigStore.get().config;

    // The validation data for this form is *almost* the raw Profile, except for the additional
    // 'usernameWithEmail' field we're adding, to be able to separate our validation on the
    // username itself from validation of the full email address.
    const validationData = fields;

    const validationCheck = {
      username: {
        presence: {
          allowEmpty: false,
          message: '^Username cannot be blank',
        },
        length: {
          minimum: 4,
          maximum: 64,
        },
      },
      usernameWithEmail: isBlank(validationData.username)
        ? {}
        : {
            email: {
              message: '^Username contains invalid characters',
            },
          },
      givenName: {
        presence: {
          allowEmpty: false,
          message: '^First name cannot be blank',
        },
      },
      familyName: {
        presence: {
          allowEmpty: false,
          message: '^Last name cannot be blank',
        },
      },
      areaOfResearch: {
        presence: {
          allowEmpty: false,
          message: '^Research description cannot be blank',
        },
        length: {
          maximum: 2000,
          message: '^Research description must be 2000 characters or fewer',
        },
      },
      'address.streetAddress1': {
        presence: {
          allowEmpty: false,
          message: '^Street address cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^Street address must be 95 characters or fewer',
        },
      },
      'address.streetAddress2': {
        length: {
          maximum: 95,
          message: '^Street address 2 must be 95 characters or fewer',
        },
      },
      'address.city': {
        presence: {
          allowEmpty: false,
          message: '^City cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^City must be 95 characters or fewer',
        },
      },
      'address.state': {
        presence: {
          allowEmpty: false,
          message: '^State/province/region cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^State/province/region must be 95 characters or fewer',
        },
        inclusion: (_value, attributes) => {
          if (attributes.address.country === Country.US) {
            return {
              within: Object.values(STATE_CODE_MAPPING),
              message: `^${stateCodeErrorMessage}`,
            };
          }
          return false;
        },
      },
      'address.zipCode': {
        presence: {
          allowEmpty: false,
          message: '^Zip/postal code cannot be blank',
        },
        length: {
          maximum: 10,
          message: '^Zip/postal code must be 10 characters or fewer',
        },
      },
      'address.country': {
        presence: {
          allowEmpty: false,
          message: '^Country cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^Country must be 95 characters or fewer',
        },
      },
    };

    // validatejs requires a scheme, which we don't necessarily need in the profile; rather than
    // forking their website regex, just ensure a scheme ahead of validation.
    const { professionalUrl } = validationData;
    const urlError = professionalUrl
      ? validate(
          { website: canonicalizeUrl(professionalUrl) },
          {
            website: {
              url: {
                message: `^Professional URL ${professionalUrl} is not a valid URL`,
              },
            },
          }
        )
      : undefined;

    const errors = {
      ...validate(validationData, validationCheck),
      ...urlError,
    };

    return fp.isEmpty(errors) ? undefined : errors;
  }

// V2 validation with zod ================================================================

export const addressSchema = refinedObject<AccountCreationFields['address']>((fields, ctx) => {
  return refineFields(fields, ctx, {
    streetAddress1: requiredString("Street address cannot be blank").max(95, { message: "Street address must be 95 characters or fewer" }),
    streetAddress2: z.string().max(95, { message: "Street address 2 must be 95 characters or fewer" }).optional(),
    city: requiredString("City cannot be blank").max(95, { message: "City must be 95 characters or fewer" }),
    state: requiredString("State/province/region cannot be blank")
      .refine((val) => {
        // Only validate state code for US addresses
        if (!(fields.country === Country.US)) {
          return true;
        }
        return Object.values(STATE_CODE_MAPPING).includes(val.toUpperCase());
      }, { message: "State must be a valid 2-letter code (CA, TX, etc.)" }),
    zipCode: requiredString("Zip/postal code cannot be blank").max(10, { message: "Zip/postal code must be 10 characters or fewer" }),
    country: requiredString("Country cannot be blank").max(95, { message: "Country must be 95 characters or fewer" })
  });
});

// Account creation schema 
export const accountCreationSchema = refinedObject<AccountCreationFields>((fields, ctx) => {
  const noop = undefined;
  return refineFields(fields, ctx, {
    username: requiredString("Username cannot be blank")
      .min(4, { message: "Username must be at least 4 characters" })
      .max(64, { message: "Username must be 64 characters or fewer" }),
    usernameWithEmail: !isBlank(fields.username) 
      ? z.string().email({ message: `Username contains invalid characters` }) 
      : noop,
    givenName: requiredString("First name cannot be blank"),
    familyName: requiredString("Last name cannot be blank"),
    areaOfResearch: requiredString("Research description cannot be blank")
      .max(2000, { message: "Research description must be 2000 characters or fewer" }),
    address: addressSchema,
    professionalUrl: z.string().url({ message: `Professional URL ${fields.professionalUrl} is not a valid URL` }).or(z.literal('')).optional(),
  });
});

// Helper function to validate account creation data
export const validateAccountCreationV2 = (fields: AccountCreationFields) => {
  try {
    accountCreationSchema.parse(fields);
  } catch (error) {
    if (error instanceof z.ZodError) {
      return formatZodErrors(error);
    }
    throw error;
  }
};
