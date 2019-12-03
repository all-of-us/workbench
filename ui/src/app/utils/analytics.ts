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

export const AnalyticsTracker = {
  Workspaces: {
    OpenCreatePage: () => triggerEvent('Workspaces', 'Open Create Page', getCurrentPageLabel()),
    Create: () => triggerEvent('Workspaces', 'Create')
  }
};

function getCurrentPageLabel() {
  if (window.location.pathname === '/') {
    return 'From Home Page';
  } else if (window.location.pathname === '/workspaces') {
    return 'From Workspace List Page';
  } else {
    return 'Unknown Label: ' + window.location.pathname;
  }
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
