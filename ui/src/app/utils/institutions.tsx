import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {InstitutionalRole, PublicInstitutionDetails} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {isAbortError} from './errors';
import {isBlank} from './index';

/**
 * Checks that the entered email address is a valid member of the chosen institution.
 */
export async function validateEmail(contactEmail: string, institutionShortName: string, aborter: AbortController) {
  try {
    return await institutionApi().checkEmail(institutionShortName, {contactEmail: contactEmail}, {signal: aborter.signal});
  } catch (e) {
    if (isAbortError(e)) {
      // Ignore abort errors.
    } else {
      throw e;
    }
  }
}

export const RestrictedDuaEmailMismatchErrorMessage = () => {
  return <div data-test-id='email-error-message' style={{color: colors.danger}}>
    The institution has authorized access only to select members.<br/>
    Please <a href='https://www.researchallofus.org/institutional-agreements' target='_blank'>
    click here</a> to request to be added to the institution</div>;
};

export const MasterDuaEmailMismatchErrorMessage = () => {
  return <div data-test-id='email-error-message' style={{color: colors.danger}}>
    Your email does not match your institution</div>;
};

export const getRoleOptions = (institutions: Array<PublicInstitutionDetails>, institutionShortName: string):
    Array<{ label: string, value: InstitutionalRole }> => {
  if (isBlank(institutionShortName)) {
    return [];
  }

  const matchedInstitution = fp.find(institution => {
    const {shortName} = institution;
    return shortName === institutionShortName;
  }, institutions);
  if (matchedInstitution === null || matchedInstitution === undefined) {
    return [];
  }

  const {organizationTypeEnum} = matchedInstitution;
  const availableRoles: Array<InstitutionalRole> =
      AccountCreationOptions.institutionalRolesByOrganizationType
      .find(obj => obj.type === organizationTypeEnum)
          .roles;

  return AccountCreationOptions.institutionalRoleOptions.filter(option =>
      availableRoles.includes(option.value)
  );
};
