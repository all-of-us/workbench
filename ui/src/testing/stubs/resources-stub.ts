import { WorkspaceResource } from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';

import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

export const stubResource: WorkspaceResource = {
  workspaceNamespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
  workspaceFirecloudName: WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
  workspaceId: 1,
  permission: 'OWNER',
  cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
  accessTierShortName: AccessTierShortNames.Registered,
  lastModifiedEpochMillis: 1634763170,
  adminLocked: false,
};
