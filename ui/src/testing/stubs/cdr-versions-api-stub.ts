import {AccessTierShortNames} from 'app/utils/access-tiers';
import {
  ArchivalStatus,
  CdrVersionsApi,
  CdrVersionTiersResponse
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

class CdrVersionsStubVariables {
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
  static DEFAULT_WORKSPACE_CDR_VERSION_ID = 'fakeCdrVersion';
  static ALT_WORKSPACE_CDR_VERSION = 'Alternative CDR Version';
  static ALT_WORKSPACE_CDR_VERSION_ID = 'altCdrVersion';
  static CONTROLLED_TIER_CDR_VERSION = 'Controlled Tier CDR Version';
  static CONTROLLED_TIER_CDR_VERSION_ID = 'ctCdrVersion';
}

const cdrVersionTiersResponse: CdrVersionTiersResponse = {
  tiers: [{
    accessTierShortName: AccessTierShortNames.Registered,
    defaultCdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    versions: [
      {
        name: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION,
        cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
        accessTierShortName: AccessTierShortNames.Registered,
        archivalStatus: ArchivalStatus.LIVE,
        hasMicroarrayData: true,
        hasFitbitData: true,
        hasWgsData: true,
        creationTime: 0
      },
      {
        name: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION,
        cdrVersionId: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
        accessTierShortName: AccessTierShortNames.Registered,
        archivalStatus: ArchivalStatus.LIVE,
        hasMicroarrayData: false,
        hasFitbitData: true,
        hasWgsData: false,
        creationTime: 0
      },
    ]},
    {
      accessTierShortName: AccessTierShortNames.Controlled,
      defaultCdrVersionId: CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION_ID,
      versions: [
        {
          name: CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION,
          cdrVersionId: CdrVersionsStubVariables.CONTROLLED_TIER_CDR_VERSION_ID,
          accessTierShortName: AccessTierShortNames.Controlled,
          archivalStatus: ArchivalStatus.LIVE,
          hasMicroarrayData: true,
          hasFitbitData: true,
          hasWgsData: true,
          creationTime: 0
        }
      ]}
  ]
};

const registeredCdrVersionTier = cdrVersionTiersResponse.tiers[0];
const defaultCdrVersion = registeredCdrVersionTier.versions[0];
const altCdrVersion = registeredCdrVersionTier.versions[1];

const controlledCdrVersionTier = cdrVersionTiersResponse.tiers[1];
const controlledCdrVersion = controlledCdrVersionTier.versions[0];

class CdrVersionsApiStub extends CdrVersionsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  getCdrVersionsByTier(options?: any): Promise<CdrVersionTiersResponse> {
    return new Promise<CdrVersionTiersResponse>(resolve => {
      resolve(cdrVersionTiersResponse);
    });
  }
}

export {
  CdrVersionsApiStub,
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
  registeredCdrVersionTier,
  controlledCdrVersionTier,
  defaultCdrVersion,
  altCdrVersion,
  controlledCdrVersion,
};
