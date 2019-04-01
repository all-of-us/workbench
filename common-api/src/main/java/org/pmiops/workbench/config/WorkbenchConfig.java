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
  public AdminConfig admin;
  public JiraConfig jira;
  public MandrillConfig mandrill;
  public ElasticsearchConfig elasticsearch; 
  public MoodleConfig moodle;
  public AccessConfig access;
  public CohortBuilderConfig cohortbuilder;

  /**
   * Creates a config with non-null-but-empty member variables, for use in testing.
   */
  public static WorkbenchConfig createEmptyConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.auth = new AuthConfig();
    config.cdr = new CdrConfig();
    config.googleCloudStorageService = new GoogleCloudStorageServiceConfig();
    config.googleDirectoryService = new GoogleDirectoryServiceConfig();
    config.server = new ServerConfig();
    config.admin = new AdminConfig();
    config.jira = new JiraConfig();
    config.mandrill = new MandrillConfig();
    config.elasticsearch = new ElasticsearchConfig();
    config.moodle = new MoodleConfig();
    config.access = new AccessConfig();
    config.cohortbuilder = new CohortBuilderConfig();
    return config;
  }

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String baseUrl;
    public String billingAccountId;
    public String billingProjectPrefix;
    public Integer clusterMaxAgeDays;
    public Integer clusterIdleMaxAgeDays;
    public String registeredDomainName;
    public boolean enforceRegistered;
    public String jupyterUserScriptUri;
    public String jupyterPlaygroundExtensionUri;
    public String leoBaseUrl;
    public Integer billingRetryCount;
  }

  public static class AuthConfig {
    // A list of GCP service accounts (not affiliated with researchers) that can be used to
    // make API calls.
    public ArrayList<String> serviceAccountApiUsers;
  }

  public static class CdrConfig {
    public boolean debugQueries;
  }

  public static class GoogleCloudStorageServiceConfig {
    public String credentialsBucketName;
    public String emailImagesBucketName;
    public String demosBucketName;
  }

  public static class GoogleDirectoryServiceConfig {
    public String gSuiteDomain;
  }

  public static class ServerConfig {
    public String apiBaseUrl;
    public String publicApiKeyForErrorReports;
    public String projectId;
    public String shortName;
    public String oauthClientId;
  }

  public static class AdminConfig {
    public String adminIdVerification;
    public String loginUrl;
  }

  public static class JiraConfig {
    public String projectKey;
    public String cdrProjectKey;
  }

  public static class MandrillConfig {
    public String fromEmail;
    public int sendRetries;
  }

  public static class ElasticsearchConfig {
    public String host;
    public boolean enableElasticsearchBackend;
  }

  public static class MoodleConfig {
    public String host;
    public boolean enableMoodleBackend;
  }

  // The access object specifies whether each of the following access requirements block access
  // to the workbench.
  public static class AccessConfig {
    public boolean enableComplianceTraining;
    public boolean enableEraCommons;
    public boolean enableDataUseAgreement;
    public boolean enableBetaAccess;
  }

  public static class CohortBuilderConfig {
    public boolean enableListSearch;
  }
}
