package org.pmiops.workbench.config;

import java.util.ArrayList;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheSpringConfiguration}. This should be kept in sync with files in the config/
 * directory.
 */
public class WorkbenchConfig {

  public FireCloudConfig firecloud;
  public AuthConfig auth;
  public CdrConfig cdr;
  public GoogleCloudStorageServiceConfig googleCloudStorageService;
  public GoogleDirectoryServiceConfig googleDirectoryService;
  public ServerConfig server;
  public AdminConfig admin;
  public MandrillConfig mandrill;
  public ElasticsearchConfig elasticsearch;
  public MoodleConfig moodle;
  public AccessConfig access;
  public CohortBuilderConfig cohortbuilder;
  public FeatureFlagsConfig featureFlags;
  public BillingConfig billing;

  /** Creates a config with non-null-but-empty member variables, for use in testing. */
  public static WorkbenchConfig createEmptyConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.access = new AccessConfig();
    config.admin = new AdminConfig();
    config.auth = new AuthConfig();
    config.auth.serviceAccountApiUsers = new ArrayList();
    config.cdr = new CdrConfig();
    config.cohortbuilder = new CohortBuilderConfig();
    config.elasticsearch = new ElasticsearchConfig();
    config.featureFlags = new FeatureFlagsConfig();
    config.firecloud = new FireCloudConfig();
    config.googleCloudStorageService = new GoogleCloudStorageServiceConfig();
    config.googleDirectoryService = new GoogleDirectoryServiceConfig();
    config.mandrill = new MandrillConfig();
    config.moodle = new MoodleConfig();
    config.server = new ServerConfig();
    config.billing = new BillingConfig();
    return config;
  }

  public static class BillingConfig {
    public Integer retryCount;
    public Integer bufferCapacity;
    public String projectNamePrefix;
    public String accountId;
    public String exportBigQueryTable;
    public Double defaultFreeCreditsLimit;
  }

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String baseUrl;
    public Integer clusterMaxAgeDays;
    public Integer clusterIdleMaxAgeDays;
    public String registeredDomainName;
    public String registeredDomainGroup;
    public String leoBaseUrl;
    // This value specifies the information we hand to Terra as our AppId header.
    // It is primarily used for metrics gathering information.
    public String xAppIdValue;
    // The name of the VPC service perimeter to create our Terra GCP projects inside,
    // if enabled.
    public String vpcServicePerimeterName;
    // The length of our API HTTP client timeouts to firecloud
    public Integer timeoutInSeconds;
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
    public String clusterResourcesBucketName;
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
    // Controls whether all api requests are traced and sent to Stackdriver tracing, or
    // whether we only trace at the default frequency.
    public boolean traceAllRequests;
  }

  public static class AdminConfig {
    public String adminIdVerification;
    public String loginUrl;
  }

  public static class MandrillConfig {
    public String fromEmail;
    public int sendRetries;
  }

  public static class ElasticsearchConfig {
    public String baseUrl;
    public boolean enableBasicAuth;
    public boolean enableElasticsearchBackend;
  }

  public static class MoodleConfig {
    public String host;
    public boolean enableMoodleBackend;
  }

  // The access object specifies whether each of the following access requirements block access
  // to the workbench.
  public static class AccessConfig {
    // Allows a user to bypass their own access modules. This is used for testing purposes so that
    // We can give control over 3rd party access modules
    public boolean unsafeAllowSelfBypass;
    public boolean enableComplianceTraining;
    public boolean enableEraCommons;
    public boolean enableDataUseAgreement;
    public boolean enableBetaAccess;
  }

  public static class CohortBuilderConfig {
    public boolean enableListSearch;
  }

  public static class FeatureFlagsConfig {
    // Allows a user to delete their own account. This is used for testing purposes so that
    // We can clean up after ourselves. This should never go to prod.
    public boolean unsafeAllowDeleteUser;
    // Whether or not AoU should request Terra to create GCP projects with high-security VPC
    // functionality and VPC flow logs enabled.
    public boolean enableVpcFlowLogs;
    // Whether or not AoU should request Terra to create GCP projects inside a VPC
    // security perimeter.
    public boolean enableVpcServicePerimeter;
    // Flag to indicate if the new create account as per CAPS requirement
    public boolean enableNewAccountCreation;
  }
}
