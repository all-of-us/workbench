import * as fp from 'lodash/fp';
import {cdrVersionStore, useStore} from "./stores";

export enum AccessTierShortNames {
    Registered = 'registered',
    Controlled = 'controlled',
}

export enum AccessTierDisplayNames {
    Registered = 'Registered Tier',
    Controlled = 'Controlled Tier',
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

export const isTierPresentInEnvironment = (accessTierShortName: string): boolean => {
    // if a user does not have registered tier access, they can't query the cdrVersionStore,
    // so we cannot use this to determine the existence of the registered tier.
    // instead: assume that it exists (which has always been true, as of Sep 2021)
    // TODO: find better solution

    // const {tiers} = useStore(cdrVersionStore);
    //
    // if (tiers) {
    //     return !!tiers.find(tier => tier.accessTierShortName === accessTierShortName);
    // } else {
        return (accessTierShortName === AccessTierShortNames.Registered);
   // }
};
