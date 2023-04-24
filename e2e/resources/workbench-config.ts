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
  API_HOSTNAME: process.env.API_HOSTNAME || 'localhost:8081',
  EMAIL_DOMAIN_NAME: '@fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetmaster@fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetcitester1@fake-research-aou.org',
  ACCESS_TEST_USER: process.env.ACCESS_TEST_USER || 'puppet-access-tester-1@fake-research-aou.org',
  ADMIN_TEST_USER: process.env.ADMIN_TEST_USER || 'puppeteer-admin-1@fake-research-aou.org',
  EGRESS_TEST_USER: process.env.EGRESS_TEST_USER || 'puppeteer-egress-1@fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v2',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'Synthetic Dataset in the Controlled Tier v4',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  RAS_TEST_USER: process.env.RAS_TEST_USER,
  ENABLED_PERSISTENT_DISK: true
};

// workbench test environment
const test: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.TEST_LOGIN_URL || 'https://all-of-us-workbench-test.appspot.com',
  API_HOSTNAME: process.env.API_HOSTNAME || 'api-dot-all-of-us-workbench-test.appspot.com',
  EMAIL_DOMAIN_NAME: '@fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetmaster@fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetcitestreader1@fake-research-aou.org',
  ACCESS_TEST_USER: process.env.ACCESS_TEST_USER || 'puppet-access-tester-1@fake-research-aou.org',
  ADMIN_TEST_USER: process.env.ADMIN_TEST_USER || 'puppeteer-admin-1@fake-research-aou.org',
  EGRESS_TEST_USER: process.env.EGRESS_TEST_USER || 'puppeteer-egress-1@fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v2',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'Synthetic Dataset in the Controlled Tier v4',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  RAS_TEST_USER: process.env.RAS_TEST_USER,
  ENABLED_PERSISTENT_DISK: true
};

// workbench staging environment
const staging: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.STAGING_LOGIN_URL || 'https://staging.fake-research-aou.org',
  API_HOSTNAME: process.env.API_HOSTNAME || 'api.staging.fake-research-aou.org',
  EMAIL_DOMAIN_NAME: '@staging.fake-research-aou.org',
  WRITER_USER: process.env.WRITER_USER || 'puppetmaster@staging.fake-research-aou.org',
  READER_USER: process.env.READER_USER || 'puppetcistagingreader1@staging.fake-research-aou.org',
  ACCESS_TEST_USER: process.env.ACCESS_TEST_USER || 'puppet-access-tester-1@staging.fake-research-aou.org',
  ADMIN_TEST_USER: process.env.ADMIN_TEST_USER || 'puppeteer-admin-1@staging.fake-research-aou.org',
  EGRESS_TEST_USER: process.env.EGRESS_TEST_USER || 'puppeteer-egress-1@staging.fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'Synthetic Dataset in the Controlled Tier v4',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  RAS_TEST_USER: process.env.RAS_TEST_USER,
  ENABLED_PERSISTENT_DISK: true
};

// NOT WORKING: workbench stable environment
const stable: IEnvConfig = {
  LOGIN_URL_DOMAIN_NAME: process.env.STABLE_LOGIN_URL || 'https://stable.fake-research-aou.org/login',
  API_HOSTNAME: process.env.API_HOSTNAME || 'api.stable.fake-research-aou.org',
  EMAIL_DOMAIN_NAME: '@stable.fake-research-aou.org',
  DEFAULT_CDR_VERSION_NAME: 'Synthetic Dataset v4',
  OLD_CDR_VERSION_NAME: 'Synthetic Dataset v3',
  WRITER_USER: 'TODO - NOT AVAILABLE',
  READER_USER: 'TODO - NOT AVAILABLE',
  ACCESS_TEST_USER: 'TODO - NOT AVAILABLE',
  ADMIN_TEST_USER: 'TODO - NOT AVAILABLE',
  EGRESS_TEST_USER: 'TODO - NOT AVAILABLE',
  CONTROLLED_TIER_CDR_VERSION_NAME: 'TODO - NOT AVAILABLE',
  LOGIN_GOV_USER: 'aou-dev-registration@broadinstitute.org',
  RAS_TEST_USER: process.env.RAS_TEST_USER,
  ENABLED_PERSISTENT_DISK: true
};

const environment = {
  local,
  test,
  staging,
  stable
};

type Config = IEnvConfig & ICredentialConfig & IPathConfig;
export const config: Config = fp.mergeAll([environment[env], userCredential, urlPath]);
