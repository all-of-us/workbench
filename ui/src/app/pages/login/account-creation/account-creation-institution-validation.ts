import {
  CheckEmailResponse,
  InstitutionalRole,
  Profile,
} from 'generated/fetch';

import { isBlank } from 'app/utils';
import {
  formatZodErrors,
  refinedObject,
  refineFields,
  requiredString,
} from 'app/utils/zod-validators';
import { z } from 'zod';

export interface CreateInstitutionFields {
  profile: Profile;
  checkEmailResponse: CheckEmailResponse;
}

export type CheckEmailResponseEx = CheckEmailResponse & {
  existingAccount?: boolean;
};

/**
 * Validate against a CheckEmailResponse API response
 * object. This validator should be enabled when the state object has a non-empty email and
 * institute. It requires that the CheckEmailResponse has returned and indicates that the
 * entered email address is a valid member of the institution.
 *
 * @param value
 */
const validateCheckEmailResponse = (value: CheckEmailResponseEx) => {
  if (value == null) {
    return 'Institutional membership check has not completed';
  }
  if (value?.existingAccount) {
    return 'An account already exists with this email address ';
  } else if (value?.validMember) {
    return null;
  } else {
    return 'Email address is not a member of the selected institution';
  }
};

// Institutional affiliation schema
export const institutionalAffiliationSchema = refinedObject<
  Profile['verifiedInstitutionalAffiliation']
>((fields, ctx) => {
  const noop = undefined;
  return refineFields(fields, ctx, {
    institutionShortName: requiredString(
      'You must select an institution to continue'
    ),
    institutionalRoleEnum: z.nativeEnum(InstitutionalRole, {
      required_error: 'Institutional role cannot be blank',
    }),
    institutionalRoleOtherText:
      fields.institutionalRoleEnum === InstitutionalRole.OTHER
        ? requiredString('Institutional role text cannot be blank').max(80, {
            message: 'Institutional role text must be 80 characters or fewer',
          })
        : noop,
  });
});

// Full institutional validation schema
export const institutionalValidationSchema =
  refinedObject<CreateInstitutionFields>((fields, ctx) => {
    const noop = undefined;

    return refineFields(fields, ctx, {
      profile: refinedObject<Profile>((profile, ctx2) => {
        return refineFields(profile, ctx2, {
          contactEmail: requiredString('Email address cannot be blank').email(
            'Email address is invalid'
          ),
          verifiedInstitutionalAffiliation: institutionalAffiliationSchema,
        });
      }),
      checkEmailResponse:
        !isBlank(
          fields.profile.verifiedInstitutionalAffiliation?.institutionShortName
        ) && !isBlank(fields.profile.contactEmail)
          ? refinedObject<CheckEmailResponse>((response, ctx2) => {
              const error = validateCheckEmailResponse(response);
              if (error) {
                ctx2.addIssue({
                  code: z.ZodIssueCode.custom,
                  message: error,
                });
              }
            })
          : noop,
    });
  });

// Helper function to validate institutional data
export const validateCreateInstitution = (
  profile: Profile,
  checkEmailResponse: CheckEmailResponse
) => {
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
