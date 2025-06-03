import validate from 'validate.js';
import { isBlank } from 'app/utils';
import { notTooLong } from 'app/utils/validators';
import { CheckEmailResponse, InstitutionalRole, Profile } from 'generated/fetch';

import { z } from 'zod';
import { formatZodErrors, requiredString, refinedObject, refineFields } from 'app/utils/zod-validators';


export type CreateInstitutionFields = Profile;

/**
 * Create a custom validate.js validator to validate against a CheckEmailResponse API response
 * object. This validator should be enabled when the state object has a non-empty email and
 * institute. It requires that the CheckEmailResponse has returned and indicates that the
 * entered email address is a valid member of the institution.
 *
 * @param value
 */
validate.validators.checkEmailResponse = (value: CheckEmailResponse) => {
  if (value == null) {
    return '^Institutional membership check has not completed';
  }
  if (value?.existingAccount) {
    return '^An account already exists with this email address ';
  } else if (value?.validMember) {
    return null;
  } else {
    return '^Email address is not a member of the selected institution';
  }
};

export const validateCreateInstitution = (profile: CreateInstitutionFields, checkEmailResponse: CheckEmailResponse): { [key: string]: Array<string> } => {
  const validationCheck = {
    'profile.verifiedInstitutionalAffiliation.institutionShortName': {
      presence: {
        allowEmpty: false,
        message: '^You must select an institution to continue',
      },
    },
    'profile.contactEmail': {
      presence: {
        allowEmpty: false,
        message: '^Email address cannot be blank',
      },
      email: {
        message: '^Email address is invalid',
      },
    },
    'profile.verifiedInstitutionalAffiliation.institutionalRoleEnum': {
      presence: {
        allowEmpty: false,
        message: '^Institutional role cannot be blank',
      },
    },
    checkEmailResponse:
      !isBlank(
        profile.verifiedInstitutionalAffiliation
          ?.institutionShortName
      ) && !isBlank(profile.contactEmail)
        ? {
            checkEmailResponse: {},
          }
        : {},
    'profile.verifiedInstitutionalAffiliation.institutionalRoleOtherText':
      profile.verifiedInstitutionalAffiliation
        ?.institutionalRoleEnum === InstitutionalRole.OTHER
        ? {
            ...notTooLong(80),
            presence: {
              allowEmpty: false,
              message: '^Institutional role text cannot be blank',
            },
          }
        : {},
  };

  return validate({ profile, checkEmailResponse }, validationCheck);
}

// V2 validation with zod ================================================================
// Email validation response schema
export const emailValidationSchema = z.object({
  validMember: z.boolean(),
  existingAccount: z.boolean().optional()
}).refine((data) => {
  if (data.existingAccount) {
    return false;
  }
  return data.validMember;
}, {
  message: "Email validation failed"
});

// Institutional affiliation schema
export const institutionalAffiliationSchema = refinedObject<
  Profile['verifiedInstitutionalAffiliation']
>((fields, ctx) => {
  const noop = undefined;
  return refineFields(fields, ctx, {
    institutionShortName: requiredString("You must select an institution to continue"),
    institutionalRoleEnum: z.nativeEnum(InstitutionalRole, { 
      required_error: "Institutional role cannot be blank" 
    }),
    institutionalRoleOtherText: (fields.institutionalRoleEnum === InstitutionalRole.OTHER)
      ? requiredString("Institutional role text cannot be blank").max(80, {
        message: "Institutional role text must be 80 characters or fewer"
      })
      : noop,
  })
});

// Full institutional validation schema
export const institutionalValidationSchema = z.object({
  profile: refinedObject<Profile>((fields, ctx) => {
    return refineFields(fields, ctx, {
      contactEmail: requiredString("Email address cannot be blank").email("Email address is invalid"),
      verifiedInstitutionalAffiliation: institutionalAffiliationSchema
    })
  }),
  checkEmailResponse: emailValidationSchema
});

// Helper function to validate institutional data
export const validateCreateInstitutionV2 = (profile: Profile, checkEmailResponse: CheckEmailResponse) => {
  try {
    // stripFieldNames(institutionalValidationSchema).parse(data);
    institutionalValidationSchema.parse({ profile, checkEmailResponse });
    return undefined;
  } catch (error) {
    if (error instanceof z.ZodError) {
      return formatZodErrors(error);
    }
    throw error;
  }
};