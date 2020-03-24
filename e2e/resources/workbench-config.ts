require('dotenv').config();
const _ = require('lodash');

const env = process.env.WORKBENCH_ENV || 'dev';

const userCredential = {
  contactEmail: 'hermione.owner@quality.firecloud.org',
  userEmail: process.env.USER_NAME,
  userPassword: process.env.PASSWORD,
  userInvitationkey: process.env.INVITATION_KEY,
};

const urlPath = {
  loginUrlPath: '/login',
  workspacesUrlPath: '/workspaces',
  profileUrlPath: '/profile',
  libraryUrlPath: '/library',
  adminUrlPath: '/admin/user',
};

const puppeteer = {
  puppeteerUserAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36',
};

// localhost development server
const local = {
  uiBaseUrl: process.env.DEV_LOGIN_URL || 'https://localhost',
  apiBaseUrl: process.env.DEV_API_URL || 'https://api-localhost/v1',
  userEmailDomain: '@fake-research-aou.org',
};

// workbench test environment
const dev = {
  uiBaseUrl: process.env.TEST_LOGIN_URL || 'https://all-of-us-workbench-test.appspot.com',
  apiBaseUrl: process.env.TEST_API_URL || 'https://api-dot-all-of-us-workbench-test.appspot.com/v1',
  userEmailDomain: '@fake-research-aou.org',
};

// workbench staging environment
const staging = {
  uiBaseUrl: process.env.STAGING_LOGIN_URL || 'https://all-of-us-rw-staging.appspot.com',
  apiBaseUrl: process.env.STAGING_API_URL || 'https://api-dot-all-of-us-rw-staging.appspot.com/v1',
  userEmailDomain: '@staging.fake-research-aou.org',
};

// workbench stable environment
const stable = {
  uiBaseUrl: process.env.STABLE_LOGIN_URL,
  apiBaseUrl: process.env.STABLE_API_URL,
  userEmailDomain: '@stable.fake-research-aou.org',
};


const environment = {
  local,
  dev,
  staging,
  stable,
};

const configs = _.merge(environment[env], userCredential, urlPath, puppeteer);

module.exports = configs;
