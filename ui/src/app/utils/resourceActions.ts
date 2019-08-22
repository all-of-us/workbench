import {
  Cohort,
  CohortReview,
  ConceptSet,
  DataSet,
  FileDetail,
  RecentResource,
  WorkspaceAccessLevel
} from 'generated/fetch';

export enum ResourceType {
    NOTEBOOK = 'notebook',
    COHORT = 'cohort',
    COHORT_REVIEW = 'cohortReview',
    CONCEPT_SET = 'conceptSet',
    DATA_SET = 'dataSet',
    INVALID = 'invalid'
  }

export function convertToResource(resource: FileDetail | Cohort | CohortReview | ConceptSet,
  workspaceNamespace: string, workspaceId: string,
  accessLevel: WorkspaceAccessLevel,
  resourceType: ResourceType): RecentResource {
  let modifiedTime: string;
  if (!resource.lastModifiedTime) {
    modifiedTime = new Date().toDateString();
  } else {
    modifiedTime = resource.lastModifiedTime.toString();
  }
  const newResource: RecentResource = {
    workspaceNamespace: workspaceNamespace,
    workspaceFirecloudName: workspaceId,
    permission: WorkspaceAccessLevel[accessLevel],
    modifiedTime: modifiedTime
  };
  if (resourceType === ResourceType.NOTEBOOK) {
    newResource.notebook = <FileDetail>resource;
  } else if (resourceType === ResourceType.COHORT) {
    newResource.cohort = <Cohort>resource;
  } else if (resourceType === ResourceType.COHORT_REVIEW) {
    newResource.cohortReview = <CohortReview>resource;
  } else if (resourceType === ResourceType.CONCEPT_SET) {
    newResource.conceptSet = <ConceptSet>resource;
  } else if (resourceType === ResourceType.DATA_SET) {
    newResource.dataSet = <DataSet>resource;
  }
  return newResource;
}