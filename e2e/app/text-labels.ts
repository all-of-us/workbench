import { config } from 'resources/workbench-config';
import { IPageUrl } from 'types';

const { LOGIN_URL_DOMAIN_NAME, WORKSPACES_URL_PATH, ADMIN_URL_PATH, PROFILE_URL_PATH } = config;

export const PageUrl: IPageUrl = {
  Home: LOGIN_URL_DOMAIN_NAME,
  Workspaces: LOGIN_URL_DOMAIN_NAME + WORKSPACES_URL_PATH,
  Admin: LOGIN_URL_DOMAIN_NAME + ADMIN_URL_PATH,
  Profile: LOGIN_URL_DOMAIN_NAME + PROFILE_URL_PATH
};

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
  LongReadWGS = 'Long Read WGS',
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
  ShortReadWGS = 'Short Read WGS',
  SuppressGroupFromTotalCount = 'Suppress group from total count',
  SuppressCriteriaFromTotalCount = 'Suppress criteria from total count',
  Surveys = 'Surveys',
  Visits = 'Visits'
}

// Button or link text labels.
export enum LinkText {
  Add = 'Add',
  AddSelection = 'Add Selection',
  AddThis = 'ADD THIS',
  ApplyModifiers = 'APPLY MODIFIERS',
  ApplyRecreate = 'APPLY & RECREATE',
  Audit = 'AUDIT',
  Back = 'Back',
  BackToCohort = 'Back to cohort',
  BackToReviewSet = 'Back to review sets',
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
  DeletePd = 'Delete Persistent Disk',
  DeleteNotebook = 'Delete Notebook',
  DeleteWorkspace = 'Delete Workspace',
  DiscardChanges = 'Discard Changes',
  DownloadSql = 'DOWNLOAD SQL',
  DuplicateWorkspace = 'Duplicate Workspace',
  Edit = 'Edit',
  Export = 'Export',
  ExtractAndContinue = 'Extract & Continue',
  FinishAndReview = 'Finish & Review',
  GetStarted = 'Get Started',
  GoToCopiedConceptSet = 'Go to Copied Concept Set',
  GoToCopiedNotebook = 'Go to Copied Notebook',
  KeepEditing = 'Keep Editing',
  LoadWorkspace = 'Load Workspace',
  LockWorkspace = 'LOCK WORKSPACE',
  Next = 'Next',
  No = 'No',
  OK = 'OK',
  Preview = 'Preview',
  Rename = 'Rename',
  RenameCohort = 'Rename Cohort',
  RenameCohortReview = 'Rename Cohort Review',
  RenameConceptSet = 'Rename Concept Set',
  RenameDataset = 'Rename Dataset',
  RenameNotebook = 'Rename Notebook',
  Review = 'Review',
  ReviewLater = 'REVIEW LATER',
  ReviewNow = 'REVIEW NOW',
  Save = 'Save',
  SaveDataset = 'Save Dataset',
  SaveCohort = 'Save Cohort',
  SaveConceptSet = 'Save Concept Set',
  SaveCriteria = 'Save Criteria',
  SeeCodePreview = 'See Code Preview',
  StayHere = 'Stay Here',
  Submit = 'Submit',
  Yes = 'Yes',
  YesDelete = 'YES, DELETE',
  YesLeave = 'Yes, Leave'
}

// Notebook programming language.
export enum Language {
  Python = 'Python',
  R = 'R'
}

export enum AnalysisTool {
  Hail = 'Hail',
  Plink = 'PLINK',
  VCFCompatible = 'Other VCF-compatible tool'
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
  RuntimeConfiguration,
  CromwellConfiguration,
  DataDictionary,
  HelpTips,
  WorkspaceMenu,
  EditAnnotations,
  GenomicExtractionsHistory,
  Applications
}

export const Institution = {
  Vanderbilt: 'Vanderbilt University Medical Center',
  Broad: 'Broad Institute',
  Verily: 'Verily LLC',
  NationalInstituteHealth: 'National Institute of Health',
  Wondros: 'Wondros',
  AdminTesting: 'Admin Testing',
  DummyMaster: 'Dummy Master',
  Google: 'Google'
};

export const InstitutionRole = {
  EarlyCareerTenureTrackResearcher: 'Early career tenure-track researcher',
  UndergraduateStudent: 'Undergraduate (Bachelor level) student',
  Industry: 'Industry',
  ResearchAssistant: 'Research Assistant (pre-doctoral)',
  ResearchAssociate: 'Research associate (post-doctoral; early/mid career)',
  SeniorResearcher: 'Senior Researcher (PI/Team Lead, senior scientist)',
  Teacher: 'Teacher/Instructor/Professor'
};

export enum HeaderName {
  InstitutionName = 'Institution Name',
  InstitutionType = 'Institution Type',
  DataAccessTiers = 'Data access tiers',
  UserEmailInstruction = 'User Email Instruction'
}

export const Cohorts = {
  AllParticipants: 'All Participants'
};

export const ConceptSets = {
  AllSurveys: 'All Surveys',
  Demographics: 'Demographics',
  FitbitActivitySummary: 'Fitbit Activity Summary',
  FitbitHeartRateLevel: 'Fitbit Heart Rate Level',
  FitbitHeartRateSummary: 'Fitbit Heart Rate Summary',
  FitbitIntraDaySteps: 'Fitbit Intra Day Steps',
  WholeGenomeSequenceVariantData: 'Short Read Whole Genome Sequencing Data'
};

export const DataSets = {
  VCFFile: 'VCF Files',
  PersonID: 'person_id',
  GenderConceptId: 'gender_concept_id'
};

export enum AgeSelectionRadioButton {
  AgeAtCdrDate = 'Age at CDR Date',
  CurrentAge = 'Current Age',
  AgeAtConsent = 'Age at Consent'
}

export enum CloudStorageHeader {
  Location = 'Location',
  Filename = 'Filename',
  FileSize = 'File size (MB)'
}

export enum Tabs {
  Data = 'Data',
  Analysis = 'Analysis',
  About = 'About',
  Cohorts = 'Cohorts',
  Datasets = 'Datasets',
  CohortReviews = 'Cohort Reviews',
  ConceptSets = 'Concept Sets',
  ShowAll = 'Show All'
}

export enum AccessTierDisplayNames {
  Registered = 'Registered Tier',
  Controlled = 'Controlled Tier'
}
