export interface IConfig {
  USER_NAME: string;
  PASSWORD: string;
  LOGIN_URL: string;
  API_URL: string;
  DOMAIN: string;
  COLLABORATOR_USER: string;
  WRITER_USER: string;
  READER_USER: string;
  DEFAULT_CDR_VERSION: string;
  ALTERNATIVE_CDR_VERSION: string;
  userAccessTokenFilename: string;
  collaboratorUserAccessTokenFilename: string;
  readerUserAccessTokenFilename: string;
  writerUserAccessTokenFilename: string;
  loginUrlPath: string;
  workspacesUrlPath: string;
  profileUrlPath: string;
  libraryUrlPath: string;
  adminUrlPath: string;
  institutionContactEmail: string;
}
