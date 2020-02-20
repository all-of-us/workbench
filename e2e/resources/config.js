require('dotenv').config();
const _ = require('lodash');

const env = process.env.WORKBENCH_ENV || 'test';

const userCredential = {
  registerationContactEmail: process.env.CONTACT_EMAIL,
  userEmail: process.env.USER_NAME,
  userPassword: process.env.PASSWORD,
  userInvitationkey: process.env.INVITATION_KEY,
};

const urlPath = {
  loginUrlPath: "/login",
  workspacesUrlPath: "/workspaces",
  profileUrlPath: "/profile",
  libraryUrlPath: "/library",
  adminUrlPath: "/admin/user",
};

const puppeteer = {
  isHeadless: process.env.PUPPETEER_HEADLESS === true,
  slowMotion: 10, // milliseconds
  isDevTools: process.env.PUPPETEER_DEVTOOLS || false,
  puppeteerUserAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36',
};

// localhost development server
const local = {
  uiBaseUrl: process.env.DEV_LOGIN_URL || "https://localhost",
  apiBaseUrl: process.env.DEV_API_URL || "https://api-localhost/v1",
  userEmailDomain: "@fake-research-aou.org",
};

// workbench test environments
const test = {
  uiBaseUrl: process.env.TEST_LOGIN_URL || "https://all-of-us-workbench-test.appspot.com",
  apiBaseUrl: process.env.TEST_API_URL || "https://api-dot-all-of-us-workbench-test.appspot.com/v1",
  userEmailDomain: "@fake-research-aou.org",
};

const staging = {
  uiBaseUrl: process.env.STAGING_LOGIN_URL || "https://all-of-us-rw-staging.appspot.com",
  apiBaseUrl: process.env.STAGING_API_URL || "https://api-dot-all-of-us-rw-staging.appspot.com/v1",
  userEmailDomain: "@staging.fake-research-aou.org",
};

const stable = {
  uiBaseUrl: process.env.PRODUCTION_LOGIN_URL,
  apiBaseUrl: process.env.PRODUCTION_API_URL,
  userEmailDomain: "@stable.fake-research-aou.org",
};

const production = {
  uiBaseUrl: process.env.PRODUCTION_LOGIN_URL,
  apiBaseUrl: process.env.PRODUCTION_API_URL,
  userEmailDomain: "I_DO_NOT_CARE",
};

const environment = {
  local,
  test,
  staging,
  stable,
  production
};

const configs = _.merge(environment[env], userCredential, urlPath, puppeteer);

// uncomment for configs inspection
// console.log(`env: ${env}`);
// console.log(`${JSON.stringify(configs)}`);

module.exports = configs;
