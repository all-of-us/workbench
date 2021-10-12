import * as fp from 'lodash/fp';
import * as React from 'react';
import {CSSProperties} from "react";

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
import {isAbortError} from './errors';
import {isBlank, switchCase} from './index';
import {ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';

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

export const defaultTierConfig = (accessTier: string): InstitutionTierConfig => ({
  accessTierShortName: accessTier,
  membershipRequirement: InstitutionMembershipRequirement.NOACCESS,
  eraRequired: true,
  emailAddresses: [],
  emailDomains: []
});

export function getTierConfig(institution: Institution, accessTierShortName: string): InstitutionTierConfig {
  if (!institution.tierConfigs) {
    return defaultTierConfig(accessTierShortName);
  }

  return institution.tierConfigs.find(t => t.accessTierShortName === accessTierShortName) || defaultTierConfig(accessTierShortName);
}

export function getRegisteredTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Registered);
}

export function getControlledTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Controlled);
}

export function getTierEmailAddresses(institution: Institution, accessTierShortName: string): Array<string> {
  const tierConfig = getTierConfig(institution, accessTierShortName);
  if (tierConfig.emailAddresses) {
    return tierConfig.emailAddresses;
  }
  return [];
}

export function getTierEmailDomains(institution: Institution, accessTierShortName: string): Array<string> {
  const tierConfig = getTierConfig(institution, accessTierShortName);
  if (tierConfig.emailDomains) {
    return tierConfig.emailDomains;
  }
  return [];
}

function mergeTierConfig(institution: Institution, tierConfig: InstitutionTierConfig): Array<InstitutionTierConfig> {
  const otherTierConfigs = institution.tierConfigs.filter(t => t.accessTierShortName !== tierConfig.accessTierShortName);
  return [tierConfig, ...otherTierConfigs];
}

// Update the email addresses of a single tier and return the new tier configs.
export function updateTierEmailAddresses(
    institution: Institution,
    accessTierShortName: string,
    emailAddresses: Array<string>): Array<InstitutionTierConfig> {

  return mergeTierConfig(institution, {
    ...getTierConfig(institution, accessTierShortName),
    emailAddresses
  });
}

// Update the email domains of a single tier and return the new tier configs.
export function updateTierEmailDomains(
    institution: Institution,
    accessTierShortName: string,
    emailDomains: Array<string>): Array<InstitutionTierConfig> {

  return mergeTierConfig(institution, {
    ...getTierConfig(institution, accessTierShortName),
    emailDomains
  });
}

export function updateMembershipRequirement(
    institution: Institution,
    accessTierShortName: string,
    membershipRequirement: InstitutionMembershipRequirement): Array<InstitutionTierConfig> {

  return mergeTierConfig(institution, {
    ...getTierConfig(institution, accessTierShortName),
    membershipRequirement
  })
}

export function updateRequireEra(
    institution: Institution,
    accessTierShortName: string,
    eraRequired: boolean): Array<InstitutionTierConfig> {

  return mergeTierConfig(institution, {
    ...getTierConfig(institution, accessTierShortName),
    eraRequired
  })
}

export function updateEnableControlledTier(
    institution: Institution,
    accessTierShortName: string,
    enableCtAccess: boolean): Array<InstitutionTierConfig> {

  return mergeTierConfig(institution, {
    ...getTierConfig(institution, accessTierShortName),
    membershipRequirement: enableCtAccess === true ?
        InstitutionMembershipRequirement.DOMAINS : InstitutionMembershipRequirement.NOACCESS,
  })
}

export function getTierBadge(accessTierShortName: string): () => JSX.Element {
  const tierBadgeStyle: CSSProperties = {
    marginTop: '0.6rem',
    marginLeft: '0.6rem',
  };

  return () => switchCase(accessTierShortName,
      [AccessTierShortNames.Registered, () => <RegisteredTierBadge style={tierBadgeStyle}/>],
      [AccessTierShortNames.Controlled, () => <ControlledTierBadge style={tierBadgeStyle}/>]);
}
