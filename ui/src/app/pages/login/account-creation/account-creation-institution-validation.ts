import validate from 'validate.js';

import {
  CheckEmailResponse,
  InstitutionalRole,
  Profile,
} from 'generated/fetch';

import { isBlank } from 'app/utils';
import { notTooLong } from 'app/utils/validators';

export type CreateInstitutionFields = Pick<
  Profile,
  'contactEmail' | 'verifiedInstitutionalAffiliation'
>;

export type CheckEmailResponseEx = CheckEmailResponse & {
  existingAccount?: boolean;
};

/**
 * Create a custom validate.js validator to validate against a CheckEmailResponse API response
 * object. This validator should be enabled when the state object has a non-empty email and
 * institute. It requires that the CheckEmailResponse has returned and indicates that the
 * entered email address is a valid member of the institution.
 *
 * @param value
 */
validate.validators.checkEmailResponse = (value: CheckEmailResponseEx) => {
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

export const validateCreateInstitution = (
  profile: CreateInstitutionFields,
  checkEmailResponse: CheckEmailResponse
): { [key: string]: Array<string> } => {
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
        profile.verifiedInstitutionalAffiliation?.institutionShortName
      ) && !isBlank(profile.contactEmail)
        ? {
            checkEmailResponse: {},
          }
        : {},
    'profile.verifiedInstitutionalAffiliation.institutionalRoleOtherText':
      profile.verifiedInstitutionalAffiliation?.institutionalRoleEnum ===
      InstitutionalRole.OTHER
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
};
