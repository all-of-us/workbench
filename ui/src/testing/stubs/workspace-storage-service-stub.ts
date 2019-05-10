import {workspaceStubs} from 'testing/stubs/workspaces-api-stub';

import {WorkspaceAccessLevel} from 'generated/fetch';

export const workspaceDataStub = {
  ...workspaceStubs[0],
  accessLevel: WorkspaceAccessLevel.OWNER,
};
