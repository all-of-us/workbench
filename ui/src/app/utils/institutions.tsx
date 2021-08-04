import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {
  Institution,
  InstitutionalRole,
  InstitutionMembershipRequirement,
  InstitutionTierConfig,
  PublicInstitutionDetails
} from 'generated/fetch';
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

export const EmailAddressMismatchErrorMessage = () => {
  return <div data-test-id='email-error-message' style={{color: colors.danger}}>
    The institution has authorized access only to select members.<br/>
    Please <a href='https://www.researchallofus.org/institutional-agreements' target='_blank'>
    click here</a> to request to be added to the institution</div>;
};

export const EmailDomainMismatchErrorMessage = () => {
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
  if (matchedInstitution === undefined) {
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

function getTierConfig(institution: Institution, accessTier: string): InstitutionTierConfig {
  if (!institution.tierConfigs) {
    return {
      accessTierShortName: accessTier,
      membershipRequirement: InstitutionMembershipRequirement.NOACCESS,
      emailAddresses: [],
      emailDomains: []
    };
  }
  return institution.tierConfigs.find(t => t.accessTierShortName === accessTier);
}

export function getRegisteredTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Registered);
}

export function getRegisteredTierEmailAddresses(institution: Institution): Array<string> {
  return getTierEmailAddresses(institution, AccessTierShortNames.Registered);
}

export function getTierEmailAddresses(institution: Institution, accessTier: string): Array<string> {
  const tierConfig = getTierConfig(institution, accessTier);
  if (tierConfig.emailAddresses) {
    return tierConfig.emailAddresses;
  }
  return [];
}

export function getRegisteredTierEmailDomains(institution: Institution): Array<string> {
  return getTierEmailDomains(institution, AccessTierShortNames.Registered);
}

export function getTierEmailDomains(institution: Institution, accessTier: string): Array<string> {
  const tierConfig = getTierConfig(institution, accessTier);
  if (tierConfig.emailDomains) {
    return tierConfig.emailDomains;
  }
  return [];
}
