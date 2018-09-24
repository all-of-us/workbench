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

export function convertToResources(list: any[], component): RecentResource[] {
  const resourceList = [];
  for (const resource of list) {
    resourceList.push(this.convertToResource(resource, component));
  }
  return resourceList;
}

export function convertToResource(resource: any, component): RecentResource {
  let modifiedTime: string;
  if (resource.lastModifiedTime === undefined) {
    modifiedTime = new Date().toDateString();
  } else {
    modifiedTime = resource.lastModifiedTime.toString();
  }
  const newResource: RecentResource = {
    workspaceNamespace: component.wsNamespace,
    workspaceFirecloudName: component.wsId,
    permission: WorkspaceAccessLevel[component.accessLevel],
    modifiedTime: modifiedTime
  };
  if (component.resourceType === ResourceType.NOTEBOOK) {
    newResource.notebook = resource;
  } else if (component.resourceType === ResourceType.COHORT) {
    newResource.cohort = resource;
  }
  return newResource;
}


