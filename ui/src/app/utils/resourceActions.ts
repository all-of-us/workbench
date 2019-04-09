 import {
    Cohort,
    ConceptSet,
    FileDetail,
    RecentResource,
    WorkspaceAccessLevel
  } from 'generated/fetch';

export enum ResourceType {
    NOTEBOOK = 'notebook',
    COHORT = 'cohort',
    CONCEPT_SET = 'conceptSet',
    INVALID = 'invalid'
  }

export function convertToResources(list: FileDetail[] | Cohort[] | ConceptSet[],
  workspaceNamespace: string, workspaceId: string,
  accessLevel: WorkspaceAccessLevel,
  resourceType: ResourceType): RecentResource[] {
  const resourceList = [];
  for (const resource of list) {
    resourceList.push(convertToResource(resource, workspaceNamespace, workspaceId,
      accessLevel, resourceType));
  }
  return resourceList;
}

export function convertToResource(resource: FileDetail | Cohort | ConceptSet,
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
  } else if (resourceType === ResourceType.CONCEPT_SET) {
    newResource.conceptSet = <ConceptSet>resource;
  }
  return newResource;
}
