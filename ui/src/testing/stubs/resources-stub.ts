import {WorkspaceData} from 'app/utils/workspace-data';
import {
    BillingStatus,
    Cohort,
    CohortReview,
    ConceptSet,
    DataSet,
    FileDetail,
    ResourceType,
    WorkspaceAccessLevel,
    WorkspaceResource,
} from 'generated/fetch';
import {CdrVersionsStubVariables} from 'testing/stubs/cdr-versions-api-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

type InputResource = FileDetail | Cohort | CohortReview | ConceptSet | DataSet;

export function convertToResource(
  inputResource: InputResource,
  resourceType: ResourceType,
  workspace: WorkspaceData): WorkspaceResource {
  const {namespace, id, accessLevel, cdrVersionId, billingStatus} = workspace;
  return {
    workspaceNamespace: namespace,
    workspaceFirecloudName: id,
    permission: WorkspaceAccessLevel[accessLevel],
    modifiedTime: inputResource.lastModifiedTime ? new Date(inputResource.lastModifiedTime).toString() : new Date().toDateString(),
    cdrVersionId,
    workspaceBillingStatus: billingStatus,
    cohort: resourceType === ResourceType.COHORT ? inputResource as Cohort : null,
    cohortReview: resourceType === ResourceType.COHORTREVIEW ? inputResource as CohortReview : null,
    conceptSet: resourceType === ResourceType.CONCEPTSET ? inputResource as ConceptSet : null,
    dataSet: resourceType === ResourceType.DATASET ? inputResource as DataSet : null,
    notebook: resourceType === ResourceType.NOTEBOOK ? inputResource as FileDetail : null,
  };
}

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
  workspaceBillingStatus: BillingStatus.ACTIVE,
};
