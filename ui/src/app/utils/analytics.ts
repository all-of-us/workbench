import {environment} from 'environments/environment';
import {VerifiedInstitutionalAffiliation} from 'generated/fetch';

declare let gtag: Function;

/**
 * Triggers a Google Analytics event.
 *
 * For guidance on how to use action names, categories, and labels effectively,
 * see ttps://support.google.com/analytics/answer/1033068#Anatomy.
 *
 * @param category The page, feature, or section where this event is happening.
 *   Examples: "Workspaces", "Cohort Builder", "Homepage"
 * @param action The type of action taken by the user. Examples: "click",
 *   "delete", "open"
 * @param label An optional label further describing the event. Examples:
 *   "Workspace - workspace name click"
 * @param value An optional integer value to associate with the event.
 */
export function triggerEvent(
  category: string, action: string, label?: string, value?: number) {
  if (window['gtag']) {
    gtag('event', action, {
      'event_category': category,
      'event_label': label,
      'event_value': value,
    });
  } else {
    console.error('Google Analytics gtag.js has not been loaded');
  }
}

enum AnalyticsCategory {
  WORKSPACES = 'Workspaces',
  FEATURED_WORKSPACES = 'Featured Workspaces',
  DATASET_BUILDER = 'Dataset Builder',
  NOTEBOOKS = 'Notebooks',
  SIDEBAR = 'Sidebar Menu',
  FOOTER = 'Footer',
  HELP = 'Help',
  WORKSPACE_UPDATE_PROMPT = 'Workspace update prompt'
}

