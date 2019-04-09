export interface Environment {
  // Permanent environment variables.
  //
  // The URL to use when making API requests against the AoU API. This is used
  // by the core API / fetch modules and shouldn't be needed by most other components.
  // Example value: 'https://api-dot-all-of-us-rw-stable.appspot.com'
  allOfUsApiUrl: string;
  // The OAuth2 client ID. Used by the sign-in module to authenticate the user.
  // Example value: '56507752110-ovdus1lkreopsfhlovejvfgmsosveda6.apps.googleusercontent.com'
  clientId: string;
  // Indicates that the current server is a local server where client-side
  // debugging should be enabled (e.g. console.log, or devtools APIs).
  debug: boolean;
  // A prefix to add to the site title (shown in the tab title).
  // Example value: 'Test' would cause the following full title:
  // "Homepage | [Test] All of Us Research Workbench"
  displayTag: string;
  // Indicates if the displayTag should be shown in the web app. If it is true,
  // a small label will be added under the "All of Us" logo in the header.
  shouldShowDisplayTag: boolean;
  // The Google Analytics account ID for logging actions and page views.
  // Example value: 'UA-112406425-3'
  gaId: string;
  // The Google Analytics custom dimension ID for sending User Agent
  // info, allowing us to filter out Pingdom, Appscan, etc
  // This value should look like 'dimension1' with the digit
  // being the variable
  gaUserAgentDimension: string;
  // API endpoint to use for Leonardo (notebook proxy) API calls.
  // Example value: 'https://notebooks.firecloud.org'
  leoApiUrl: string;
  // The URL to use when making requests to the "public" API (e.g. non-signed-in
  // endpoints.
  // Example value: 'https://public-api-dot-all-of-us-rw-stable.appspot.com'
  publicApiUrl: string;
  // The URL to forward users to for the public UI (aka Data Browser).
  // Example value: 'https://public-ui-dot-all-of-us-rw-stable.appspot.com'
  publicUiUrl: string;
  // The Shibboleth URL for linking ERA Commons accounts
  shibbolethUrl: string;
  // The TCell API key. See RW-1682 for details.
  // Example value: 'AQEBBAEkx4iE2KxNyI7Wx08EwU1ycTM7E4FMSmaibbMUQxNU6uQvuAJt7fyABAtFYSYfgEE'
  tcellapikey: string;
  // The TCell app ID. See RW-1682 for details.
  // Example value: 'AoUNonProd-WZFW2'
  tcellappid: string;
  // The url for Moodle integration
  // Example value: https://aoudev.nnlm.gov
  trainingUrl: string;
  // The base URL for the Zendesk help center / user forum.
  // Example value: https://aousupporthelp.zendesk.com/hc/
  zendeskHelpCenterUrl: string;

  // Transient client-side flags.
  //
  // Whether users should be able to use JupyterLab as an option.
  // See RW-1966 for details.
  // Exit criteria: remove flag for Athens release.
  enableJupyterLab: boolean;

  // Whether users should be able to see the dataset builder.
  // See RW-2169 for details
  // Exit Criteria: remove flag for Bedford release.
  enableDatasetBuilder: boolean;

  // Whether users should be able to see the new UI for cohort builder search.
  enableCBListSearch: boolean;
}
