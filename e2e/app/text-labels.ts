import {config} from 'resources/workbench-config';

export enum PageUrl {
   Home = config.uiBaseUrl,
   Workspaces = config.uiBaseUrl + config.workspacesUrlPath,
   Admin = config.uiBaseUrl + config.adminUrlPath,
}

export enum WorkspaceAccessLevel {
   Owner = 'OWNER',
   Reader = 'READER',
   Writer = 'WRITER',
}

export enum EllipsisMenuAction {
   Duplicate  = 'Duplicate',
   Delete = 'Delete',
   Edit = 'Edit',
   Share = 'Share',
   Review = 'Review',
   RenameDataset = 'Rename Dataset',
   Rename = 'Rename',
}

export enum LinkText {
   Confirm = 'Confirm',
   KeepEditing = 'Keep Editing',
   Cancel = 'Cancel',
   Calculate = 'Calculate',
   AddThis = 'ADD THIS',
   Finish = 'Finish',
   Rename = 'Rename',
   Save = 'Save',
   Update = 'Update',
   Next = 'Next',
   CreateSet = 'CREATE SET',
   DiscardChanges = 'Discard Changes',
   SaveCohort = 'Save Cohort',
   CreateCohort = 'Create Cohort',
   DeleteCohort = 'Delete Cohort',
   RenameDataset = 'Rename Dataset',
   DeleteConceptSet = 'Delete Concept Set',
   DeleteDataset = 'Delete Dataset',
   BackToReviewSet = 'Back to review set',
   BackToCohort = 'Back to cohort',
   DeleteNotebook = 'Delete Notebook',
   CreateAnotherCohort = 'Create another Cohort',
   CreateReviewSets = 'Create Review Sets',
   CreateDataset = 'Create a Dataset',
   CreateAnotherConceptSet = 'Create another Concept Set',
   Submit = 'Submit',
   CreateNewWorkspace = 'Create a New Workspace',
   CreateWorkspace = 'Create Workspace',
   DuplicateWorkspace = 'Duplicate Workspace',
}