export const AnalyticsTracker = {
  Workspaces: {
    OpenCreatePage: () => triggerEvent(AnalyticsCategory.WORKSPACES, 'Open Create Page', getCurrentPageLabel()),
    Create: () => triggerEvent(AnalyticsCategory.WORKSPACES, 'Create'),
    OpenDuplicatePage: (suffix = '') => triggerEvent(AnalyticsCategory.WORKSPACES, 'Open Duplicate Page', getCurrentPageLabel(suffix)),
    Duplicate: () => triggerEvent(AnalyticsCategory.WORKSPACES, 'Duplicate'),
    DuplicateFeatured: (name) =>
      triggerEvent(AnalyticsCategory.FEATURED_WORKSPACES, 'Click', `Featured Workspace - Tile - Duplicate - ${name}`),
    NavigateToFeatured: (name) =>
      triggerEvent(AnalyticsCategory.FEATURED_WORKSPACES, 'Click', `Featured Workspace - Tile - ${name}`),
    OpenEditPage: (suffix = '') => triggerEvent(AnalyticsCategory.WORKSPACES, 'Open Edit Page', getCurrentPageLabel(suffix)),
    Edit: () => triggerEvent(AnalyticsCategory.WORKSPACES, 'Edit'),
    OpenShareModal: (suffix = '') => triggerEvent(AnalyticsCategory.WORKSPACES, 'Open Share Modal', getCurrentPageLabel(suffix)),
    Share: () => triggerEvent(AnalyticsCategory.WORKSPACES, 'Share', getCurrentPageLabel()),
    OpenDeleteModal: (suffix = '') => triggerEvent(AnalyticsCategory.WORKSPACES, 'Open Delete Modal', getCurrentPageLabel(suffix)),
    Delete: () => triggerEvent(AnalyticsCategory.WORKSPACES, 'Delete', getCurrentPageLabel()),
  },
  WorkspaceUpdatePrompt: {
    LooksGood: () => triggerEvent(AnalyticsCategory.WORKSPACE_UPDATE_PROMPT, 'Click \'Looks  Good\'', 'No Edits'),
    UpdateWorkspace: () => triggerEvent(AnalyticsCategory.WORKSPACE_UPDATE_PROMPT, 'Click \'Update\'', 'Edit Workspace')
  },
  DatasetBuilder: {
    OpenCreatePage: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Open Create Page'),
    Save: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Save'),
    SaveAndAnalyze: (suffix) => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Save and Analyze', suffix),
    OpenEditPage: (suffix) => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Open Edit Page', suffix),
    Update: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Update'),
    UpdateAndAnalyze: (suffix) => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Update and Analyze', suffix),
    SeeCodePreview: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'See Code Preview'),
    OpenRenameModal: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Open Rename Modal'),
    Rename: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Rename'),
    OpenExportModal: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Open Export Modal'),
    Export: (suffix) => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Export', suffix),
    OpenDeleteModal: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Open Delete Modal'),
    OpenGenomicExtractionModal: (suffix) => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Open Genomic Extraction Modal', suffix),
    Delete: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'Delete'),
    ViewPreviewTable: () => triggerEvent(AnalyticsCategory.DATASET_BUILDER, 'View Preview Table')
  },
  Notebooks: {
    OpenCreateModal: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Open Create Modal'),
    Create: (suffix) => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Create', suffix),
    OpenRenameModal: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Open Rename Modal'),
    Rename: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Rename'),
    Duplicate: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Duplicate'),
    OpenCopyModal: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Open Copy Modal'),
    Copy: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Copy'),
    OpenDeleteModal: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Open Delete Modal'),
    Delete: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Delete'),
    Preview: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Preview'),
    Edit: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Edit'),
    Run: () => triggerEvent(AnalyticsCategory.NOTEBOOKS, 'Run (Playground Mode)')
  },
  Registration: {
    SignIn: () => triggerEvent('Registration', 'Click sign-in', 'Sign in'),
    CreateAccount: () => triggerEvent('Registration', 'Click create account', 'Create account'),
    TOS: () => triggerEvent('Registration', 'Click next', 'Accepted TOS'),
    InstitutionNotListed: () => triggerEvent('Registration', 'Click \'don\'t see institution\' link', 'Institution not listed'),
    InstitutionPage: () =>
      triggerEvent('Registration', 'Clicked \'Next\' in step 1/3 in registration process', 'Institution info completed'),
    CreateAccountPage: () =>
      triggerEvent('Registration', 'Clicked \'Next\' in step 2/3 in registration process', 'Create Account page completed'),
    DemographicSurvey: () =>
      triggerEvent('Registration', 'Clicked \'Next\' in step 3/3 in registration process', 'Demographic survey completed'),
    TwoFactorAuth: () => triggerEvent('Registration', 'Clicked on \'2FA\' button', '2FA'),
    EthicsTraining: () => triggerEvent('Registration', 'Clicked on \'Ethics training\' button', 'Training'),
    ERACommons: () => triggerEvent('Registration', 'Clicked on eRA commons button', 'eRA commons'),
    RasLoginGov: () => triggerEvent('Registration', 'Clicked on RAS LoginGov linking button', 'RAS LoginGov'),
    EnterDUCC: () => triggerEvent('Registration', 'Clicked in DUCC button', 'Entered DUCC'),
    AcceptDUCC: () => triggerEvent('Registration', 'Clicked in DUCC button', 'Accepted DUCC'),
    TutorialVideo: () => triggerEvent('Home Page', 'Clicked on a tutorial video', 'Tutorial videos'),
  },
  Sidebar: {
    OpenSidebar: (suffix) => triggerEvent(AnalyticsCategory.SIDEBAR, 'Click', suffix),
    Search: (suffix) => triggerEvent(AnalyticsCategory.HELP, 'Search', `Help - Search - ${suffix}`),
    ContactUs: (suffix) => triggerEvent(AnalyticsCategory.HELP, 'Click', `Help - Contact Us - ${suffix}`),
    UserSupport: (suffix) => triggerEvent(AnalyticsCategory.HELP, 'Click', `Help - User Support - ${suffix}`)
  },
  Footer: {
    Home: () => triggerEvent(AnalyticsCategory.FOOTER, 'Home', 'Home'),
    DataBrowser: () => triggerEvent(AnalyticsCategory.FOOTER, 'Data Browser', 'Data Browser'),
    FeaturedWorkspaces: () => triggerEvent(AnalyticsCategory.FOOTER, 'Featured workspaces', 'Featured workspaces'),
    ResearchHub: () => triggerEvent(AnalyticsCategory.FOOTER, 'Research Hub', 'Research Hub'),
    YourWorkspaces: () => triggerEvent(AnalyticsCategory.FOOTER, 'Your workspaces', 'Your workspaces'),
    GettingStarted: () => triggerEvent(AnalyticsCategory.FOOTER, 'Getting started', 'Support - Getting started'),
    SupportDocs: () => triggerEvent(AnalyticsCategory.FOOTER, 'Documentation', 'Support - Documentation'),
    CommunityForum: () => triggerEvent(AnalyticsCategory.FOOTER, 'Community forum', 'Support - Community forum'),
    SupportFAQ: () => triggerEvent(AnalyticsCategory.FOOTER, 'FAQ', 'Support - FAQ'),
    ContactUs: (suffix) => triggerEvent(AnalyticsCategory.FOOTER, 'Contact Us', `Support - Contact Us - ${suffix}`),
    HHS: () => triggerEvent(AnalyticsCategory.FOOTER, 'US Dept of Health & Human Svcs', 'hhs.gov')
  }
};

