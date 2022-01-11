package org.pmiops.workbench.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class representing the main workbench configuration; parsed from JSON stored in the database.
 * See {@link CacheSpringConfiguration}. This should be kept in sync with files in the config/
 * directory.
 */
public class WorkbenchConfig {

  public FireCloudConfig firecloud;
  public AuthConfig auth;
  public WgsCohortExtractionConfig wgsCohortExtraction;
  public CdrConfig cdr;
  public GoogleCloudStorageServiceConfig googleCloudStorageService;
  public GoogleDirectoryServiceConfig googleDirectoryService;
  public ServerConfig server;
  public AdminConfig admin;
  public MandrillConfig mandrill;
  public MoodleConfig moodle;
  public ZendeskConfig zendesk;
  public AccessConfig access;
  public FeatureFlagsConfig featureFlags;
  public BillingConfig billing;
  public ActionAuditConfig actionAudit;
  public RdrExportConfig rdrExport;
  public CaptchaConfig captcha;
  public ReportingConfig reporting;
  public RasConfig ras;
  public AccessRenewalConfig accessRenewal;
  public OfflineBatchConfig offlineBatch;
  public EgressAlertRemediationPolicy egressAlertRemediationPolicy;

  /** Creates a config with non-null-but-empty member variables, for use in testing. */
  public static WorkbenchConfig createEmptyConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.access = new AccessConfig();
    config.admin = new AdminConfig();
    config.auth = new AuthConfig();
    config.auth.serviceAccountApiUsers = new ArrayList<>();
    config.wgsCohortExtraction = new WgsCohortExtractionConfig();
    config.cdr = new CdrConfig();
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
    config.reporting = new ReportingConfig();
    config.ras = new RasConfig();
    config.accessRenewal = new AccessRenewalConfig();
    config.offlineBatch = new OfflineBatchConfig();
    config.egressAlertRemediationPolicy = new EgressAlertRemediationPolicy();
    return config;
  }

  // Environment config variables related to billing and the billing project buffer (which buffers
  // GCP projects, aka "billing projects" in Terra terminology).
  public static class BillingConfig {
    // This config variable seems to be unused.
    public Integer retryCount;
    // The total capacity of the GCP project buffer, per access tier. The buffering system will not
    // attempt to create any new projects in a tier when the total number of in-progress & ready
    // projects is at or above this level.
    public Map<String, Integer> bufferCapacityPerTier;
    // The number of times to attempt project creation per cron task execution. This effectively
    // controls the max rate of project refill. If the cron task is configured to run once per
    // minute and this param is set to 5, then the buffer system will create up to approximately
    // 5 projects per minute.
    //
    // Per guidance from Google Cloud's project infrastructure team, we should limit our total rate
    // of project creation to a number less than 1 per second. In practice, a reasonable aggressive
    // value for this parameter would be 5-10 project refills per minute.
    public Integer bufferRefillProjectsPerTask;
    // The number of projects whose status should be checked per cron task execution. This controls
    // the maximum rate of API calls to Terra's getBillingProjectStatus endpoint. This value has
    // little impact during normal operation, when the number of CREATING projects which need to be
    // synced is quite small, but can impact system behavior during outages and after recovery.
    //
    // A higher number ensures that projects are kept in sync more quickly, at the cost of greater
    // load on Terra's endpoints. Historically this number was hard-coded to 5, but a larger value
    // (between 10-20) significantly speeds the Workbench's recovery from an outage.
    public Integer bufferStatusChecksPerTask;
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
    // The contact email from Carahsoft for billing account setup
    public String carahsoftEmail;
  }

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String baseUrl;
    public String samBaseUrl;
    public String terraUiBaseUrl;
    public Integer notebookRuntimeMaxAgeDays;
    public Integer notebookRuntimeIdleMaxAgeDays;
    public String leoBaseUrl;
    // This value specifies the information we hand to Terra as our AppId header.
    // It is primarily used for metrics gathering information.
    public String xAppIdValue;
    // The name of the VPC service perimeter to create our Terra GCP projects inside,
    // if enabled.
    // The length of our API HTTP client timeouts to firecloud
    public Integer timeoutInSeconds;
    // The docker image that we use for our jupyter images
    public String jupyterDockerImage;
    // Base URL for the Shibboleth API server, e.g.
    // https://profile-dot-broad-shibboleth-prod.appspot.com
    // See RW-4257 for more details on Terra's Shibboleth-specific API.
    //
    // Note that the Terra Shibboleth API server does not have a distinct App Engine service for
    // dev environments. Instead, a /dev prefix should be added to this base URL.
    public String shibbolethApiBaseUrl;
    // Base URL for the Shibboleth UI service, e.g. https://broad-shibboleth-prod.appspot.com.
    // This is the base URL that a browser client should be redirected to in order to complete
    // an authentication round trip with eRA Commons.
    public String shibbolethUiBaseUrl;

    // Project containing aggregated BigQuery log sinks for Terra workspace projects. This contains
    // information such as runtime VM server logs.
    public String workspaceLogsProject;

    public RuntimeImages runtimeImages;

    // The deployment area of the GCE VM. For example, us-east1-a or europe-west2-c
    public String gceVmZone;
  }

  public static class RuntimeImages {
    public ArrayList<String> gce;
    public ArrayList<String> dataproc;
  }

  public static class AuthConfig {
    // A list of GCP service accounts (not affiliated with researchers) that can be used to
    // make API calls.
    public ArrayList<String> serviceAccountApiUsers;
  }

  public static class WgsCohortExtractionConfig {
    public String serviceAccount;
    public String serviceAccountTerraProxyGroup;
    public String operationalTerraWorkspaceNamespace;
    public String operationalTerraWorkspaceName;
    public String operationalTerraWorkspaceBucket;
    public String extractionPetServiceAccount;
    public String extractionMethodConfigurationNamespace;
    public String extractionMethodConfigurationName;
    public Integer extractionMethodConfigurationVersion;
    public String extractionCohortsDataset;
    public String extractionDestinationDataset;
    public String extractionTempTablesDataset;
    public boolean enableJiraTicketingOnFailure;
  }

  public static class CdrConfig {
    public boolean debugQueries;
  }

  public static class GoogleCloudStorageServiceConfig {
    public String credentialsBucketName;
    public String emailImagesBucketName;
  }

  public static class GoogleDirectoryServiceConfig {
    public String gSuiteDomain;
  }

  public static class ServerConfig {
    // Base URL for the App Engine API service (e.g. backend server).
    public String apiBaseUrl;
    // Base URL for the App Engine UI service (e.g. webapp / client).
    public String uiBaseUrl;
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
    public String loginUrl;
  }

  public static class MandrillConfig {
    public String fromEmail;
    public int sendRetries;
  }

  public static class MoodleConfig {
    public String host;
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
    // Indicates that the system should be allowed to re-create a DbUser row based on data from
    // GSuite OAuth and directory data. This path is used primarily as a convenience for developers
    // in their local environment. When a local database is wiped and a user logs into the Workbench
    // with their RW-test @fake-research-aou.org credentials, the AuthInterceptor will lazily call
    // createUser to allow continued Workbench access.
    public boolean unsafeAllowUserCreationFromGSuiteData;

    // These booleans control whether each of our core access modules are enabled per environment.
    public boolean enableComplianceTraining;
    public boolean enableEraCommons;
    // If true, new users are required to finish identity verification using RAS/login.gov.
    // This will be replaced with enforceRasLoginGovLinking.
    public boolean enableRasLoginGovLinking;
    // If true, all users are required to finish identity verification using RAS/login.gov
    public boolean enforceRasLoginGovLinking;
  }

  public static class FeatureFlagsConfig {
    // Allows a user to delete their own account. This is used for testing purposes so that
    // We can clean up after ourselves. This should never go to prod.
    public boolean unsafeAllowDeleteUser;
    // Flag to indicate whether to show the Event Date modifier in cohort builder
    public boolean enableEventDateModifier;
    // Flag to indicate whether to show Update research purpose prompt after an year of workspace
    // creation
    public boolean enableResearchPurposePrompt;
    // If true, enable genomic extraction functionality for datasets which have genomics data
    // associated with their CDRs.
    public boolean enableGenomicExtraction;
    // If true, the backend and UI will support gpu for standard vm
    public boolean enableGpu;
    // If true, the backend and UI will support persistent disk as the default notebook storage
    public boolean enablePersistentDisk;
    // If true, ask Leo to set dataproc worker VMs not having internet access.
    public boolean enablePrivateDataprocWorker;
    // If true, workspace owner and writers will have Google Lifesience Runner permission, and their
    // Terra Pet SA can actAs itself.
    public boolean grantLifescienceApiRunnerAcl;
    // If true, copy the support staff when sending Admin Locking emails.
    public boolean ccSupportWhenAdminLocking;
  }

  public static class ActionAuditConfig {
    // Name of the Stackdriver log for Action Audit JSON entries.
    public String logName;
    // BigQuery dataset, which is the destination of a Stackdriver sink linking the
    // log to BigQuery. Changing this field does not change the sink, but rather updates the
    // fully-qualified table name when querying the dataset.
    // Currently this is named after the log, replacing hyphens with underscores, in every
    // environment.
    // However, it's not guaranteed to follow that pattern forever, so we leave the three names
    // independently variable.
    // See https://broad.io/aou-new-environment for how to initialize the BigQuery dataset and
    // Stackdriver
    // sink.
    public String bigQueryDataset;
    // Table in the BigQuery dataset that receives log events. Currently named the same as the
    // dataset, but this could change in the future.
    public String bigQueryTable;
    // The column used to partition the action audit dataset.
    public String partitionColumn;
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
    public boolean useTestCaptcha;
  }

  public static class ReportingConfig {
    public String dataset;
    // Max rows per batch queried in MySQL, and also the upload batch size for BigQuery. Max
    // possible is 10000, though around 2500 may be the most Workspace rows we can load into memory
    // on the smallest App Engine machine.
    public Integer maxRowsPerInsert;
  }

  /** RAS(Researcher Auth Service) configurations. */
  public static class RasConfig {
    // RAS hostname
    public String host;
    // RAS client id to finish the OAuth flow.
    public String clientId;
    // The URL that can sign out RAS login session.
    public String logoutUrl;
  }

  public static class AccessRenewalConfig {
    // Days a user's module completion is good for until it expires
    public Long expiryDays;
    // Lookback period - the point when we give users the option to update their compliance items
    public Long lookbackPeriod;
    // Thresholds for sending warning emails based on approaching module expiration, in days
    public List<Long> expiryDaysWarningThresholds;
  }

  public static class OfflineBatchConfig {
    // If specified, registers an alternate Cloud Tasks handler which immediately dispatches tasks
    // against the provided host. Intended for local development only.
    public String unsafeCloudTasksForwardingHost;
    // Number of users to process within a single access audit task. This should be tuned in concert
    // with the task queue configuration to affect the overall concurrency of the offline batch
    // process.
    public Integer usersPerAuditTask;
    // Number of users to process within a single access synchronization task.
    public Integer usersPerSynchronizeAccessTask;
  }

  /**
   * One or more escalating remediation actions to take, depending on the number of observed egress
   * incidents. The highest matching escalation takes precedence. For example, with the given
   * policy: <code>
   * escalations: [
   *   {afterIncidentCount: 1, suspendCompute: {durationMinutes: 10}},
   *   {afterIncidentCount: 2, suspendCompute: {durationMinutes: 60}},
   *   {afterIncidentCount: 4, disableUser: {}}
   * ]
   * </code>
   *
   * <ul>
   *   <li>On incident 1, the user's compute is suspended for 10 minutes
   *   <li>On incident 2, the user's compute is suspended for 1 hour
   *   <li>On incident 3, the user's compute is suspended for 1 hour
   *   <li>On incident 4+, the user is disabled
   * </ul>
   *
   * Only has an effect if enableEgressAlertingV2 is enabled.
   */
  public static class EgressAlertRemediationPolicy {
    public static class Escalation {
      public static class SuspendCompute {
        public Long durationMinutes;
      }

      public static class DisableUser {}

      // This policy will take effect after this many egress incidents have occurred. The highest
      // matching incident count escalation takes precedence. Should be a positive integer.
      public Integer afterIncidentCount;

      // Exactly one of the following should be specified.
      public SuspendCompute suspendCompute;
      public DisableUser disableUser;
    }

    public String notifyFromEmail;
    public boolean enableJiraTicketing;
    public List<Escalation> escalations;
  }
}
