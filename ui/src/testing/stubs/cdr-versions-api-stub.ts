import {CdrVersion, CdrVersionListResponse, CdrVersionsApi, DataAccessLevel} from 'generated/fetch';

export class CdrVersionsStubVariables {
  static DEFAULT_WORKSPACE_CDR_VERSION = 'Fake CDR Version';
  static DEFAULT_WORKSPACE_CDR_VERSION_ID = 'fakeCdrVersion';
}

export const cdrVersions = [
  {
    name: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    dataAccessLevel: DataAccessLevel.Registered,
    creationTime: 0
  }
];

export class CdrVersionsApiStub extends CdrVersionsApi {
  public cdrVersions: CdrVersion[];
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.cdrVersions = cdrVersions;
  }

  getCdrVersions(options?: any): Promise<CdrVersionListResponse> {
    return new Promise<CdrVersionListResponse>(resolve => {
      resolve({items: this.cdrVersions,
        defaultCdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID});
    });
  }
}
