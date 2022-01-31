import * as fp from 'lodash/fp';
import * as React from 'react';
import { CSSProperties } from 'react';

import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import { institutionApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  Institution,
  InstitutionalRole,
  InstitutionMembershipRequirement,
  InstitutionTierConfig,
  PublicInstitutionDetails,
} from 'generated/fetch';
import { isAbortError } from './errors';
import { isBlank, switchCase } from './index';
import { ControlledTierBadge, RegisteredTierBadge } from 'app/components/icons';

/**
 * Checks that the entered email address is a valid member of the chosen institution.
 */
export async function checkInstitutionalEmail(
  contactEmail: string,
  institutionShortName: string,
  aborter: AbortController
) {
  try {
    return await institutionApi().checkEmail(
      institutionShortName,
      { contactEmail },
      { signal: aborter.signal }
    );
  } catch (e) {
    if (isAbortError(e)) {
      // Ignore abort errors.
    } else {
      throw e;
    }
  }
}

const EmailAddressMismatchErrorMessage = () => {
  return (
    <div data-test-id='email-error-message' style={{ color: colors.danger }}>
      The institution has authorized access only to select members.
      <br />
      Please{' '}
      <a
        href='https://www.researchallofus.org/institutional-agreements'
        target='_blank'
      >
        click here
      </a>{' '}
      to request to be added to the institution
    </div>
  );
};

const EmailDomainMismatchErrorMessage = () => {
  return (
    <div data-test-id='email-error-message' style={{ color: colors.danger }}>
      Your email does not match your institution
    </div>
  );
};

export const getEmailValidationErrorMessage = (
  institution: PublicInstitutionDetails
) => {
  if (!institution) {
    return null;
  }

  if (
    institution.registeredTierMembershipRequirement ===
    InstitutionMembershipRequirement.ADDRESSES
  ) {
    // Institution requires an exact email address match and the email is not in allowed emails list
    return <EmailAddressMismatchErrorMessage />;
  } else {
    // Institution requires email domain matching and the domain is not in the allowed list
    return <EmailDomainMismatchErrorMessage />;
  }
};

export const getRoleOptions = (
  institutions: Array<PublicInstitutionDetails>,
  institutionShortName?: string
): Array<{ label: string; value: InstitutionalRole }> => {
  if (isBlank(institutionShortName)) {
    return [];
  }

  const matchedInstitution = fp.find((institution) => {
    const { shortName } = institution;
    return shortName === institutionShortName;
  }, institutions);
  if (matchedInstitution === undefined) {
    return [];
  }

  const { organizationTypeEnum } = matchedInstitution;
  const availableRoles: Array<InstitutionalRole> =
    AccountCreationOptions.institutionalRolesByOrganizationType.find(
      (obj) => obj.type === organizationTypeEnum
    ).roles;

  return AccountCreationOptions.institutionalRoleOptions.filter((option) =>
    availableRoles.includes(option.value)
  );
};

export const defaultTierConfig = (
  accessTier: string
): InstitutionTierConfig => ({
  accessTierShortName: accessTier,
  membershipRequirement: InstitutionMembershipRequirement.NOACCESS,
  eraRequired: false,
  emailAddresses: [],
  emailDomains: [],
});

export function getTierConfigOrDefault(
  configs: Array<InstitutionTierConfig>,
  accessTierShortName: string
): InstitutionTierConfig {
  return (
    configs?.find((t) => t.accessTierShortName === accessTierShortName) ||
    defaultTierConfig(accessTierShortName)
  );
}

export function getTierConfig(
  institution: Institution,
  accessTierShortName: string
): InstitutionTierConfig {
  return getTierConfigOrDefault(institution?.tierConfigs, accessTierShortName);
}

export function getTierEmailAddresses(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string
): Array<string> {
  const tierConfig = getTierConfigOrDefault(tierConfigs, accessTierShortName);
  return tierConfig.emailAddresses || [];
}

export function getTierEmailDomains(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string
): Array<string> {
  const tierConfig = getTierConfigOrDefault(tierConfigs, accessTierShortName);
  return tierConfig.emailDomains || [];
}

function mergeTierConfigs(
  configs: InstitutionTierConfig[],
  tierConfig: InstitutionTierConfig
): Array<InstitutionTierConfig> {
  const otherTierConfigs = configs.filter(
    (t) => t.accessTierShortName !== tierConfig.accessTierShortName
  );
  return [tierConfig, ...otherTierConfigs];
}

// Update the email addresses of a single tier and return the new tier configs.
export function updateTierEmailAddresses(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string,
  emailAddresses: Array<string>
): Array<InstitutionTierConfig> {
  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    emailAddresses,
  });
}

// Update the email domains of a single tier and return the new tier configs.
export function updateTierEmailDomains(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string,
  emailDomains: Array<string>
): Array<InstitutionTierConfig> {
  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    emailDomains,
  });
}

export function updateMembershipRequirement(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string,
  membershipRequirement: InstitutionMembershipRequirement
): Array<InstitutionTierConfig> {
  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    membershipRequirement,
  });
}

export function updateRequireEra(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string,
  eraRequired: boolean
): Array<InstitutionTierConfig> {
  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    eraRequired,
  });
}

export function updateEnableControlledTier(
  tierConfigs: Array<InstitutionTierConfig>,
  accessTierShortName: string,
  enableCtAccess: boolean
): Array<InstitutionTierConfig> {
  return mergeTierConfigs(tierConfigs, {
    ...getTierConfigOrDefault(tierConfigs, accessTierShortName),
    membershipRequirement:
      enableCtAccess === true
        ? InstitutionMembershipRequirement.DOMAINS
        : InstitutionMembershipRequirement.NOACCESS,
  });
}

export function getTierBadge(accessTierShortName: string): () => JSX.Element {
  const tierBadgeStyle: CSSProperties = {
    marginTop: '0.6rem',
    marginLeft: '0.6rem',
  };

  return () =>
    switchCase(
      accessTierShortName,
      [
        AccessTierShortNames.Registered,
        () => <RegisteredTierBadge style={tierBadgeStyle} />,
      ],
      [
        AccessTierShortNames.Controlled,
        () => <ControlledTierBadge style={tierBadgeStyle} />,
      ]
    );
}
