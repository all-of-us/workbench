require('dotenv').config();
const _ = require('lodash');

const env = process.env.WORKBENCH_ENV || 'test';

const userCredential = {
    userEmail: process.env.USER_NAME,
    userPassword: process.env.PASSWORD,
    userInvitationkey: process.env.INVITATION_KEY,
    registerationContactEmail: 'hermione.owner@quality.firecloud.org'
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
  slowMotion: parseInt(process.env.PUPPETEER_SLOWMO) || 10, // milliseconds
  isDevTools: process.env.PUPPETEER_DEVTOOLS || false,
  puppeteerUserAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36'
  };

// localhost development server
const local = {
    uiBaseUrl: process.env.DEV_LOGIN_URL || "https://localhost",
    apiBaseUrl: process.env.DEV_API_URL || "https://api-localhost/v1"
};

// workbench test environments
const test = {
    uiBaseUrl: process.env.TEST_LOGIN_URL || "https://all-of-us-workbench-test.appspot.com",
    apiBaseUrl: process.env.TEST_API_URL || "https://api-dot-all-of-us-workbench-test.appspot.com/v1"
};

const staging = {
    uiBaseUrl: process.env.STAGING_LOGIN_URL || "https://all-of-us-rw-staging.appspot.com",
    apiBaseUrl: process.env.STAGING_API_URL || "https://api-dot-all-of-us-rw-staging.appspot.com/v1"
};

const production = {
    uiBaseUrl: process.env.PRODUCTION_LOGIN_URL,
    apiBaseUrl: process.env.PRODUCTION_API_URL
};

const environment = {
  local,
  test,
  staging,
  production
};

const configs = _.merge(environment[env], userCredential, urlPath, puppeteer);

// uncomment for configs inspection
// console.log(`env: ${env} configs: ${JSON.stringify(configs, undefined, configs.json_indentation)}`);

module.exports = configs;
