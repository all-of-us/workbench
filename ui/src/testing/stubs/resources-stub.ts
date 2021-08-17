import {convertToResource} from 'app/utils/resources';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
    BillingStatus,
    Cohort,
    CohortReview,
    ConceptSet,
    DataSet,
    FileDetail,
    ResourceType,
    WorkspaceResource,
} from 'generated/fetch';
import {CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {AccessTierShortNames} from 'app/utils/access-tiers';

type InputResource = FileDetail | Cohort | CohortReview | ConceptSet | DataSet;
export function convertToResources(
  inputResources: InputResource[],
  resourceType: ResourceType,
  workspace: WorkspaceData): WorkspaceResource[] {
  return inputResources.map(ir => convertToResource(ir, resourceType, workspace));
}

export const stubResource: WorkspaceResource = {
  workspaceNamespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
  workspaceFirecloudName: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
  workspaceId: 1,
  modifiedTime: '2019-01-28 20:13:58.0',
  permission: 'OWNER',
  cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
  accessTierShortName: AccessTierShortNames.Registered,
  workspaceBillingStatus: BillingStatus.ACTIVE,
};
