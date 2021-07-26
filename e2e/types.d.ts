export interface ICredentialConfig {
  USER_NAME: string;
  PASSWORD: string;
  INSTITUTION_CONTACT_EMAIL: string;
  // This is passed via a file to leave open the future option to allow token
  // refresh during a Puppeteer test run, and also limits logging exposure of the token.
  USER_ACCESS_TOKEN_FILE: string;
  COLLABORATOR_ACCESS_TOKEN_FILE: string;
  READER_ACCESS_TOKEN_FILE: string;
  WRITER_ACCESS_TOKEN_FILE: string;
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
  API_URL: string;
  EMAIL_DOMAIN_NAME: string;
  COLLABORATOR_USER: string;
  WRITER_USER: string;
  READER_USER: string;
  DEFAULT_CDR_VERSION_NAME: string;
  ALTERNATIVE_CDR_VERSION_NAME: string;
}

export interface IPageUrl {
  Home: string;
  Workspaces: string;
  Admin: string;
  Profile: string;
}
