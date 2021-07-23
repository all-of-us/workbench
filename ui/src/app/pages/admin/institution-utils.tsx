import {AccessTierShortNames} from 'app/utils/access-tiers';
import {Institution, InstitutionTierRequirement} from 'generated/fetch';

export function getTierEmailAddresses(institution: Institution, accessTier: string): Array<string> {
  if (institution.tierEmailAddresses || institution.tierEmailAddresses === null) {
    return [];
  } else {
    return institution.tierEmailAddresses.find(t => t.accessTierShortName === accessTier).emailAddresses;
  }
}

export function getTierEmailDomains(institution: Institution, accessTier: string): Array<string> {
  if (institution.tierEmailDomains || institution.tierEmailDomains === null) {
    return [];
  } else {
    return institution.tierEmailDomains.find(t => t.accessTierShortName === accessTier).emailDomains;
  }
}

function getTierRequirement(institution: Institution, accessTier: string): InstitutionTierRequirement {
  return institution.tierRequirements.find(t => t.accessTierShortName === accessTier);
}

export function getRegisteredTierRequirement(institution: Institution): InstitutionTierRequirement {
  return getTierRequirement(institution, AccessTierShortNames.Registered);
}
