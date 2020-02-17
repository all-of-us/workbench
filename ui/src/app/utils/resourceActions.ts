import {
  Cohort,
  CohortReview,
  ConceptSet,
  DataSet,
  FileDetail,
  RecentResource,
  ResourceType,
  WorkspaceAccessLevel
} from 'generated/fetch';

export function toDisplay(resourceType: ResourceType): string {
  switch (resourceType) {
    case ResourceType.NOTEBOOK:
      return 'Notebook';
    case ResourceType.COHORT:
      return 'Cohort';
    case ResourceType.COHORTSEARCHITEM:
      return 'Item';
    case ResourceType.COHORTREVIEW:
      return 'Cohort Review';
    case ResourceType.CONCEPTSET:
      return 'Concept Set';
    case ResourceType.DATASET:
      return 'Dataset';
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
  } else if (resourceType === ResourceType.COHORTREVIEW) {
    newResource.cohortReview = <CohortReview>resource;
  } else if (resourceType === ResourceType.CONCEPTSET) {
    newResource.conceptSet = <ConceptSet>resource;
  } else if (resourceType === ResourceType.DATASET) {
    newResource.dataSet = <DataSet>resource;
  }
  return newResource;
}
