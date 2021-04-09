import * as fp from 'lodash/fp';

export enum AccessTierShortNames {
    Registered = 'registered',
    Controlled = 'controlled',
}

/**
 * Determine whether the given access level is Registered. This is required to do most things in the Workbench app
 * (outside of local/test development).
 *
 * TODO: make separate evaluations by tier
 */
export function hasRegisteredAccess(accessTierShortNames: Array<string>): boolean {
  return fp.includes(AccessTierShortNames.Registered, accessTierShortNames);
}
