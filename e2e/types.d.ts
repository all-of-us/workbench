export interface IConfig {
  USER_NAME: string;
  PASSWORD: string;
  LOGIN_URL_DOMAIN_NAME: string;
  API_URL: string;
  EMAIL_DOMAIN_NAME: string;
  COLLABORATOR_USER: string;
  WRITER_USER: string;
  READER_USER: string;
  DEFAULT_CDR_VERSION_NAME: string;
  ALTERNATIVE_CDR_VERSION_NAME: string;
  USER_ACCESS_TOKEN_FILE: string;
  COLLABORATOR_ACCESS_TOKEN_FILE: string;
  READER_ACCESS_TOKEN_FILE: string;
  WRITER_ACCESS_TOKEN_FILE: string;
  LOGIN_URL_PATH: string;
  WORKSPACES_URL_PATH: string;
  PROFILE_URL_PATH: string;
  LIBRARY_URL_PATH: string;
  ADMIN_URL_PATH: string;
  INSTITUTION_CONTACT_EMAIL: string;
}

export interface IPageUrl {
  Home: string;
  Workspaces: string;
  Admin: string;
  Profile: string;
}
