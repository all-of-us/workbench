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
  NOTEBOOK = 'Notebook',
  COHORT = 'Cohort',
  COHORT_REVIEW = 'Cohort Review',
  CONCEPT_SET = 'Concept Set',
  DATA_SET = 'Dataset',
  INVALID = 'Invalid',
  WORKSPACE = 'Workspace'
}

export function toDisplay(resourceType: ResourceType): string {
  switch (resourceType) {
    case ResourceType.NOTEBOOK:
      return 'Notebook';
    case ResourceType.COHORT:
      return 'Cohort';
    case ResourceType.COHORT_REVIEW:
      return 'Cohort Review';
    case ResourceType.CONCEPT_SET:
      return 'Concept Set';
    case ResourceType.DATA_SET:
      return 'Dataset';
    case ResourceType.INVALID:
      return 'Invalid';
    case ResourceType.WORKSPACE:
      return 'Workspace';
  }
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
