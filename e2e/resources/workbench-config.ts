import * as fp from 'lodash/fp';

const env = process.env.WORKBENCH_ENV || 'test';

const userCredential = {
  userEmail: process.env.USER_NAME,
  userPassword: process.env.PASSWORD,
  institutionContactEmail: 'aou-dev-registration@broadinstitute.org',
  // This is passed via a file to leave open the future option to allow token
  // refresh during a Puppeteer test run, and also limits logging exposure of the token.
  userAccessTokenFilename: 'puppeteer-access-token.txt'
};

const urlPath = {
  loginUrlPath: '/login',
  workspacesUrlPath: '/workspaces',
  profileUrlPath: '/profile',
  libraryUrlPath: '/library',
  adminUrlPath: '/admin/user',
};

// localhost development server
const local = {
  uiBaseUrl: process.env.DEV_LOGIN_URL || 'http://localhost:4200',
  apiBaseUrl: process.env.DEV_API_URL || 'http://localhost/v1',
  userEmailDomain: '@fake-research-aou.org',
  collaboratorUsername: process.env.DEV_COLLABORATOR || 'puppetmaster@fake-research-aou.org',
  defaultCdrVersionName: 'Synthetic Dataset v3',
  altCdrVersionName: 'Synthetic Dataset v3 with Microarray',
};

// workbench test environment
const test = {
  uiBaseUrl: process.env.TEST_LOGIN_URL || 'https://all-of-us-workbench-test.appspot.com',
  apiBaseUrl: process.env.TEST_API_URL || 'https://api-dot-all-of-us-workbench-test.appspot.com/v1',
  userEmailDomain: '@fake-research-aou.org',
  collaboratorUsername: process.env.TEST_COLLABORATOR || 'puppetmaster@fake-research-aou.org',
  defaultCdrVersionName: 'Synthetic Dataset v3',
  altCdrVersionName: 'Synthetic Dataset v3 with Microarray',
};

// workbench staging environment
const staging = {
  uiBaseUrl: process.env.STAGING_LOGIN_URL || 'https://all-of-us-rw-staging.appspot.com',
  apiBaseUrl: process.env.STAGING_API_URL || 'https://api-dot-all-of-us-rw-staging.appspot.com/v1',
  userEmailDomain: '@staging.fake-research-aou.org',
  collaboratorUsername: process.env.STAGING_COLLABORATOR || 'puppetcitester4@staging.fake-research-aou.org',
  defaultCdrVersionName: 'Synthetic Dataset v4',
  altCdrVersionName: 'Synthetic Dataset v3',
};

// workbench stable environment
const stable = {
  uiBaseUrl: process.env.STABLE_LOGIN_URL,
  apiBaseUrl: process.env.STABLE_API_URL,
  userEmailDomain: '@stable.fake-research-aou.org',
  defaultCdrVersionName: 'Synthetic Dataset v4',
  altCdrVersionName: 'Synthetic Dataset v3',
};


const environment = {
  local,
  test,
  staging,
  stable,
};

export const config = fp.mergeAll([environment[env], userCredential, urlPath]);
