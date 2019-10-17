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
  DATA_SET = 'dataset',
  INVALID = 'invalid',
  WORKSPACE = 'workspace'
}

const ResourceTypeDisplayNames = new Map()
  .set(ResourceType.NOTEBOOK, 'Notebook')
  .set(ResourceType.COHORT, 'Cohort')
  .set(ResourceType.COHORT_REVIEW, 'Cohort Review')
  .set(ResourceType.CONCEPT_SET, 'Concept Set')
  .set(ResourceType.DATA_SET, 'Dataset')
  .set(ResourceType.INVALID, 'Invalid')
  .set(ResourceType.WORKSPACE, 'Workspace');

export function getResourceTypeDisplayName(resourceType: ResourceType) {
  return ResourceTypeDisplayNames.get(resourceType);
}

export function convertToResources(list: FileDetail[] | Cohort[] | CohortReview[] | ConceptSet[]
  | DataSet[], workspaceNamespace: string, workspaceId: string,
  accessLevel: WorkspaceAccessLevel,
  resourceType: ResourceType): RecentResource[] {
  const resourceList = [];
  for (const resource of list) {
    resourceList.push(convertToResource(resource, workspaceNamespace, workspaceId,
      accessLevel, resourceType));
  }
  return resourceList;
}

export function convertToResource(resource: FileDetail | Cohort | CohortReview | ConceptSet
  | DataSet, workspaceNamespace: string, workspaceId: string,
  accessLevel: WorkspaceAccessLevel,
  resourceType: ResourceType): RecentResource {
  let modifiedTime: string;
  if (!resource.lastModifiedTime) {
    modifiedTime = new Date().toDateString();
  } else {
    modifiedTime = new Date(resource.lastModifiedTime).toString();
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
