  import {
    Cohort,
    FileDetail,
    RecentResource,
    WorkspaceAccessLevel
  } from 'generated';

  export enum ResourceType {
    NOTEBOOK = 'notebook',
    COHORT = 'cohort',
    INVALID = 'invalid'
  }

  export const notebookActionList = [
    {
    type: 'notebook',
    class: 'pencil',
    link: 'renameNotebook',
    text: 'Rename'
  }, {
    type: 'notebook',
    class: 'copy',
    link: 'cloneResource',
    text: 'Clone'
  }, {
    type: 'notebook',
    class: 'trash',
    text: 'Delete',
    link: 'deleteResource'
  }];

export const cohortActionList = [
  {
    type: 'cohort',
    class: 'copy',
    text: 'Clone',
    link: 'cloneResource'
  }, {
    type: 'cohort',
    class: 'pencil',
    text: 'Edit',
    link: 'editCohort'
  },  {
    type: 'cohort',
    class: 'grid-view',
    text: 'Review',
    link: 'reviewCohort'
  }, {
    type: 'cohort',
    class: 'trash',
    text: 'Delete',
    link: 'deleteResource'
  }];

export const resourceActionList =  notebookActionList.concat(cohortActionList);

export function convertToResources(list: FileDetail[] | Cohort[],
                                   workspaceNamespace: string, workspaceId: string,
                                   accessLevel: WorkspaceAccessLevel,
                                   resourceType: ResourceType): RecentResource[] {
  const resourceList = [];
  for (const resource of list) {
    resourceList.push(this.convertToResource(resource, workspaceNamespace, workspaceId,
      accessLevel, resourceType));
  }
  return resourceList;
}

export function convertToResource(resource: FileDetail | Cohort,
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
  }
  return newResource;
}


