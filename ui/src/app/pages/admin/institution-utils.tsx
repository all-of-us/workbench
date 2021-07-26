import {AccessTierShortNames} from 'app/utils/access-tiers';
import {
  Institution,
  InstitutionMembershipRequirement,
  InstitutionTierRequirement
} from 'generated/fetch';

export function getRegisteredTierEmailAddresses(institution: Institution): Array<string> {
  return getTierEmailAddresses(institution, AccessTierShortNames.Registered);
}

export function getTierEmailAddresses(institution: Institution, accessTier: string): Array<string> {
  if (institution.tierEmailAddresses) {
    const tierEmailAddresses = institution.tierEmailAddresses.find(t => t.accessTierShortName === accessTier);
    if (tierEmailAddresses) {
      return institution.tierEmailAddresses.find(t => t.accessTierShortName === accessTier).emailAddresses;
    }
  }
  return [];
}

export function getRegisteredTierEmailDomains(institution: Institution): Array<string> {
  return getTierEmailDomains(institution, AccessTierShortNames.Registered);
}

export function getTierEmailDomains(institution: Institution, accessTier: string): Array<string> {
  if (institution.tierEmailDomains) {
    const tierEmailDomains = institution.tierEmailDomains.find(t => t.accessTierShortName === accessTier);
    if (tierEmailDomains) {
      return institution.tierEmailDomains.find(t => t.accessTierShortName === accessTier).emailDomains;
    }
  }
  return [];
}

export function getRegisteredTierRequirement(institution: Institution): InstitutionTierRequirement {
  return getTierRequirement(institution, AccessTierShortNames.Registered);
}


function getTierRequirement(institution: Institution, accessTier: string): InstitutionTierRequirement {
  if (!institution.tierRequirements) {
    return {accessTierShortName: accessTier, membershipRequirement: InstitutionMembershipRequirement.NOACCESS};
  }
  return institution.tierRequirements.find(t => t.accessTierShortName === accessTier);
}
