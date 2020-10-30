import {BillingStatus, UserMetricsApi, WorkspaceResource} from 'generated/fetch';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';
import {CdrVersionsStubVariables} from './cdr-versions-api-stub';

export class UserMetricsApiStub extends UserMetricsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw StubImplementationRequired; });
  }

  getUserRecentResources() {
    const resource: WorkspaceResource = {
      modifiedTime: '2019-01-28 20:13:58.0',
      notebook: {lastModifiedTime: null, name: 'a.ipynb', path: 'gs://q/notebooks/'},
      permission: 'OWNER',
      workspaceFirecloudName: 'q',
      workspaceId: 1,
      workspaceNamespace: 'z',
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      workspaceBillingStatus: BillingStatus.ACTIVE,
    };
    return Promise.resolve([resource]);
  }
}
