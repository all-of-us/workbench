import {
  ArchivalStatus,
  CdrVersionsApi,
  CdrVersionTiersResponse,
} from 'generated/fetch';

import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class CdrVersionsStubVariables {
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
  static DEFAULT_WORKSPACE_CDR_VERSION_ID = '-1';
  static ALT_WORKSPACE_CDR_VERSION = 'Alternative CDR Version';
  static ALT_WORKSPACE_CDR_VERSION_ID = '-2';
  static CONTROLLED_TIER_CDR_VERSION = 'Controlled Tier CDR Version';
  static CONTROLLED_TIER_CDR_VERSION_ID = '-3';
}

export const cdrVersionTiersResponse: CdrVersionTiersResponse = {
  tiers: [
    {
      accessTierShortName: AccessTierShortNames.Registered,
      accessTierDisplayName: AccessTierDisplayNames.Registered,
      defaultCdrVersionId:
        CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      defaultCdrVersionCreationTime: 0,
      versions: [
        {
          name: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION,
          cdrVersionId:
            CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
          accessTierShortName: AccessTierShortNames.Registered,
          archivalStatus: ArchivalStatus.LIVE,
          hasFitbitData: true,
          hasFitbitSleepData: false,
          hasWgsData: true,
          creationTime: 0,
        },
        {
          name: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION,
          cdrVersionId: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
          accessTierShortName: AccessTierShortNames.Registered,
          archivalStatus: ArchivalStatus.LIVE,
          hasFitbitData: true,
          hasFitbitSleepData: false,
          hasWgsData: false,
          creationTime: 0,
        },
      ],
    },
    {
      accessTierShortName: AccessTierShortNames.Controlled,
      accessTierDisplayName: AccessTierDisplayNames.Controlled,
      defaultCdrVersionId:
        CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION_ID,
      defaultCdrVersionCreationTime: 0,
      versions: [
        {
          name: CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION,
          cdrVersionId: CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION_ID,
          accessTierShortName: AccessTierShortNames.Controlled,
          archivalStatus: ArchivalStatus.LIVE,
          hasFitbitData: true,
          hasFitbitSleepData: false,
          hasWgsData: true,
          creationTime: 0,
        },
      ],
    },
  ],
};

export const registeredCdrVersionTier = cdrVersionTiersResponse.tiers[0];
export const defaultCdrVersion = registeredCdrVersionTier.versions[0];
export const altCdrVersion = registeredCdrVersionTier.versions[1];

export const controlledCdrVersionTier = cdrVersionTiersResponse.tiers[1];
export const controlledCdrVersion = controlledCdrVersionTier.versions[0];

export class CdrVersionsApiStub extends CdrVersionsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  getCdrVersionsByTier(): Promise<CdrVersionTiersResponse> {
    return new Promise<CdrVersionTiersResponse>((resolve) => {
      resolve(cdrVersionTiersResponse);
    });
  }
}
