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
  public ZendeskConfig zendesk;
  public AccessConfig access;
  public CohortBuilderConfig cohortbuilder;
  public FeatureFlagsConfig featureFlags;
  public BillingConfig billing;
  public ActionAuditConfig actionAudit;
  public RdrExportConfig rdrExport;
  public CaptchaConfig captcha;

  /** Creates a config with non-null-but-empty member variables, for use in testing. */
  public static WorkbenchConfig createEmptyConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.access = new AccessConfig();
    config.admin = new AdminConfig();
    config.auth = new AuthConfig();
    config.auth.serviceAccountApiUsers = new ArrayList<>();
    config.cdr = new CdrConfig();
    config.cohortbuilder = new CohortBuilderConfig();
    config.elasticsearch = new ElasticsearchConfig();
    config.featureFlags = new FeatureFlagsConfig();
    config.firecloud = new FireCloudConfig();
    config.googleCloudStorageService = new GoogleCloudStorageServiceConfig();
    config.googleDirectoryService = new GoogleDirectoryServiceConfig();
    config.mandrill = new MandrillConfig();
    config.moodle = new MoodleConfig();
    config.zendesk = new ZendeskConfig();
    config.server = new ServerConfig();
    config.billing = new BillingConfig();
    config.actionAudit = new ActionAuditConfig();
    config.rdrExport = new RdrExportConfig();
    config.captcha = new CaptchaConfig();
    return config;
  }

  // Environment config variables related to billing and the billing project buffer (which buffers
  // GCP projects, aka "billing projects" in Terra terminology).
  public static class BillingConfig {
    // This config variable seems to be unused.
    public Integer retryCount;
    // The total capacity of the GCP project buffer. The buffering system will not attempt to create
    // any new projects when the total number of in-progress & ready projects is at or above this
    // level.
    public Integer bufferCapacity;
    // The number of times to attempt project creation per cron task execution. This effectively
    // controls the max rate of project refill. If the cron task is configured to run once per
    // minute and this param is set to 5, then the buffer system will create up to approximately
    // 5 projects per minute.
    //
    // Per guidance from Google Cloud's project infrastructure team, we should limit our total rate
    // of project creation to a number less than 1 per second. In practice, a reasonable aggressive
    // value for this parameter would be 5-10 project refills per minute.
    public Integer bufferRefillProjectsPerTask;
    // The environment-driven prefix to apply to GCP projects created in the buffer. Example:
    // "aou-rw-perf-" causes the buffer to create projects named like "aou-rw-perf-8aec175b".
    public String projectNamePrefix;
    // The free tier GCP billing account ID to associate with Terra / GCP projects.
    public String accountId;

    public String freeTierBillingAccountName() {
      return "billingAccounts/" + accountId;
    }

    // The full table name for the BigQuery billing export, which is read from by the free-tier
    // usage tracking cron endpoint.
    public String exportBigQueryTable;
    // The default dollar limit to apply to free-credit usage in this environment.
    public Double defaultFreeCreditsDollarLimit;
    // Thresholds for email alerting based on free tier usage, by cost
    public ArrayList<Double> freeTierCostAlertThresholds;
    // For project garbage collection, the max # of projects allowed to be associated with each
    // garbage-collection service account.
    public Integer garbageCollectionUserCapacity;
    // A list of GCP service accounts for billing project garbage collection
    public ArrayList<String> garbageCollectionUsers;
  }

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String baseUrl;
    public Integer clusterMaxAgeDays;
    public Integer clusterIdleMaxAgeDays;
    public String clusterDefaultMachineType;
    public Integer clusterDefaultDiskSizeGb;
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
    // The docker image that we use for our jupyter images
    public String jupyterDockerImage;
    // The docker image that we use for our welder images
    public String welderDockerImage;
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
    // Base URL for the webapp (e.g. client / ui service).
    public String clientBaseUrl;
    // Base URL for the App Engine API service (e.g. backend server).
    public String apiBaseUrl;
    public String publicApiKeyForErrorReports;
    public String projectId;
    public String shortName;
    public String oauthClientId;
    // Controls whether all api requests are traced and sent to Stackdriver tracing, or
    // whether we only trace at the default frequency.
    public boolean traceAllRequests;
    public String appEngineLocationId;
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
    public String credentialsKeyV1;
    public String credentialsKeyV2;
  }

  public static class ZendeskConfig {
    public String host;
  }

  // Config related to user sign-up and registration, including access modules and controls around
  // the sign-up flow.
  public static class AccessConfig {
    // Allows a user to bypass their own access modules. This is used for testing purposes so that
    // We can give control over 3rd party access modules
    public boolean unsafeAllowSelfBypass;
    // These booleans control whether each of our core access modules are enabled per environment.
    public boolean enableComplianceTraining;
    public boolean enableEraCommons;
    public boolean enableDataUseAgreement;
    public boolean enableBetaAccess;
    // Controls whether an invitation key is required for user creation. When true, the account
    // creation UI will show an invitation key form and the server will validate the key before
    // proceeding.
    public boolean requireInvitationKey;
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
    // Flag to indicate if USER/WORKSPACE data is exported to RDR
    public boolean enableRdrExport;
    // Setting this to true will prevent users from making compute increasing operations on
    // inactive billing workspaces
    // See RW-3209.
    public boolean enableBillingLockout;
    // Whether or not AoU should handle inbound SumoLogic high-egress event requests.
    // See RW-2253.
    public boolean enableSumoLogicEventHandling;
    // Causes the server to use an API-based method for generating delegated user credentials,
    // as opposed to reading service account private keys from GCS.
    // See RW-2840.
    public boolean useKeylessDelegatedCredentials;
    // Whether we send emails to users after they pass Free Tier usage thresholds
    // Blocked by RW-4135: do not enable in an environment where contact_email can be NULL
    public boolean sendFreeTierAlertEmails;
    // Flag to indicate whether to use the new Moodle badges API
    // https://precisionmedicineinitiative.atlassian.net/browse/RW-2957
    public boolean enableMoodleV2Api;
    // Do we require new users to have a contact email with a verified institutional affiliation,
    // enforced by pattern-matching the user's contact email against the institution's
    // set of whitelisted email domains or addresses
    public boolean requireInstitutionalVerification;
    // Flag to indicate whether to use the new age type options in cohort builder age wizard
    public boolean enableCBAgeTypeOptions;
  }

  public static class ActionAuditConfig {
    public String logName;
  }

  public static class RdrExportConfig {
    // RDR Host to connect to to export data
    public String host;
    // Google cloud Queue name to which the task will be pushed to
    public String queueName;
    // Number of ids per task
    public Integer exportObjectsPerTask;
  }

  public static class CaptchaConfig {
    public boolean enableCaptcha;
  }
}
