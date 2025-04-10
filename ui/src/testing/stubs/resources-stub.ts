import {
  Cohort,
  CohortReview,
  ConceptSet,
  DataSet,
  FileDetail,
  ResourceType,
  WorkspaceResource,
} from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import { convertToResource } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

type InputResource = FileDetail | Cohort | CohortReview | ConceptSet | DataSet;
export function convertToResources(
  inputResources: InputResource[],
  resourceType: ResourceType,
  workspace: WorkspaceData
): WorkspaceResource[] {
  return inputResources.map((ir) =>
    convertToResource(ir, resourceType, workspace)
  );
}

export const stubResource: WorkspaceResource = {
  workspaceNamespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
  workspaceFirecloudName: WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
  workspaceId: 1,
  permission: 'OWNER',
  cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
  accessTierShortName: AccessTierShortNames.Registered,
  lastModifiedEpochMillis: 1634763170,
  adminLocked: false,

  // deprecated and will be removed soon
  workspaceBillingStatus: undefined,
};
