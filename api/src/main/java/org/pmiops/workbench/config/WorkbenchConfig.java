package org.pmiops.workbench.config;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheSpringConfiguration}. This should be kept in sync with files in the config/ directory.
 */
public class WorkbenchConfig {

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String billingAccountId;
    public String billingProjectPrefix;
    public String registeredDomainName;
    public boolean enforceRegistered;
  }

  public FireCloudConfig firecloud;

  public static class CdrConfig {
    public String defaultCdrVersion;
  }

  public CdrConfig cdr;

  public static class GoogleCloudStorageServiceConfig {
    public String credentialsBucketName;
  }

  public GoogleCloudStorageServiceConfig googleCloudStorageService;

  public static class GoogleDirectoryServiceConfig {
    public String gSuiteDomain;
  }

  public GoogleDirectoryServiceConfig googleDirectoryService;

  public static class ServerConfig {
    public String stackdriverApiKey;
    public String projectId;
  }

  public ServerConfig server;
}
