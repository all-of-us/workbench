  import {
    FileDetail,
    RecentResource,
    WorkspaceAccessLevel
  } from 'generated';

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

  export function convertToResources(fileList: FileDetail[]): RecentResource[] {
    const resourceList = [];
    for (const file of fileList) {
      resourceList.push(this.convertToResource(file));
    }
    return resourceList;
  }

  export function convertToResource(file: FileDetail): RecentResource {
    let mTime: string;
    if (file.lastModifiedTime === undefined) {
      mTime = new Date().toDateString();
    } else {
      mTime = file.lastModifiedTime.toString();
    }
    const newResource: RecentResource = {
      workspaceNamespace: this.wsNamespace,
      workspaceFirecloudName: this.wsId,
      permission: WorkspaceAccessLevel[this.accessLevel],
      notebook: file,
      modifiedTime: mTime
    };
    return newResource;
  }


