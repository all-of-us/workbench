import { WorkspaceAdminApi } from 'generated/fetch';

import { workspaceStubs } from './workspaces';

export class WorkspaceAdminApiStub extends WorkspaceAdminApi {
  constructor(public workspaces = workspaceStubs) {
    super(undefined);
  }
}
