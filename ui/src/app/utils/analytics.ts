import {environment} from 'environments/environment';

declare let gtag: Function;

/**
 * Triggers a Google Analytics event.
 *
 * For guidance on how to use action names, categories, and labels effectively,
 * see https://support.google.com/analytics/answer/1033068#Anatomy.
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

enum ANALYTICS_CATEGORIES {
  WORKSPACES = 'Workspaces',
  DATASET_BUILDER = 'Dataset Builder',
  NOTEBOOKS = 'Notebooks',
  SIDEBAR = 'Sidebar Menu',
  HELP = 'Help'
}

export const AnalyticsTracker = {
  Workspaces: {
    OpenCreatePage: () => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Open Create Page', getCurrentPageLabel()),
    Create: () => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Create'),
    OpenDuplicatePage: (suffix = '') => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Open Duplicate Page', getCurrentPageLabel(suffix)),
    Duplicate: () => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Duplicate'),
    OpenEditPage: (suffix = '') => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Open Edit Page', getCurrentPageLabel(suffix)),
    Edit: () => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Edit'),
    OpenShareModal: (suffix = '') => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Open Share Modal', getCurrentPageLabel(suffix)),
    Share: () => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Share', getCurrentPageLabel()),
    OpenDeleteModal: (suffix = '') => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Open Delete Modal', getCurrentPageLabel(suffix)),
    Delete: () => triggerEvent(ANALYTICS_CATEGORIES.WORKSPACES, 'Delete', getCurrentPageLabel())
  },
  DatasetBuilder: {
    OpenCreatePage: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Open Create Page'),
    Save: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Save'),
    SaveAndAnalyze: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Save and Analyze', suffix),
    OpenEditPage: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Open Edit Page', suffix),
    Update: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Update'),
    UpdateAndAnalyze: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Update and Analyze', suffix),
    SeeCodePreview: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'See Code Preview'),
    OpenRenameModal: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Open Rename Modal'),
    Rename: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Rename'),
    OpenExportModal: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Open Export Modal'),
    Export: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Export', suffix),
    OpenDeleteModal: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Open Delete Modal'),
    Delete: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'Delete'),
    ViewPreviewTable: () => triggerEvent(ANALYTICS_CATEGORIES.DATASET_BUILDER, 'View Preview Table')
  },
  Notebooks: {
    OpenCreateModal: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Open Create Modal'),
    Create: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Create', suffix),
    OpenRenameModal: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Open Rename Modal'),
    Rename: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Rename'),
    Duplicate: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Duplicate'),
    OpenCopyModal: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Open Copy Modal'),
    Copy: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Copy'),
    OpenDeleteModal: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Open Delete Modal'),
    Delete: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Delete'),
    Preview: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Preview'),
    Edit: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Edit'),
    Run: () => triggerEvent(ANALYTICS_CATEGORIES.NOTEBOOKS, 'Run (Playground Mode)')
  },
  Sidebar: {
    OpenSidebar: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.SIDEBAR, 'Click', suffix),
    Search: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.HELP, 'Search', `Help - Search - ${suffix}`),
    ContactUs: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.HELP, 'Click', `Help - Contact Us - ${suffix}`),
    UserSupport: (suffix) => triggerEvent(ANALYTICS_CATEGORIES.HELP, 'Click', `Help - User Support - ${suffix}`)
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
  gtag('set', {
    [environment.gaLoggedInDimension]: loggedIn ? UserAuthState.LOGGED_IN : UserAuthState.LOGGED_OUT
  });
}

/**
 * Initializes the Google Analytics config using gtag.js. This method looks to
 * environment variables for various GA configuration parameters, and sets
 * initial values for contextual custom dimensions.
 */
export function initializeAnalytics() {
  gtag('js', new Date());
  gtag('set', {
    [environment.gaUserAgentDimension]: window.navigator.userAgent.slice(0, 100),
    [environment.gaLoggedInDimension]: UserAuthState.LOGGED_OUT
  });
  gtag('config', environment.gaId);
}
