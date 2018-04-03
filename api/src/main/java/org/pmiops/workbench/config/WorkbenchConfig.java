package org.pmiops.workbench.config;

import java.util.ArrayList;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheSpringConfiguration}. This should be kept in sync with files in the config/ directory.
 */
public class WorkbenchConfig {

  public FireCloudConfig firecloud;
  public AuthConfig auth;
  public CdrConfig cdr;
  public GoogleCloudStorageServiceConfig googleCloudStorageService;
  public GoogleDirectoryServiceConfig googleDirectoryService;
  public ServerConfig server;

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String billingAccountId;
    public String billingProjectPrefix;
    public String registeredDomainName;
    public boolean enforceRegistered;
    public String jupyterUserScriptUri;
  }

  public static class AuthConfig {
    // A list of GCP service accounts (not affiliated with researchers) that can be used to
    // make API calls.
    public ArrayList<String> serviceAccountApiUsers;
  }

  public static class CdrConfig {
    public String defaultCdrVersion;
    public boolean debugQueries;
  }

  public static class GoogleCloudStorageServiceConfig {
    public String credentialsBucketName;
  }

  public static class GoogleDirectoryServiceConfig {
    public String gSuiteDomain;
  }

  public static class ServerConfig {
    public String publicApiKeyForErrorReports;
    public String projectId;
  }
}
