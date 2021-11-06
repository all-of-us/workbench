import {ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';
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
import {CSSProperties} from "react";

import {isAbortError} from './errors';
import {isBlank, switchCase} from './index';

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

export function getTierConfigOrDefault(configs: Array<InstitutionTierConfig>, accessTierShortName: string): InstitutionTierConfig {
   return configs.find(t => t.accessTierShortName === accessTierShortName) || defaultTierConfig(accessTierShortName);
}

export function getTierConfig(institution: Institution, accessTierShortName: string): InstitutionTierConfig {
  if (!institution.tierConfigs) {
    return defaultTierConfig(accessTierShortName);
  }

  return getTierConfigOrDefault(institution.tierConfigs, accessTierShortName);
}

export function getRegisteredTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Registered);
}

export function getControlledTierConfig(institution: Institution): InstitutionTierConfig {
  return getTierConfig(institution, AccessTierShortNames.Controlled);
}

export function getTierEmailAddresses(tierConfigs: Array<InstitutionTierConfig>, accessTierShortName: string): Array<string> {
  const tierConfig = getTierConfigOrDefault(tierConfigs, accessTierShortName);
  return tierConfig.emailAddresses || [];
}

export function getTierEmailDomains(tierConfigs: Array<InstitutionTierConfig>, accessTierShortName: string): Array<string> {
  const tierConfig = getTierConfigOrDefault(tierConfigs, accessTierShortName);
  return tierConfig.emailDomains || [];
}

function mergeTierConfigs(configs: InstitutionTierConfig[], tierConfig: InstitutionTierConfig): Array<InstitutionTierConfig> {
  const otherTierConfigs = configs.filter(t => t.accessTierShortName !== tierConfig.accessTierShortName);
  return [tierConfig, ...otherTierConfigs];
}

// Update the email addresses of a single tier and return the new tier configs.
export function updateTierEmailAddresses(
    tierConfigs: Array<InstitutionTierConfig>,
    accessTierShortName: string,
    emailAddresses: Array<string>): Array<InstitutionTierConfig> {

  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    emailAddresses
  });
}

// Update the email domains of a single tier and return the new tier configs.
export function updateTierEmailDomains(
    tierConfigs: Array<InstitutionTierConfig>,
    accessTierShortName: string,
    emailDomains: Array<string>): Array<InstitutionTierConfig> {

  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    emailDomains
  });
}

export function updateMembershipRequirement(
    tierConfigs: Array<InstitutionTierConfig>,
    accessTierShortName: string,
    membershipRequirement: InstitutionMembershipRequirement): Array<InstitutionTierConfig> {

  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    membershipRequirement
  })
}

export function updateRequireEra(
    tierConfigs: Array<InstitutionTierConfig>,
    accessTierShortName: string,
    eraRequired: boolean): Array<InstitutionTierConfig> {

  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    eraRequired
  })
}

export function updateEnableControlledTier(
    tierConfigs: Array<InstitutionTierConfig>,
    accessTierShortName: string,
    enableCtAccess: boolean): Array<InstitutionTierConfig> {

  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
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
