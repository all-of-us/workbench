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
   CopyToAnotherWorkspace = 'Copy to another Workspace',
   Delete = 'Delete',
   Duplicate  = 'Duplicate',
   Edit = 'Edit',
   Rename = 'Rename',
   RenameDataset = 'Rename Dataset',
   Review = 'Review',
   Share = 'Share',
}

export enum LinkText {
   AddThis = 'ADD THIS',
   BackToCohort = 'Back to cohort',
   BackToReviewSet = 'Back to review set',
   Calculate = 'Calculate',
   Cancel = 'Cancel',
   Copy = 'Copy',
   Confirm = 'Confirm',
   CreateAnotherCohort = 'Create another Cohort',
   CreateAnotherConceptSet = 'Create another Concept Set',
   CreateCohort = 'Create Cohort',
   CreateDataset = 'Create a Dataset',
   CreateNewNotebook = 'New Notebook',
   CreateNewWorkspace = 'Create a New Workspace',
   CreateNotebook = 'Create Notebook',
   CreateReviewSets = 'Create Review Sets',
   CreateSet = 'CREATE SET',
   CreateWorkspace = 'Create Workspace',
   DeleteCohort = 'Delete Cohort',
   DeleteConceptSet = 'Delete Concept Set',
   DeleteDataset = 'Delete Dataset',
   DeleteNotebook = 'Delete Notebook',
   Deleteworkspace = 'Delete Workspace',
   DiscardChanges = 'Discard Changes',
   DuplicateWorkspace = 'Duplicate Workspace',
   Finish = 'Finish',
   GoToCopiedConceptSet = 'Go to Copied Concept Set',
   KeepEditing = 'Keep Editing',
   Next = 'Next',
   Rename = 'Rename',
   RenameDataset = 'Rename Dataset',
   RenameNotebook = 'Rename Notebook',
   Save = 'Save',
   SaveCohort = 'Save Cohort',
   StayHere = 'Stay Here',
   Submit = 'Submit',
   Update = 'Update',
}

export enum Language {
   Python = 'Python',
   R = 'R',
}
