import { config } from 'resources/workbench-config';

export enum PageUrl {
  Home = config.uiBaseUrl,
  Workspaces = config.uiBaseUrl + config.workspacesUrlPath,
  Admin = config.uiBaseUrl + config.adminUrlPath,
  Profile = config.uiBaseUrl + config.profileUrlPath
}

export enum WorkspaceAccessLevel {
  Owner = 'OWNER',
  Reader = 'READER',
  Writer = 'WRITER'
}

// Options in dropdown select.
export enum MenuOption {
  Age = 'Age',
  AgeAtCDR = 'Age at CDR',
  AgeAtConsent = 'Age  at Consent',
  Conditions = 'Conditions',
  CopyToAnotherWorkspace = 'Copy to another Workspace',
  CurrentAge = 'Current Age',
  Deceased = 'Deceased',
  Delete = 'Delete',
  DeleteCriteria = 'Delete criteria',
  DeleteGroup = 'Delete group',
  Demographics = 'Demographics',
  Drugs = 'Drugs',
  Duplicate = 'Duplicate',
  Edit = 'Edit',
  EditCriteria = 'Edit criteria',
  EditCriteriaName = 'Edit criteria name',
  EditGroupName = 'Edit group name',
  Ethnicity = 'Ethnicity',
  ExportToNotebook = 'Export to Notebook',
  Fitbit = 'Fitbit',
  GenderIdentity = 'Gender Identity',
  LabsAndMeasurements = 'Labs and Measurements',
  PhysicalMeasurements = 'Physical Measurements',
  Procedures = 'Procedures',
  Race = 'Race',
  Rename = 'Rename',
  RenameDataset = 'Rename Dataset',
  Review = 'Review',
  Save = 'Save',
  SaveAs = 'Save as',
  SexAssignedAtBirth = 'Sex Assigned at Birth',
  SexAtBirth = 'Sex at Birth',
  Share = 'Share',
  SuppressGroupFromTotalCount = 'Suppress group from total count',
  SuppressCriteriaFromTotalCount = 'Suppress criteria from total count',
  Visits = 'Visits',
  WholeGenomeVariant = 'Whole Genome Variant'
}

// Button or link text labels.
export enum LinkText {
  AddSelection = 'Add Selection',
  AddThis = 'ADD THIS',
  AllSurveys = 'All Surveys',
  ApplyModifiers = 'APPLY MODIFIERS',
  ApplyRecreate = 'APPLY & RECREATE',
  Back = 'Back',
  BackToCohort = 'Back to cohort',
  BackToReviewSet = 'Back to review set',
  Calculate = 'Calculate',
  Cancel = 'Cancel',
  Confirm = 'Confirm',
  Continue = 'Continue',
  Copy = 'Copy',
  Close = 'Close',
  Create = 'Create',
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
  Customize = 'Customize',
  Delete = 'Delete',
  DeleteCohort = 'Delete Cohort',
  DeleteConceptSet = 'Delete Concept Set',
  DeleteDataset = 'Delete Dataset',
  DeleteEnvironment = 'Delete Environment',
  DeleteNotebook = 'Delete Notebook',
  DeleteWorkspace = 'Delete Workspace',
  Demographics = 'Demographics',
  DiscardChanges = 'Discard Changes',
  DuplicateWorkspace = 'Duplicate Workspace',
  Edit = 'Edit',
  ExportAndOpen = 'Export and Open',
  Finish = 'Finish',
  FinishAndReview = 'Finish & Review',
  GoToCopiedConceptSet = 'Go to Copied Concept Set',
  GoToCopiedNotebook = 'Go to Copied Notebook',
  KeepEditing = 'Keep Editing',
  Next = 'Next',
  No = 'No',
  Rename = 'Rename',
  RenameCohort = 'Rename Cohort',
  RenameCohortReview = 'Rename Cohort Review',
  RenameConceptSet = 'Rename Concept Set',
  RenameDataset = 'Rename Dataset',
  RenameNotebook = 'Rename Notebook',
  ReviewLater = 'REVIEW LATER',
  ReviewNow = 'REVIEW NOW',
  Save = 'Save',
  SaveCohort = 'Save Cohort',
  SaveConceptSet = 'Save Concept Set',
  SaveCriteria = 'Save Criteria',
  SeeCodePreview = 'See Code Preview',
  StayHere = 'Stay Here',
  Submit = 'Submit',
  Update = 'Update',
  Yes = 'Yes'
}

// Notebook programming language.
export enum Language {
  Python = 'Python',
  R = 'R'
}

// Data resource card.
export enum ResourceCard {
  Cohort = 'Cohort',
  ConceptSet = 'Concept Set',
  Notebook = 'Notebook',
  Dataset = 'Dataset',
  CohortReview = 'Cohort Review'
}

export enum SideBarLink {
  ComputeConfiguration,
  DataDictionary,
  HelpTips,
  WorkspaceMenu,
  EditAnnotations
}
