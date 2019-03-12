import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {WorkspaceAccessLevel} from 'generated';

export const workspaceDataStub = {
  ...WorkspacesServiceStub.stubWorkspace(),
  accessLevel: WorkspaceAccessLevel.OWNER,
};

export class WorkspaceStorageServiceStub {
  getWorkspace() {
    return Promise.resolve(workspaceDataStub);
  }
}
