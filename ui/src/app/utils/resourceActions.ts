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
import {WorkspaceData} from './workspace-data';

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
  resourceType: ResourceType;
  workspace: WorkspaceData;
}

export function convertToResources(args: ConvertToResourcesArgs): WorkspaceResource[] {
  const resourceList = [];
  for (const resource of args.list) {
    resourceList.push(convertToResource(resource, args.resourceType, args.workspace));
  }
  return resourceList;
}

export function convertToResource(
  resource: FileDetail | Cohort | CohortReview | ConceptSet | DataSet,
  resourceType: ResourceType,
  workspace: WorkspaceData): WorkspaceResource {
  const {namespace, id, accessLevel, cdrVersionId, billingStatus} = workspace;
  let modifiedTime: string;
  if (!resource.lastModifiedTime) {
    modifiedTime = new Date().toDateString();
  } else {
    modifiedTime = new Date(resource.lastModifiedTime).toString();
  }
  const newResource: WorkspaceResource = {
    workspaceNamespace: namespace,
    workspaceFirecloudName: id,
    permission: WorkspaceAccessLevel[accessLevel],
    modifiedTime,
    cdrVersionId,
    workspaceBillingStatus: billingStatus,
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
