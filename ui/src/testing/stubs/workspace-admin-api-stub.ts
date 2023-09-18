import { WorkspaceAdminApi } from 'generated/fetch';

import { stubNotImplementedError } from './stub-utils';
import { workspaceStubs } from './workspaces';

export class WorkspaceAdminApiStub extends WorkspaceAdminApi {
  constructor(public workspaces = workspaceStubs) {
    super(undefined);
  }
}
