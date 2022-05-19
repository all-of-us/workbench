export interface ICredentialConfig {
  USER_NAME: string;
  PASSWORD: string;
  INSTITUTION_CONTACT_EMAIL: string;
  LOGIN_GOV_PASSWORD: string;
  LOGIN_GOV_2FA_SECRET: string;
}

export interface IPathConfig {
  LOGIN_URL_PATH: string;
  WORKSPACES_URL_PATH: string;
  PROFILE_URL_PATH: string;
  LIBRARY_URL_PATH: string;
  ADMIN_URL_PATH: string;
}

export interface IEnvConfig {
  LOGIN_URL_DOMAIN_NAME: string;
  API_HOSTNAME: string;
  EMAIL_DOMAIN_NAME: string;
  WRITER_USER: string;
  READER_USER: string;
  ACCESS_TEST_USER: string;
  ADMIN_TEST_USER: string;
  EGRESS_TEST_USER: string;
  DEFAULT_CDR_VERSION_NAME: string;
  OLD_CDR_VERSION_NAME: string;
  CONTROLLED_TIER_CDR_VERSION_NAME: string;
  LOGIN_GOV_USER: string;
  RAS_TEST_USER: string;
  ENABLED_PERSISTENT_DISK: boolean;
}

export interface IPageUrl {
  Home: string;
  Workspaces: string;
  Admin: string;
  Profile: string;
}
