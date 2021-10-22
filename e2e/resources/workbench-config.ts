import * as fp from 'lodash/fp';
import { ICredentialConfig, IEnvConfig, IPathConfig } from 'types';

const env = process.env.WORKBENCH_ENV || 'test';

const userCredential: ICredentialConfig = {
  USER_NAME: process.env.USER_NAME,
  PASSWORD: process.env.PASSWORD,
  INSTITUTION_CONTACT_EMAIL: 'aou-dev-registration@broadinstitute.org',
  LOGIN_GOV_PASSWORD: process.env.LOGIN_GOV_PASSWORD,
  LOGIN_GOV_2FA_SECRET: process.env.LOGIN_GOV_2FA_SECRET
};

const urlPath: IPathConfig = {
  LOGIN_URL_PATH: '/login',
  WORKSPACES_URL_PATH: '/workspaces',
  PROFILE_URL_PATH: '/profile',
  LIBRARY_URL_PATH: '/library',
  ADMIN_URL_PATH: '/admin/user'
};

// localhost development server
const local: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.DEV_LOGIN_URL || 'http://localhost:4200',
  API_URL: process.env.DEV_API_URL || 'http://localhost/v1',
  EMAIL_DOMAIN_NAME: '@fake-research-aou.org',
  COLLABORATOR_USER: process.env.COLLABORATOR_USER || 'puppetmaster@fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetmaster@fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetcitester1@fake-research-aou.org',
  ACCESS_TEST_USER: process.env.ACCESS_TEST_USER || 'puppet-access-tester-1@fake-research-aou.org',
  ADMIN_TEST_USER: process.env.ADMIN_TEST_USER || 'puppeteer-admin-1@fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v2',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'Synthetic Dataset in the Controlled Tier',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  ENABLED_PERSISTENT_DISK: true
};

// workbench test environment
const test: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.TEST_LOGIN_URL || 'https://all-of-us-workbench-test.appspot.com',
  API_URL: process.env.TEST_API_URL || 'https://api-dot-all-of-us-workbench-test.appspot.com/v1',
  EMAIL_DOMAIN_NAME: '@fake-research-aou.org',
  COLLABORATOR_USER: process.env.COLLABORATOR_USER || 'puppetmaster@fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetmaster@fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetcitestreader1@fake-research-aou.org',
  ACCESS_TEST_USER: process.env.ACCESS_TEST_USER || 'puppet-access-tester-1@fake-research-aou.org',
  ADMIN_TEST_USER: process.env.ADMIN_TEST_USER || 'puppeteer-admin-1@fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v2',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'Synthetic Dataset in the Controlled Tier',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  ENABLED_PERSISTENT_DISK: true
};

// workbench staging environment
const staging: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.STAGING_LOGIN_URL || 'https://all-of-us-rw-staging.appspot.com',
  API_URL: process.env.STAGING_API_URL || 'https://api-dot-all-of-us-rw-staging.appspot.com/v1',
  EMAIL_DOMAIN_NAME: '@staging.fake-research-aou.org',
  COLLABORATOR_USER: process.env.COLLABORATOR_USER || 'puppetcitester4@staging.fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetmaster@staging.fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetcistagingreader1@staging.fake-research-aou.org',
  ACCESS_TEST_USER: process.env.ACCESS_TEST_USER || 'puppet-access-tester-1@staging.fake-research-aou.org',
  ADMIN_TEST_USER: process.env.ADMIN_TEST_USER || 'puppeteer-admin-1@staging.fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'Synthetic Dataset in the Controlled Tier',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  ENABLED_PERSISTENT_DISK: false
};

// NOT WORKING: workbench stable environment
const stable: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.STABLE_LOGIN_URL,
  API_URL: process.env.STABLE_API_URL,
  EMAIL_DOMAIN_NAME: '@stable.fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  COLLABORATOR_USER: 'TODO - NOT AVAILABLE',
  WRITER_USER: 'TODO - NOT AVAILABLE',
  READER_USER: 'TODO - NOT AVAILABLE',
  ACCESS_TEST_USER: 'TODO - NOT AVAILABLE',
  ADMIN_TEST_USER: 'TODO - NOT AVAILABLE',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'TODO - NOT AVAILABLE',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  ENABLED_PERSISTENT_DISK: false
};

// workbench perf environment
const perf: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.PERF_LOGIN_URL || 'https://all-of-us-rw-perf.appspot.com',
  API_URL: process.env.PERF_API_URL || 'https://api-dot-all-of-us-rw-perf.appspot.com/v1',
  EMAIL_DOMAIN_NAME: '@perf.fake-research-aou.org',
  COLLABORATOR_USER: process.env.COLLABORATOR_USER || 'puppetciperfreader@perf.fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetciperfwriter1@perf.fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetciperfreader@perf.fake-research-aou.org',
  ACCESS_TEST_USER: 'TODO - NOT AVAILABLE',
  ADMIN_TEST_USER: 'TODO - NOT AVAILABLE',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'TODO - NOT AVAILABLE',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  ENABLED_PERSISTENT_DISK: false
};

const environment = {
  local,
  test,
  staging,
  stable,
  perf
};

export const config = fp.mergeAll([environment[env], userCredential, urlPath]);
