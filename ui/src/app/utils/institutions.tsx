import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Institution,
  InstitutionalRole,
  InstitutionMembershipRequirement,
  InstitutionTierConfig,
  PublicInstitutionDetails,
} from 'generated/fetch';

import { cond, switchCase } from '@terra-ui-packages/core-utils';
import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import { institutionApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { getCustomOrDefaultUrl } from 'app/utils/urls';

import { isAbortError } from './errors';
import { isBlank } from './index';

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

const EmailAddressMismatchErrorMessage = ({
  requestAccessUrl,
}: {
  requestAccessUrl: string;
}) => {
  const defaultUrl = 'https://www.researchallofus.org/institutional-agreements';
  const url = getCustomOrDefaultUrl(requestAccessUrl, defaultUrl);

  return (
    <div data-test-id='email-error-message' style={{ color: colors.danger }}>
      The institution has authorized access only to select members.
      <br />
      Please{' '}
      <a href={url} target='_blank'>
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
    return (
      <EmailAddressMismatchErrorMessage
        requestAccessUrl={institution.requestAccessUrl}
      />
    );
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

// initialize a new CT config depending on the RT config:
// RT = DOMAINS -> copy the DOMAINS list to CT
// RT = ADDRESSES -> use ADDRESSES in CT but do not copy the list
// RT = UNINITIALIZED (add mode) -> CT is also UNINITIALIZED
const initControlledTierConfig = (
  tierConfigs: InstitutionTierConfig[]
): InstitutionTierConfig => {
  const rtConfig = tierConfigs.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Registered
  );

  return switchCase(
    rtConfig.membershipRequirement,
    [
      // if RT is DOMAINS, copy RT to CT
      InstitutionMembershipRequirement.DOMAINS,
      () => ({
        ...rtConfig,
        accessTierShortName: AccessTierShortNames.Controlled,
      }),
    ],
    [
      // if RT is ADDRESSES, copy RT to CT but clear the address list
      InstitutionMembershipRequirement.ADDRESSES,
      () => ({
        ...rtConfig,
        accessTierShortName: AccessTierShortNames.Controlled,
        emailAddresses: [],
      }),
    ],
    [
      // if RT is UNINITIALIZED, copy UNINITIALIZED RT to CT
      InstitutionMembershipRequirement.UNINITIALIZED,
      () => ({
        ...rtConfig,
        accessTierShortName: AccessTierShortNames.Controlled,
      }),
    ]
  );
};

// 3 scenarios here:
// disable CT -> use the default no-render tier config for CT
// enable CT for the first time -> initControlledTierConfig() from RT
// re-enable CT after disabling without saving -> use the pre-changes CT
export function updateEnableControlledTier(
  tierConfigs: InstitutionTierConfig[],
  tierConfigsBeforeEditing: InstitutionTierConfig[],
  enableCtAccess: boolean
): InstitutionTierConfig[] {
  // if the before-editing institution had a CT and we are restoring it, use the before-editing CT
  const previousCtConfig = tierConfigsBeforeEditing?.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Controlled
  );

  const newCtConfig = cond(
    [!enableCtAccess, () => defaultTierConfig(AccessTierShortNames.Controlled)],
    [enableCtAccess && !!previousCtConfig, () => previousCtConfig],
    [
      enableCtAccess && !previousCtConfig,
      () => initControlledTierConfig(tierConfigs),
    ]
  );

  return mergeTierConfigs(tierConfigs, newCtConfig);
}

export const getAdminUrl = (shortName: string) =>
  `/admin/institution/edit/${shortName}`;
