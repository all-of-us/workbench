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
  const defaultTierConfig : InstitutionTierConfig = {
    accessTierShortName: accessTier,
        membershipRequirement: InstitutionMembershipRequirement.NOACCESS,
        eraRequired: true,
        emailAddresses: [],
        emailDomains: []
  }
  if (!institution.tierConfigs || !institution.tierConfigs.find(t => t.accessTierShortName === accessTier)) {
    return defaultTierConfig
  }
  return institution.tierConfigs.find(t => t.accessTierShortName === accessTier);
}

export function getRegisteredTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Registered);
}

export function getControlledTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Controlled);
}

export function getRegisteredTierEmailAddresses(institution: Institution): Array<string> {
  return getTierEmailAddresses(institution, AccessTierShortNames.Registered);
}

export function getControlledTierEmailAddresses(institution: Institution): Array<string> {
  return getTierEmailAddresses(institution, AccessTierShortNames.Controlled);
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

export function getControlledTierEmailDomains(institution: Institution): Array<string> {
  return getTierEmailDomains(institution, AccessTierShortNames.Controlled);
}

export function getTierEmailDomains(institution: Institution, accessTier: string): Array<string> {
  const tierConfig = getTierConfig(institution, accessTier);
  if (tierConfig.emailDomains) {
    return tierConfig.emailDomains;
  }
  return [];
}

// Update User register tier email addresses and return the new tier configs.
export function updateRtEmailAddresses(institution: Institution, emailAddresses: Array<string>): Array<InstitutionTierConfig> {
  const rtTierConfig = {
    ...getRegisteredTierConfig(institution),
    emailAddresses: emailAddresses
  };
  return [rtTierConfig, getControlledTierConfig(institution)];
}

// Update User controlled tier email addresses and return the new tier configs.
export function updateCtEmailAddresses(institution: Institution, emailAddresses: Array<string>): Array<InstitutionTierConfig> {
  const ctConfig = {
    ...getControlledTierConfig(institution),
    emailAddresses: emailAddresses
  };
  return [getRegisteredTierConfig(institution), ctConfig];
}

// Update User register tier email domains and return the new tier configs.
export function updateRtEmailDomains(institution: Institution, emailDomains: Array<string>): Array<InstitutionTierConfig> {
  const rtTierConfig = {
    ...getRegisteredTierConfig(institution),
    emailDomains: emailDomains
  };
  return [rtTierConfig, getControlledTierConfig(institution)];
}

// Update User controlled tier email domains and return the new tier configs.
export function updateCtEmailDomains(institution: Institution, emailDomains: Array<string>): Array<InstitutionTierConfig> {
  const ctConfig = {
    ...getControlledTierConfig(institution),
    emailDomains: emailDomains
  };
  return [getRegisteredTierConfig(institution), ctConfig];
}