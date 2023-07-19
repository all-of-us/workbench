// The Zendesk environment instance. Note that sandbox is a mirror which has
// different content IDs and separate SAML integration. See also app/utils/zendesk.ts.
export enum ZendeskEnv {
  Prod = 'prod',
  Preprod = 'preprod',
  Sandbox = 'sandbox',
}

export interface EnvironmentBase {
  // Permanent environment variables.
  //
  //
  // The URL to use when making API requests against the AoU API. This is used
  // by the core API / fetch modules and shouldn't be needed by most other components.
  // Example value: 'https://api.stable.fake-research-aou.org'
  allOfUsApiUrl: string;
  // The OAuth2 client ID. Used by the sign-in module to authenticate the user.
  // Example value: '56507752110-ovdus1lkreopsfhlovejvfgmsosveda6.apps.googleusercontent.com'
  clientId: string;
  // The Google Analytics account ID for logging actions and page views.
  // Example value: 'UA-112406425-3'
  gaId: string;
  // The Google Analytics custom dimension ID for sending User Agent
  // info, allowing us to filter out Pingdom, Appscan, etc
  // This value should look like 'dimension1' or similar,
  // matching the value in GA -> Admin -> Custom Dimensions.
  gaUserAgentDimension: string;
  // The Google Analytics custom dimension ID for sending logged-in state,
  // allowing us to distinguish between unauthenticated and logged-in sessions.
  // This value should look like 'dimension2' or similar,
  // matching the value in GA -> Admin -> Custom Dimensions.
  gaLoggedInDimension: string;
  // The Google Analytics custom dimension ID for distinguishing between researchers and operational users,
  // by checking the name of the user's institution.
  // This value should look like 'dimension3' or similar,
  // matching the value in GA -> Admin -> Custom Dimensions.
  gaUserInstitutionCategoryDimension: string;
  // API endpoint to use for Leonardo (notebook proxy) API calls.
  // Example value: 'https://notebooks.firecloud.org'
  leoApiUrl: string;
  // The URL to forward users to for the public UI (aka Data Browser).
  // Example value: 'https://aou-db-stable.appspot.com'
  publicUiUrl: string;
  // The Zendesk environment corresponding to this deployment.
  zendeskEnv: ZendeskEnv;
  inactivityTimeoutSeconds: number;
  inactivityWarningBeforeSeconds: number;
  // Captcha site key registered with the domain
  captchaSiteKey: string;

  // Environment flags: UI-specific feature flags
  //
  // WARNING: Please think *very* carefully before adding a new environment flag here! Instead
  // of this file, prefer storing feature flags in the server-side WorkbenchConfig and passing them
  // to the UI via ConfigController and serverConfigStore.
  //
  // The UI environment config should be restricted to truly UI-specific environment variables, such
  // as server API endpoints and client IDs.

  // Indicates if the displayTag should be shown in the web app. If it is true,
  // a small label will be added under the "All of Us" logo in the header.
  shouldShowDisplayTag: boolean;
  // Whether to allow for sign in token overrides; alternate auth scheme for testing purposes.
  allowTestAccessTokenOverride: boolean;
  // Show the new Analysis Tab in the UI
  showNewAnalysisTab: boolean;
}

export interface Environment extends EnvironmentBase {
  // Indicates that the current server is a local server where client-side
  // debugging should be enabled (e.g. console.log, or devtools APIs).
  debug: boolean;

  // A prefix to add to the site title (shown in the tab title).
  // Example value: 'Test' would cause the following full title:
  // "Homepage | [Test] All of Us Researcher Workbench"
  displayTag: string;
}
