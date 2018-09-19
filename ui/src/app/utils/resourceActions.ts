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


