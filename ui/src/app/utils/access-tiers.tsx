import { Profile } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';

export enum AccessTierShortNames {
  Registered = 'registered',
  Controlled = 'controlled',
}

export const orderedAccessTierShortNames = [
  AccessTierShortNames.Registered,
  AccessTierShortNames.Controlled,
];

export enum AccessTierDisplayNames {
  Registered = 'Registered Tier',
  Controlled = 'Controlled Tier',
}

export function hasTierAccess(profile: Profile, shortName): boolean {
  return profile.accessTierShortNames.includes(shortName);
}

/**
 * Determine whether the given user has at least Registered Tier access
 * This is required to do most things in the Workbench app
 */
export function hasRegisteredTierAccess(profile: Profile): boolean {
  return hasTierAccess(profile, AccessTierShortNames.Registered);
}

export const displayNameForTier = (shortName: string) =>
  switchCase(
    shortName,
    [AccessTierShortNames.Registered, () => AccessTierDisplayNames.Registered],
    [AccessTierShortNames.Controlled, () => AccessTierDisplayNames.Controlled]
  );
