package org.pmiops.workbench.config;

import static org.pmiops.workbench.utils.BillingUtils.fullBillingAccountName;

import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchConfig.WgsCohortExtractionConfig.CDRv8PlusConfig;
import org.pmiops.workbench.config.WorkbenchConfig.WgsCohortExtractionConfig.LegacyWorkflowConfig;

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
  public AbsorbConfig absorb;
  public TanagraConfig tanagra;
  public ZendeskConfig zendesk;
  public AccessConfig access;
  public FeatureFlagsConfig featureFlags;
  public BillingConfig billing;
  public ActionAuditConfig actionAudit;
  public BucketAuditConfig bucketAudit;
  public RdrExportConfig rdrExport;
  public CaptchaConfig captcha;
  public ReportingConfig reporting;
  public RasConfig ras;
  public OfflineBatchConfig offlineBatch;
  public EgressAlertRemediationPolicy egressAlertRemediationPolicy;
  public E2ETestUserConfig e2eTestUsers;
  public TermsOfServiceConfig termsOfService;
  public ArtifactRegistryConfig artifactRegistry;
  public BannerConfig banner;
  public VwbConfig vwb;

  /** Creates a config with non-null-but-empty member variables, for use in testing. */
  public static WorkbenchConfig createEmptyConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.access = new AccessConfig();
    config.access.currentDuccVersions = new ArrayList<>();
    config.access.renewal = new AccessConfig.Renewal();
    config.actionAudit = new ActionAuditConfig();
    config.admin = new AdminConfig();
    config.auth = new AuthConfig();
    config.auth.serviceAccountApiUsers = new ArrayList<>();
    config.billing = new BillingConfig();
    config.captcha = new CaptchaConfig();
    config.cdr = new CdrConfig();
    config.egressAlertRemediationPolicy = new EgressAlertRemediationPolicy();
    config.featureFlags = new FeatureFlagsConfig();
    config.firecloud = new FireCloudConfig();
    config.googleCloudStorageService = new GoogleCloudStorageServiceConfig();
    config.googleDirectoryService = new GoogleDirectoryServiceConfig();
    config.mandrill = new MandrillConfig();
    config.absorb = new AbsorbConfig();
    config.offlineBatch = new OfflineBatchConfig();
    config.ras = new RasConfig();
    config.rdrExport = new RdrExportConfig();
    config.reporting = new ReportingConfig();
    config.server = new ServerConfig();
    config.tanagra = new TanagraConfig();
    config.wgsCohortExtraction = new WgsCohortExtractionConfig();
    config.wgsCohortExtraction.legacyVersions = new LegacyWorkflowConfig();
    config.wgsCohortExtraction.cdrv8plus = new CDRv8PlusConfig();
    config.zendesk = new ZendeskConfig();
    config.bucketAudit = new BucketAuditConfig();
    config.e2eTestUsers = new E2ETestUserConfig();
    config.termsOfService = new TermsOfServiceConfig();
    config.artifactRegistry = new ArtifactRegistryConfig();
    config.vwb = new VwbConfig();
    return config;
  }

  // Environment config variables related to Terra billing projects / GCP projects.
  public static class BillingConfig {
    // The environment-driven prefix to apply to Terra billing projects we create. Example:
    // "aou-rw-stable-" causes us to create projects named like "aou-rw-stable-8aec175b".
    public String projectNamePrefix;
    // The initial credits GCP billing account ID to associate with Terra / GCP projects.
    public String accountId;

    public String initialCreditsBillingAccountName() {
      return fullBillingAccountName(accountId);
    }

    // The full table name for the BigQuery billing export, which is read from by the free-tier
    // usage tracking cron endpoint.
    public String exportBigQueryTable;
    // The default dollar limit to apply to free-credit usage in this environment.
    public Double defaultFreeCreditsDollarLimit;
    // Thresholds for email alerting based on free tier usage, by cost
    public List<Double> freeTierCostAlertThresholds;
    // The contact email from Carahsoft for billing account setup
    public String carahsoftEmail;

    // The batch size used by the cron job to process users
    public Integer freeTierCronUserBatchSize;

    // The number of minutes elapsed after the last cron run to update the free tier billing
    // information
    public Integer minutesBeforeLastFreeTierJob;

    // A value that defines the number of days to consider between the last update of the Free tier
    // usage in the database and the last workspace update when calculating the eligibility of a
    // workspace free tier usage to be updated. To account for charges that may occur after the
    // workspace gets deleted and after the last cron had run
    public Long numberOfDaysToConsiderForFreeTierUsageUpdate;

    // The number of days that initial credits are valid for.
    public Long initialCreditsValidityPeriodDays;

    // The number of days that initial credits can be extended for.
    public Long initialCreditsExtensionPeriodDays;

    // The number of days before initial credits expire that a warning email should be sent.
    public Long initialCreditsExpirationWarningDays;
  }

  public static class FireCloudConfig {
    public boolean debugEndpoints;
    public String baseUrl;
    public String calhounBaseUrl;
    public String rawlsBaseUrl;
    public String samBaseUrl;
    public String terraUiBaseUrl;
    public Integer notebookRuntimeMaxAgeDays;
    public Integer notebookRuntimeIdleMaxAgeDays;
    public String leoBaseUrl;
    // This value specifies the information we hand to Terra as our AppId header.
    // It is primarily used for metrics gathering information.
    public String xAppIdValue;
    // The length of our API HTTP client timeouts to firecloud.
    public Integer timeoutInSeconds;
    // The length of our API HTTP client timeouts to firecloud for slow endpoints where we are
    // willing to tolerate a higher latency, e.g. CreateWorkspace, CloneWorkspace (as of 2022). This
    // value should be >= timeoutInSeconds. Intended for use with offline jobs only (crons, cloud
    // tasks) where we are not subject to the 60s GAE request deadline.
    public Integer lenientTimeoutInSeconds;
    // The docker image that we use for our jupyter images
    public String jupyterDockerImage;
    // Project containing aggregated BigQuery log sinks for Terra workspace projects. This contains
    // information such as runtime VM server logs.
    public String workspaceLogsProject;

    // The workspace GCS bucket location
    public String workspaceBucketLocation;

    // The available deployment areas for the GCE VM. For example, us-east1-a or europe-west2-c
    public List<String> gceVmZones;

    // The zone that is selected by default when creating a GCE VM. This must be one of the values
    // specified in gceVmZones.
    public String defaultGceVmZone;
  }

  public static class AuthConfig {
    // A list of GCP service accounts (not affiliated with researchers) that can be used to
    // make API calls.
    public List<String> serviceAccountApiUsers;
  }

  public static class WgsCohortExtractionConfig {
    public String serviceAccount;
    // used by publish_cdr_wgs() in db-cdr devstart
    public String serviceAccountTerraProxyGroup;
    public String operationalTerraWorkspaceNamespace;
    public String operationalTerraWorkspaceName;
    public String operationalTerraWorkspaceBucket;
    public String extractionDestinationDataset;
    public boolean enableJiraTicketingOnFailure;
    // This should not exceed the value of GenomicExtractionService.MAX_EXTRACTION_SCATTER.
    public int minExtractionScatterTasks;
    public float extractionScatterTasksPerSample;

    public abstract static class VersionedConfig {
      // 'method' values refer to both the stored Method and the generated Method Configuration
      public String methodNamespace;
      public String methodName;
      // This is the Agora snapshot identifier, returned when running create-terra-method-snapshot.
      // This may be different across environments, as the method configurations are independently
      // defined.
      public int methodRepoVersion;
      // This is a logical workflow version used by the RW server. This should be incremented when
      // backwards incompatible changes are introduced into the Workflow, e.g. new required inputs
      // are added.
      public int methodLogicalVersion;
    }

    // for extraction workflows compatible with CDR v7 and earlier
    public static class LegacyWorkflowConfig extends VersionedConfig {
      public String gatkJarUri;
    }

    // for extraction workflows compatible with CDR v8 and later
    public static class CDRv8PlusConfig extends VersionedConfig {}

    public LegacyWorkflowConfig legacyVersions;
    public CDRv8PlusConfig cdrv8plus;
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

    // Server that host API server assets (e.g. jupyter extensions). Assets are hosted in backend
    // servers
    // except for local server.
    // When running server locally, get assets from test backend server.
    public String apiAssetsBaseUrl;
    public String publicApiKeyForErrorReports;
    public String projectId;
    public String shortName;
    public String oauthClientId;
    public String appEngineLocationId;
    public boolean isDownForMaintenance;
  }

  public static class AdminConfig {
    public String loginUrl;
  }

  public static class MandrillConfig {
    public String fromEmail;
    public int sendRetries;
  }

  public static class AbsorbConfig {
    public String externalDepartmentId;
    public String samlIdentityProviderId;
    public String samlServiceProviderId;
  }

  public static class TanagraConfig {
    public String baseUrl;
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
    public boolean enableRasIdMeLinking;
    public boolean enableRasLoginGovLinking;
    // Which Data User Code of Conduct (DUCC) Agreement version(s) are currently accepted as valid
    public List<Integer> currentDuccVersions;

    public static class Renewal {
      // Days a user's module completion is good for until it expires
      public Long expiryDays;
      // Lookback period - the point when we give users the option to update their compliance items
      public Long lookbackPeriod;
      // Training lookback period - the point when we give users the option to take training for
      // their compliance items
      public Long trainingLookbackPeriod;
      // Thresholds for sending warning emails based on approaching module expiration, in days
      public List<Long> expiryDaysWarningThresholds;
    }

    public Renewal renewal;

    // Whether to check API request URL. If enabled, requests to API server are required to match
    // baseApiUrl.
    public boolean enableApiUrlCheck;
  }

  public static class FeatureFlagsConfig {
    // Allows a user to delete their own account. This is used for testing purposes so that
    // We can clean up after ourselves. This should never go to prod.
    public boolean unsafeAllowDeleteUser;
    // Flag to indicate whether to show the Event Date modifier in cohort builder
    public boolean enableEventDateModifier;
    // If true, ask Leo to set dataproc worker VMs not having internet access.
    public boolean enablePrivateDataprocWorker;
    // If true, copy the support staff when sending Admin Locking emails.
    public boolean ccSupportWhenAdminLocking;
    // If true, enable visual data explorer
    public boolean enableDataExplorer;
    // If true, allow users to pause their GKE apps
    public boolean enableGKEAppPausing;
    // If true, allows users to set machine type for their GKE apps
    public boolean enableGKEAppMachineTypeChoice;
    // If true, enable mounting GCS buckets on GKE apps
    public boolean enableGcsFuseOnGke;
    public boolean enableInitialCreditsExpiration;
    // If true, prevents users from taking compliance training, however
    // the training is still required if enableComplianceTraining is true.
    public boolean blockComplianceTraining;

    // If true, users will be able to create workspace in VWB
    public boolean enableVWBWorkspaceCreation;
    // If true, AoU API will start accepting egress notification coming from VWB Service
    public boolean enableVWBEgressMonitor;
    // If true, AoU will call SAM and VWB to add/remove user from VWB tier group.
    public boolean enableVWBUserAccessManagement;
    // If true, AoU will create the VWB user when the user first signs in, similar to Terra
    // experience
    public boolean enableVWBUserCreation;
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

  public static class BucketAuditConfig {
    public String logProjectId;
    public String bigQueryDataset;
    public String bigQueryTable;
  }

  public static class RdrExportConfig {
    // RDR Host to connect to for exporting data
    public String host;
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

    // Whether to export Tera Data Warehouse into reporting BQ. This should be enabled on dev and
    // prod
    public Boolean exportTerraDataWarehouse;
    // Terra Data Warehouse leonardo_app BQ table id.
    public String terraWarehouseLeoAppTableId;
    // Terra Data Warehouse leonardo_app_usage BQ table id.
    public String terraWarehouseLeoAppUsageTableId;
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
    // Number of users to process within a single check initial credits expiration task.
    public Integer usersPerCheckInitialCreditsExpirationTask;
    // Number of users to process within a single access expiration email task.
    public Integer usersPerAccessExpirationEmailTask;
    // Number of workspaces to process within a single delete workspace environments task.
    public Integer workspacesPerDeleteWorkspaceEnvironmentsTask;
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
    public List<String> notifyCcEmails;
    public boolean enableJiraTicketing;
    public List<Escalation> escalations;
  }

  public static class E2ETestUserConfig {
    public int workspaceDeletionBatchSize;
    public List<String> testUserEmails;
  }

  // Configs for AoU and Terra Terms of Service
  public static class TermsOfServiceConfig {
    // the minimum All of Us Terms of Service version which is considered current (newer is OK too)
    public int minimumAcceptedAouVersion;
  }

  public static class ArtifactRegistryConfig {
    public String dockerRepoName;
  }

  public static class BannerConfig {
    public boolean enableLoginIssueBanner;
  }

  public static class VwbConfig {
    public String wsmBaseUrl;
    public String vwbSamBaseUrl;
    public String exfilManagerBaseUrl;
    public String organizationId;
    // This will only be used for preview release, later on, each user will have their own pod.
    public String defaultPodId;
    // The service account that will be calling AoU API to notify egress alerts. It will be used in
    // later authZ check.
    public String exfilManagerServiceAccount;
    public String userManagerBaseUrl;
  }
}
