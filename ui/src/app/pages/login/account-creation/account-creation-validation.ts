import * as fp from 'lodash/fp';
import validate from 'validate.js';

import { Profile } from 'generated/fetch';

import { isBlank } from 'app/utils';
import { Country, STATE_CODE_MAPPING } from 'app/utils/constants';
import { canonicalizeUrl } from 'app/utils/urls';

export const stateCodeErrorMessage =
  'State must be a valid 2-letter code (CA, TX, etc.)';

export type AccountCreationFields = Profile & {
  usernameWithEmail: string;
};

export const validateAccountCreation = (
  fields: AccountCreationFields
): Record<string, string[]> => {
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
};
