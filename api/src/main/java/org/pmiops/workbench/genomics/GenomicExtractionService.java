package org.pmiops.workbench.genomics;

import com.google.cloud.storage.Blob;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.WgsCohortExtractionConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionRequest;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowOutputs;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowOutputsResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.jira.JiraContent;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.jira.JiraService.IssueProperty;
import org.pmiops.workbench.jira.JiraService.IssueType;
import org.pmiops.workbench.jira.model.AtlassianContent;
import org.pmiops.workbench.jira.model.CreatedIssue;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class GenomicExtractionService {
  private static final Logger log = Logger.getLogger(GenomicExtractionService.class.getName());

  public static final String EXTRACT_WORKFLOW_NAME = "GvsExtractCohortFromSampleNames";
  // Theoretical maximum is 20K-30K, keep it lower during the initial alpha period.
  private static final int MAX_EXTRACTION_SAMPLE_COUNT = 5_000;
  // Scatter count maximum for extraction. Affects number of workers and numbers of shards.
  private static final int MAX_EXTRACTION_SCATTER = 2_000;

  private final DataSetService dataSetService;
  private final FireCloudService fireCloudService;
  private final JiraService jiraService;
  private final Provider<CloudStorageClient> extractionServiceAccountCloudStorageClientProvider;
  private final Provider<SubmissionsApi> submissionApiProvider;
  private final Provider<MethodConfigurationsApi> methodConfigurationsApiProvider;
  private final WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao;
  private final GenomicExtractionMapper genomicExtractionMapper;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final Clock clock;

  @Autowired
  public GenomicExtractionService(
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      JiraService jiraService,
      @Qualifier(StorageConfig.GENOMIC_EXTRACTION_STORAGE_CLIENT)
          Provider<CloudStorageClient> extractionServiceAccountCloudStorageClientProvider,
      Provider<SubmissionsApi> submissionsApiProvider,
      Provider<MethodConfigurationsApi> methodConfigurationsApiProvider,
      WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao,
      GenomicExtractionMapper genomicExtractionMapper,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceAuthService workspaceAuthService,
      Clock clock) {
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.jiraService = jiraService;
    this.submissionApiProvider = submissionsApiProvider;
    this.extractionServiceAccountCloudStorageClientProvider =
        extractionServiceAccountCloudStorageClientProvider;
    this.methodConfigurationsApiProvider = methodConfigurationsApiProvider;
    this.wgsExtractCromwellSubmissionDao = wgsExtractCromwellSubmissionDao;
    this.genomicExtractionMapper = genomicExtractionMapper;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.clock = clock;
  }

  private Map<String, String> createRepoMethodParameter(
      WgsCohortExtractionConfig cohortExtractionConfig) {
    return new ImmutableMap.Builder<String, String>()
        .put("methodName", cohortExtractionConfig.extractionMethodConfigurationName)
        .put(
            "methodVersion", cohortExtractionConfig.extractionMethodConfigurationVersion.toString())
        .put("methodNamespace", cohortExtractionConfig.extractionMethodConfigurationNamespace)
        .put(
            "methodUri",
            "agora://"
                + cohortExtractionConfig.extractionMethodConfigurationNamespace
                + "/"
                + cohortExtractionConfig.extractionMethodConfigurationName
                + "/"
                + cohortExtractionConfig.extractionMethodConfigurationVersion)
        .put("sourceRepo", "agora")
        .build();
  }

  private boolean isTerminal(TerraJobStatus status) {
    return !(status == TerraJobStatus.RUNNING || status == TerraJobStatus.ABORTING);
  }

  public List<GenomicExtractionJob> getGenomicExtractionJobs(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return wgsExtractCromwellSubmissionDao.findAllByWorkspace(dbWorkspace).stream()
        .map(
            dbSubmission -> {
              try {
                // Don't bother checking if we already know the job is in a terminal status.
                if (dbSubmission.getTerraStatusEnum() == null
                    || !isTerminal(dbSubmission.getTerraStatusEnum())) {
                  WgsCohortExtractionConfig cohortExtractionConfig =
                      workbenchConfigProvider.get().wgsCohortExtraction;
                  FirecloudSubmission firecloudSubmission =
                      submissionApiProvider
                          .get()
                          .getSubmission(
                              cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                              cohortExtractionConfig.operationalTerraWorkspaceName,
                              dbSubmission.getSubmissionId());

                  TerraJobStatus oldStatus = dbSubmission.getTerraStatusEnum();
                  TerraJobStatus status =
                      genomicExtractionMapper.convertWorkflowStatus(
                          // Extraction submissions should only have one workflow.
                          firecloudSubmission.getWorkflows().get(0).getStatus());
                  dbSubmission.setTerraStatusEnum(status);

                  if (TerraJobStatus.SUCCEEDED.equals(status)) {
                    dbSubmission.setVcfSizeMb(getWorkflowSize(firecloudSubmission));
                  }

                  if (isTerminal(status)) {
                    dbSubmission.setCompletionTime(
                        CommonMappers.timestamp(
                            firecloudSubmission.getWorkflows().get(0).getStatusLastChangedDate()));
                  }

                  if (TerraJobStatus.FAILED.equals(status) && !status.equals(oldStatus)) {
                    maybeNotifyOnJobFailure(dbSubmission, firecloudSubmission);
                  }

                  wgsExtractCromwellSubmissionDao.save(dbSubmission);
                }
                return genomicExtractionMapper.toApi(dbSubmission);
              } catch (ApiException e) {
                throw new ServerErrorException("Could not fetch submission status from Terra", e);
              }
            })
        .collect(Collectors.toList());
  }

  private Long getWorkflowSize(FirecloudSubmission firecloudSubmission) throws ApiException {
    final FirecloudWorkflowOutputsResponse outputsResponse =
        submissionApiProvider
            .get()
            .getWorkflowOutputs(
                workbenchConfigProvider.get()
                    .wgsCohortExtraction
                    .operationalTerraWorkspaceNamespace,
                workbenchConfigProvider.get().wgsCohortExtraction.operationalTerraWorkspaceName,
                firecloudSubmission.getSubmissionId(),
                firecloudSubmission.getWorkflows().get(0).getWorkflowId());

    final Optional<FirecloudWorkflowOutputs> workflowOutputs =
        Optional.ofNullable(outputsResponse.getTasks().get(EXTRACT_WORKFLOW_NAME));

    if (workflowOutputs.isPresent()) {
      final Optional<Object> vcfSizeMbOutput =
          Optional.ofNullable(
              workflowOutputs
                  .get()
                  .getOutputs()
                  .get(EXTRACT_WORKFLOW_NAME + ".total_vcfs_size_mb"));

      if (vcfSizeMbOutput.isPresent() && vcfSizeMbOutput.get() instanceof Double) {
        return Math.round((Double) vcfSizeMbOutput.get());
      }
    }

    return null;
  }

  private void maybeNotifyOnJobFailure(
      DbWgsExtractCromwellSubmission dbSubmission, FirecloudSubmission firecloudSubmission) {
    log.severe(
        String.format(
            "genomics extraction workflow failed: '%s'",
            getFailureCauses(firecloudSubmission).stream().collect(Collectors.joining(","))));
    if (!workbenchConfigProvider.get().wgsCohortExtraction.enableJiraTicketingOnFailure) {
      return;
    }

    String envShortName = workbenchConfigProvider.get().server.shortName;
    try {
      CreatedIssue createdIssue =
          jiraService.createIssue(
              IssueType.BUG,
              JiraContent.contentAsMinimalAtlassianDocument(
                  jiraFailureDescription(dbSubmission, firecloudSubmission)),
              ImmutableMap.<IssueProperty, Object>builder()
                  .put(
                      IssueProperty.SUMMARY,
                      String.format(
                          "[P2] %s genomic extraction %s failed @ %s",
                          envShortName,
                          dbSubmission.getSubmissionId(),
                          JiraService.summaryDateFormat.format(clock.instant())))
                  .put(IssueProperty.RW_ENVIRONMENT, envShortName)
                  .put(IssueProperty.LABELS, new String[] {"genomic-extraction-failure"})
                  .build());
      log.info("created new egress Jira ticket: " + createdIssue.getKey());
    } catch (org.pmiops.workbench.jira.ApiException e) {
      log.log(
          Level.SEVERE, "failed to file Jira ticket for failed genomic extraction, continuing", e);
    }
  }

  private Stream<AtlassianContent> jiraFailureDescription(
      DbWgsExtractCromwellSubmission dbSubmission, FirecloudSubmission firecloudSubmission) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    Duration runtime =
        Duration.between(
            dbSubmission.getTerraSubmissionDate().toInstant(),
            dbSubmission.getCompletionTime().toInstant());
    return Stream.of(
        JiraContent.text(String.format("Terra job details (as pmi-ops.org user):\n")),
        JiraContent.link(
            String.format(
                "%s#workspaces/%s/%s/job_history/%s",
                workbenchConfig.firecloud.terraUiBaseUrl,
                workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace,
                workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName,
                dbSubmission.getSubmissionId())),
        JiraContent.text(
            String.format(
                "\n\nCromwell workflow submission ID: %s\n", dbSubmission.getSubmissionId())),
        JiraContent.text(
            String.format(
                "Workbench extraction database ID: %d\n",
                dbSubmission.getWgsExtractCromwellSubmissionId())),
        JiraContent.text(
            String.format(
                "Failure occurred @ %s (runtime: %dm)\n",
                JiraService.detailedDateFormat.format(dbSubmission.getCompletionTime().toInstant()),
                runtime.toMinutes())),
        JiraContent.text(
            String.format(
                "User running extraction: %s\n", dbSubmission.getCreator().getUsername())),
        JiraContent.text(
            String.format(
                "Terra billing project / workspace namespace: %s\n",
                dbSubmission.getWorkspace().getWorkspaceNamespace())),
        JiraContent.text(
            String.format(
                "Google project ID: %s\n\n", dbSubmission.getWorkspace().getGoogleProject())),
        JiraContent.text("Workspace admin console (as RW admin): "),
        JiraContent.link(
            workbenchConfig.server.uiBaseUrl
                + "/admin/workspaces/"
                + dbSubmission.getWorkspace().getWorkspaceNamespace()),
        JiraContent.text(
            String.format(
                "\nWorkflow failure messages:\n%s",
                getFailureCauses(firecloudSubmission).stream()
                    .map(m -> "* " + m + "\n")
                    .collect(Collectors.joining()))));
  }

  private List<String> getFailureCauses(FirecloudSubmission firecloudSubmission) {
    return Optional.ofNullable(firecloudSubmission.getWorkflows())
        .filter(wfs -> !wfs.isEmpty())
        .map(wfs -> wfs.get(0))
        .map(wf -> wf.getMessages())
        .orElse(ImmutableList.of("unknown cause"));
  }

  public GenomicExtractionJob submitGenomicExtractionJob(DbWorkspace workspace, DbDataset dataSet)
      throws ApiException {
    WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    RawlsWorkspaceDetails fcUserWorkspace =
        fireCloudService.getWorkspace(workspace).get().getWorkspace();

    String extractionUuid = UUID.randomUUID().toString();
    String extractionFolder = "genomic-extractions/" + extractionUuid;

    List<String> personIds = dataSetService.getPersonIdsWithWholeGenome(dataSet);
    if (personIds.isEmpty()) {
      throw new FailedPreconditionException(
          "provided cohort contains no participants with whole genome data");
    }
    if (personIds.size() > MAX_EXTRACTION_SAMPLE_COUNT) {
      throw new FailedPreconditionException(
          String.format(
              "provided dataset contains %d individuals with whole genome data, the current limit "
                  + "for extraction is %d",
              personIds.size(), MAX_EXTRACTION_SAMPLE_COUNT));
    }

    Blob personIdsFile =
        extractionServiceAccountCloudStorageClientProvider
            .get()
            .writeFile(
                // It is critical that this file is written to a bucket that the user cannot write
                // to because its contents will feed into a SQL query with the cohort
                // extraction SA's permissions
                cohortExtractionConfig.operationalTerraWorkspaceBucket,
                extractionFolder + "/person_ids.txt",
                String.join("\n", personIds).getBytes(StandardCharsets.UTF_8));

    final String outputDir =
        "gs://" + fcUserWorkspace.getBucketName() + "/" + extractionFolder + "/vcfs/";

    // Initial heuristic for scatter count, optimizing to avoid large compute/output shards while
    // keeping overhead low and limiting footprint on shared extraction quota.
    int minScatter =
        Math.min(cohortExtractionConfig.minExtractionScatterTasks, MAX_EXTRACTION_SCATTER);
    int scatter =
        Ints.constrainToRange(
            Math.round(personIds.size() * cohortExtractionConfig.extractionScatterTasksPerSample),
            minScatter,
            MAX_EXTRACTION_SCATTER);

    Map<String, String> maybeInputs = new HashMap<>();
    int methodLogicalVersion =
        Optional.ofNullable(cohortExtractionConfig.extractionMethodLogicalVersion).orElse(0);
    if (methodLogicalVersion < 3) {
      log.severe("unsupported GVS extract method version: " + methodLogicalVersion);
      throw new ServerErrorException();
    }

    String filterSetName = workspace.getCdrVersion().getWgsFilterSetName();
    if (!Strings.isNullOrEmpty(filterSetName)) {
      // If set, apply a joint callset filter during the extraction. There may be multiple such
      // filters defined within a GVS BigQuery dataset (see the filter_set table to view options).
      // Typically, we will want to specify a filter set.
      maybeInputs.put(EXTRACT_WORKFLOW_NAME + ".filter_set_name", "\"" + filterSetName + "\"");
    }

    String[] destinationParts = cohortExtractionConfig.extractionDestinationDataset.split("\\.");
    if (destinationParts.length != 2) {
      log.severe(
          "bad config value for destination BigQuery dataset: "
              + cohortExtractionConfig.extractionDestinationDataset);
      throw new ServerErrorException();
    }

    FirecloudMethodConfiguration methodConfig =
        methodConfigurationsApiProvider
            .get()
            .createWorkspaceMethodConfig(
                new FirecloudMethodConfiguration()
                    .inputs(
                        new ImmutableMap.Builder<String, String>()
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".cohort_sample_names",
                                "\"gs://" // Cromwell string inputs require double quotes
                                    + personIdsFile.getBucket()
                                    + "/"
                                    + personIdsFile.getName()
                                    + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".query_project",
                                "\"" + workspace.getGoogleProject() + "\"")
                            // Added in https://github.com/broadinstitute/gatk/pull/7698
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".cohort_table_prefix",
                                "\"" + extractionUuid + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".destination_project_id",
                                "\"" + destinationParts[0] + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".destination_dataset_name",
                                "\"" + destinationParts[1] + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".extraction_uuid",
                                "\"" + extractionUuid + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".gvs_project",
                                "\"" + workspace.getCdrVersion().getBigqueryProject() + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".gvs_dataset",
                                "\"" + workspace.getCdrVersion().getWgsBigqueryDataset() + "\"")
                            // This value will need to be dynamically adjusted through testing
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".scatter_count", Integer.toString(scatter))
                            // Will produce files named "interval_1.vcf.gz", "interval_32.vcf.gz",
                            // etc
                            .put(EXTRACT_WORKFLOW_NAME + ".output_file_base_name", "\"interval\"")
                            .put(EXTRACT_WORKFLOW_NAME + ".output_gcs_dir", "\"" + outputDir + "\"")
                            .put(
                                EXTRACT_WORKFLOW_NAME + ".gatk_override",
                                "\"" + cohortExtractionConfig.gatkJarUri + "\"")
                            .putAll(maybeInputs)
                            .build())
                    .methodConfigVersion(
                        cohortExtractionConfig.extractionMethodConfigurationVersion)
                    .methodRepoMethod(createRepoMethodParameter(cohortExtractionConfig))
                    .name(extractionUuid)
                    .namespace(cohortExtractionConfig.extractionMethodConfigurationNamespace)
                    .outputs(new HashMap<>()),
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName)
            .getMethodConfiguration();

    FirecloudSubmissionResponse submissionResponse =
        submissionApiProvider
            .get()
            .createSubmission(
                new FirecloudSubmissionRequest()
                    .deleteIntermediateOutputFiles(true)
                    .methodConfigurationNamespace(methodConfig.getNamespace())
                    .methodConfigurationName(methodConfig.getName())
                    .useCallCache(false),
                cohortExtractionConfig.operationalTerraWorkspaceNamespace,
                cohortExtractionConfig.operationalTerraWorkspaceName);

    // Note: if this save fails we may have an orphaned job. Will likely need a cleanup task to
    // check for such jobs.
    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setSubmissionId(submissionResponse.getSubmissionId());
    dbSubmission.setWorkspace(workspace);
    dbSubmission.setDataset(dataSet);
    dbSubmission.setCreator(userProvider.get());
    dbSubmission.setCreationTime(new Timestamp(clock.instant().toEpochMilli()));
    dbSubmission.setTerraSubmissionDate(
        CommonMappers.timestamp(submissionResponse.getSubmissionDate()));
    dbSubmission.setTerraStatusEnum(TerraJobStatus.RUNNING);
    dbSubmission.setSampleCount((long) personIds.size());
    dbSubmission.setOutputDir(outputDir);
    dbSubmission.setTerraStatusEnum(TerraJobStatus.RUNNING);
    wgsExtractCromwellSubmissionDao.save(dbSubmission);

    methodConfigurationsApiProvider
        .get()
        .deleteWorkspaceMethodConfig(
            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
            cohortExtractionConfig.operationalTerraWorkspaceName,
            cohortExtractionConfig.extractionMethodConfigurationNamespace,
            methodConfig.getName());

    return genomicExtractionMapper.toApi(dbSubmission);
  }

  public void abortGenomicExtractionJob(DbWorkspace dbWorkspace, String jobId) throws ApiException {
    DbWgsExtractCromwellSubmission dbSubmission =
        wgsExtractCromwellSubmissionDao
            .findByWorkspaceWorkspaceIdAndWgsExtractCromwellSubmissionId(
                dbWorkspace.getWorkspaceId(), Long.valueOf(jobId))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Specified dataset is not in workspace " + dbWorkspace.getName()));

    WgsCohortExtractionConfig cohortExtractionConfig =
        workbenchConfigProvider.get().wgsCohortExtraction;

    submissionApiProvider
        .get()
        .abortSubmission(
            cohortExtractionConfig.operationalTerraWorkspaceNamespace,
            cohortExtractionConfig.operationalTerraWorkspaceName,
            dbSubmission.getSubmissionId());
  }
}
