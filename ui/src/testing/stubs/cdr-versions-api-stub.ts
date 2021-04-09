import {AccessTierShortNames} from 'app/utils/access-tiers';
import {ArchivalStatus, CdrVersion, CdrVersionListResponse, CdrVersionsApi} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export class CdrVersionsStubVariables {
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
  static DEFAULT_WORKSPACE_CDR_VERSION_ID = 'fakeCdrVersion';
  static ALT_WORKSPACE_CDR_VERSION = 'Alternative CDR Version';
  static ALT_WORKSPACE_CDR_VERSION_ID = 'altCdrVersion';
}

export const cdrVersionListResponse: CdrVersionListResponse = {
  defaultCdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
  items: [
    {
      name: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
      archivalStatus: ArchivalStatus.LIVE,
      hasMicroarrayData: true,
      creationTime: 0
    },
    {
      name: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION,
      cdrVersionId: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
      accessTierShortName: AccessTierShortNames.Registered,
      archivalStatus: ArchivalStatus.LIVE,
      hasMicroarrayData: false,
      creationTime: 0
    },
  ]
};

export class CdrVersionsApiStub extends CdrVersionsApi {
  public cdrVersions: CdrVersion[];
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
    this.cdrVersions = cdrVersionListResponse.items;
  }

  getCdrVersions(options?: any): Promise<CdrVersionListResponse> {
    return new Promise<CdrVersionListResponse>(resolve => {
      resolve({
        ...cdrVersionListResponse,
        items: this.cdrVersions,
      });
    });
  }
}