function getCurrentPageLabel(suffix = '') {
  let prefix;

  if (window.location.pathname === '/') {
    prefix = 'From Home Page';
  } else if (window.location.pathname === '/workspaces') {
    prefix = 'From Workspace List Page';
  } else if (window.location.pathname.match(/\/workspaces\/.*/) !== null) {
    prefix = 'From Workspace Page';
  } else {
    prefix = 'Unknown Label: ' + window.location.pathname;
  }

  return suffix ? `${prefix} (${suffix})` : prefix;
}

enum UserAuthState {
  LOGGED_IN = 'Logged in',
  LOGGED_OUT = 'Logged out'
}

/**
 * Sets the Analytics custom dimension indicating whether the current user
 * session should be treated as logged-in or logged-out. This allows us to
 * create Analytics segments which only show logged-in user activity.
 * @param signedIn
 */
export function setLoggedInState(loggedIn: boolean) {
  gtagSet({
    [environment.gaLoggedInDimension]: loggedIn ? UserAuthState.LOGGED_IN : UserAuthState.LOGGED_OUT
  });
}

enum InstitutionCategoryState {
  RESEARCHER = 'Researcher',
  OPERATIONS = 'Operations',
  NO_INSTITUTION = 'No Institution',
  UNKNOWN = 'Unknown',
}

// matches backend InstitutionServiceImpl
// TODO: avoid reliance on the name of the ops institution
const OPERATIONAL_USER_INSTITUTION_SHORT_NAME = 'AouOps';

/**
 * Sets the Analytics custom dimension indicating what category of institution the user belongs to:
 * RESEARCHER, OPERATIONS, or NONE. This allows us to create Analytics segments split by category.
 * @param affiliation the verified institutional affiliation
 */
export function setInstitutionCategoryState(affiliation: VerifiedInstitutionalAffiliation) {
  if (affiliation) {
    const category = (OPERATIONAL_USER_INSTITUTION_SHORT_NAME === affiliation.institutionShortName) ?
        InstitutionCategoryState.OPERATIONS : InstitutionCategoryState.RESEARCHER;
    gtagSet({
      [environment.gaUserInstitutionCategoryDimension]: category
    });
  } else {
    gtagSet({
      [environment.gaUserInstitutionCategoryDimension]: InstitutionCategoryState.NO_INSTITUTION
    });
  }
}

/**
 * Initializes the Google Analytics config using gtag.js. This method looks to
 * environment variables for various GA configuration parameters, and sets
 * initial values for contextual custom dimensions.
 */
export function initializeAnalytics() {
  gtag('js', new Date());
  gtagSet({
    [environment.gaUserAgentDimension]: window.navigator.userAgent.slice(0, 100),
    [environment.gaLoggedInDimension]: UserAuthState.LOGGED_OUT,
    [environment.gaUserInstitutionCategoryDimension]: InstitutionCategoryState.UNKNOWN,
  });
  gtag('config', environment.gaId);
}

// invokes the 'set' command in gtag.js which mutates global state.
// All gtag event triggers after this call will be affected.
// see https://developers.google.com/gtagjs/reference/api
function gtagSet(setParam: object) {
  gtag('set', setParam);
}
