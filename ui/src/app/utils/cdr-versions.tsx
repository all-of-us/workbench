import {CdrVersion, CdrVersionTier, CdrVersionTiersResponse, Workspace} from 'generated/fetch';
import * as fp from 'lodash/fp';

function getCdrVersionTier(accessTierShortName: string, cdrTiers: CdrVersionTiersResponse): CdrVersionTier {
  return cdrTiers.tiers.find(v => v.accessTierShortName === accessTierShortName);
}

function getDefaultCdrVersionForTier(accessTierShortName: string, cdrTiers: CdrVersionTiersResponse): CdrVersion {
  const tier = getCdrVersionTier(accessTierShortName, cdrTiers);
  if (tier) {
    return tier.versions.find(v => v.cdrVersionId === tier.defaultCdrVersionId);
  }
}

function hasDefaultCdrVersion(workspace: Workspace, cdrTiers: CdrVersionTiersResponse): boolean {
  const tier = getCdrVersionTier(workspace.accessTierShortName, cdrTiers);
  return tier ? workspace.cdrVersionId === tier.defaultCdrVersionId : false;
}

// does not consider tier; IDs are globally unique, enforced by the API DB
function findCdrVersion(cdrVersionId: string, cdrTiers: CdrVersionTiersResponse): CdrVersion {
  console.log(cdrVersionId, cdrTiers);
  const allTiersVersions = fp.flatMap(tier => tier.versions, cdrTiers.tiers);
  return allTiersVersions.find(v => v.cdrVersionId === cdrVersionId);
}

function getCdrVersion(workspace: Workspace, cdrTiers: CdrVersionTiersResponse): CdrVersion {
  return findCdrVersion(workspace.cdrVersionId, cdrTiers);
}

export {
    getCdrVersionTier,
    getDefaultCdrVersionForTier,
    hasDefaultCdrVersion,
    findCdrVersion,
    getCdrVersion,
};
