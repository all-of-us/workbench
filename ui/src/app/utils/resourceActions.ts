import {
  Cohort,
  CohortReview,
  ConceptSet,
  DataSet,
  FileDetail,
  ResourceType,
  WorkspaceAccessLevel,
  WorkspaceResource
} from 'generated/fetch';

export function toDisplay(resourceType: ResourceType): string {
  switch (resourceType) {
    case ResourceType.NOTEBOOK:
      return 'Notebook';
    case ResourceType.COHORT:
      return 'Cohort';
    case ResourceType.COHORTSEARCHGROUP:
      return 'Group';
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

export interface ConvertToResourcesArgs {
  list:  FileDetail[] | Cohort[] | CohortReview[] | ConceptSet[] | DataSet[];
  workspaceNamespace: string;
  workspaceId: string;
  accessLevel: WorkspaceAccessLevel;
  resourceType: ResourceType;
}

export function convertToResources(args: ConvertToResourcesArgs): WorkspaceResource[] {
  const resourceList = [];
  for (const resource of args.list) {
    resourceList.push(convertToResource(resource, args.workspaceNamespace, args.workspaceId,
      args.accessLevel, args.resourceType));
  }
  return resourceList;
}

export function convertToResource(resource: FileDetail | Cohort | CohortReview | ConceptSet
  | DataSet, workspaceNamespace: string, workspaceId: string,
  accessLevel: WorkspaceAccessLevel,
  resourceType: ResourceType): WorkspaceResource {
  let modifiedTime: string;
  if (!resource.lastModifiedTime) {
    modifiedTime = new Date().toDateString();
  } else {
    modifiedTime = new Date(resource.lastModifiedTime).toString();
  }
  const newResource: WorkspaceResource = {
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
