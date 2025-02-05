package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.genomics.GenomicExtractionService.EXTRACT_WORKFLOW_NAME;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.VwbAccessService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.GenomicDatasetService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.MethodConfigurationsApi;
import org.pmiops.workbench.firecloud.api.SubmissionsApi;
import org.pmiops.workbench.firecloud.model.FirecloudMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudSubmission;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionResponse;
import org.pmiops.workbench.firecloud.model.FirecloudSubmissionStatus;
import org.pmiops.workbench.firecloud.model.FirecloudValidatedMethodConfiguration;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflow;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowOutputs;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowOutputsResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkflowStatus;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.jira.model.CreatedIssue;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.TanagraGenomicDataRequest;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GenomicExtractionServiceTest {

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private static final String FC_SUBMISSION_ID = "123";

  @Autowired GenomicExtractionService genomicExtractionService;

  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired DataSetDao dataSetDao;
  @Autowired UserDao userDao;
  @Autowired WgsExtractCromwellSubmissionDao wgsExtractCromwellSubmissionDao;
  @Autowired WorkspaceDao workspaceDao;

  @MockBean FireCloudService mockFireCloudService;
  @MockBean VwbAccessService mockVwbAccessService;
  @MockBean GenomicDatasetService mockGenomicDatasetService;
  @MockBean JiraService mockJiraService;
  @MockBean MethodConfigurationsApi mockMethodConfigurationsApi;
  @MockBean SubmissionsApi mockSubmissionsApi;

  private DbWorkspace targetWorkspace;

  private static CloudStorageClient cloudStorageClient;
  private static WorkbenchConfig workbenchConfig;
  private static DbUser currentUser;
  private static DbDataset dataset;

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    CommonMappers.class,
    GenomicExtractionMapperImpl.class,
    GenomicExtractionService.class,
    WorkspaceAuthService.class,
  })
  @MockBean({InitialCreditsService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Qualifier(StorageConfig.GENOMIC_EXTRACTION_STORAGE_CLIENT)
    CloudStorageClient cloudStorageClient() {
      return cloudStorageClient;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    cloudStorageClient = mock(CloudStorageClient.class);
    Blob blob = mock(Blob.class);
    doReturn("bucket").when(blob).getBucket();
    doReturn("filename").when(blob).getName();
    doReturn(blob).when(cloudStorageClient).writeFile(any(), any(), any());
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.server.uiBaseUrl = "https://workbench.researchallofus.org";
    workbenchConfig.server.shortName = "test";
    workbenchConfig.firecloud.terraUiBaseUrl = "https://app.terra.bio";
    workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceBucket = "terraBucket";
    workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace =
        "operationalTerraWorkspaceNamespace";
    workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName =
        "operationalTerraWorkspaceName";
    workbenchConfig.wgsCohortExtraction.extractionDestinationDataset = "extract-proj.extract-ds";
    workbenchConfig.wgsCohortExtraction.minExtractionScatterTasks = 100;
    workbenchConfig.wgsCohortExtraction.extractionScatterTasksPerSample = 4;
    workbenchConfig.wgsCohortExtraction.legacyVersions.methodName = "methodName";
    workbenchConfig.wgsCohortExtraction.legacyVersions.methodNamespace = "methodNamespace";
    workbenchConfig.wgsCohortExtraction.legacyVersions.methodRepoVersion = 10;
    workbenchConfig.wgsCohortExtraction.legacyVersions.methodLogicalVersion = 3;
    workbenchConfig.wgsCohortExtraction.cdrv8plus.methodName = "v8MethodName";
    workbenchConfig.wgsCohortExtraction.cdrv8plus.methodNamespace = "v8MethodNamespace";
    workbenchConfig.wgsCohortExtraction.cdrv8plus.methodRepoVersion = 1;
    workbenchConfig.wgsCohortExtraction.cdrv8plus.methodLogicalVersion = 1;

    RawlsWorkspaceDetails fcWorkspace = new RawlsWorkspaceDetails().bucketName("user-bucket");
    RawlsWorkspaceResponse fcWorkspaceResponse =
        new RawlsWorkspaceResponse().workspace(fcWorkspace);
    doReturn(Optional.of(fcWorkspaceResponse)).when(mockFireCloudService).getWorkspace(any());
    currentUser = createUser("a@fake-research-aou.org");

    FirecloudMethodConfiguration firecloudMethodConfiguration = new FirecloudMethodConfiguration();
    firecloudMethodConfiguration.setNamespace("methodNamespace");
    firecloudMethodConfiguration.setName("methodName");

    FirecloudValidatedMethodConfiguration validatedMethodConfiguration =
        new FirecloudValidatedMethodConfiguration();
    validatedMethodConfiguration.setMethodConfiguration(firecloudMethodConfiguration);
    doReturn(validatedMethodConfiguration)
        .when(mockMethodConfigurationsApi)
        .createWorkspaceMethodConfig(any(), any(), any());

    DbCdrVersion cdrVersion =
        cdrVersionDao.save(
            new DbCdrVersion()
                .setCdrVersionId(7)
                .setBigqueryProject("bigquery_project")
                .setWgsBigqueryDataset("wgs_dataset"));

    targetWorkspace =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceId(2)
                .setWorkspaceNamespace("target-ws-namespace")
                .setFirecloudName("target-ws-fc-name")
                .setName("target-ws-name")
                .setCdrVersion(cdrVersion));

    FirecloudSubmissionResponse submissionResponse = new FirecloudSubmissionResponse();
    submissionResponse.setSubmissionId(FC_SUBMISSION_ID);
    submissionResponse.setSubmissionDate(
        CommonMappers.offsetDateTimeUtc(new Timestamp(CLOCK.instant().toEpochMilli())));
    doReturn(submissionResponse).when(mockSubmissionsApi).createSubmission(any(), any(), any());

    doReturn(new RawlsWorkspaceResponse().accessLevel(RawlsWorkspaceAccessLevel.READER))
        .when(mockFireCloudService)
        .getWorkspace(anyString(), anyString());

    doReturn(new CreatedIssue().key("RW-123"))
        .when(mockJiraService)
        .createIssue(any(), any(), any());

    dataset = createDataset();
  }

  public void mockGetFirecloudSubmission(FirecloudSubmission submission) throws ApiException {
    doReturn(submission)
        .when(mockSubmissionsApi)
        .getSubmission(
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace,
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName,
            submission.getSubmissionId());
  }

  @Test
  public void getExtractionJobs() throws ApiException {
    OffsetDateTime submissionDate = OffsetDateTime.now();
    DbWgsExtractCromwellSubmission dbWgsExtractCromwellSubmission =
        createDbWgsExtractCromwellSubmission();
    dbWgsExtractCromwellSubmission.setUserCost(new BigDecimal("2.05"));
    wgsExtractCromwellSubmissionDao.save(dbWgsExtractCromwellSubmission);

    OffsetDateTime completionTimestamp = submissionDate.plusSeconds(127313);

    FirecloudSubmission submission =
        new FirecloudSubmission()
            .submissionId(dbWgsExtractCromwellSubmission.getSubmissionId())
            .status(FirecloudSubmissionStatus.DONE)
            .addWorkflowsItem(
                new FirecloudWorkflow()
                    .statusLastChangedDate(completionTimestamp)
                    .status(FirecloudWorkflowStatus.SUCCEEDED))
            .submissionDate(submissionDate);
    mockGetFirecloudSubmission(submission);
    mockWorkflowOutputVcfSize(submission, 12345.0);

    GenomicExtractionJob wgsCohortExtractionJob =
        genomicExtractionService
            .getGenomicExtractionJobs(
                targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName())
            .get(0);

    assertThat(wgsCohortExtractionJob.getCost()).isEqualTo(new BigDecimal("2.05"));
    assertThat(wgsCohortExtractionJob.getCompletionTime())
        .isEqualTo(completionTimestamp.toInstant().toEpochMilli());
    assertThat(wgsCohortExtractionJob.getDatasetName()).isEqualTo(dataset.getName());
    assertThat(wgsCohortExtractionJob.getStatus()).isEqualTo(TerraJobStatus.SUCCEEDED);
  }

  @Test
  public void getExtractionJobs_userHasReaderWorkspaceAccess() throws ApiException {
    final String submissionId = UUID.randomUUID().toString();
    DbWgsExtractCromwellSubmission dbWgsExtractCromwellSubmission =
        new DbWgsExtractCromwellSubmission();
    dbWgsExtractCromwellSubmission.setSubmissionId(submissionId);
    dbWgsExtractCromwellSubmission.setCreator(currentUser);
    dbWgsExtractCromwellSubmission.setWorkspace(targetWorkspace);
    wgsExtractCromwellSubmissionDao.save(dbWgsExtractCromwellSubmission);

    mockGetFirecloudSubmission(
        new FirecloudSubmission()
            .submissionId(dbWgsExtractCromwellSubmission.getSubmissionId())
            .status(FirecloudSubmissionStatus.DONE)
            .addWorkflowsItem(new FirecloudWorkflow().statusLastChangedDate(OffsetDateTime.now()))
            .submissionDate(OffsetDateTime.now()));

    doReturn(new RawlsWorkspaceResponse().accessLevel(RawlsWorkspaceAccessLevel.NO_ACCESS))
        .when(mockFireCloudService)
        .getWorkspace(targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName());

    assertThrows(
        ForbiddenException.class,
        () -> {
          genomicExtractionService.getGenomicExtractionJobs(
              targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName());
        });

    doReturn(new RawlsWorkspaceResponse().accessLevel(RawlsWorkspaceAccessLevel.READER))
        .when(mockFireCloudService)
        .getWorkspace(targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName());
    genomicExtractionService.getGenomicExtractionJobs(
        targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName());
  }

  @Test
  public void getExtractionJobs_status() throws ApiException {
    Map<Long, TerraJobStatus> expectedStatuses = new HashMap<>();

    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.DONE, FirecloudWorkflowStatus.SUCCEEDED)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.SUCCEEDED);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.DONE, FirecloudWorkflowStatus.FAILED)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.FAILED);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.ABORTED, FirecloudWorkflowStatus.ABORTED)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.ABORTED);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.ABORTING, FirecloudWorkflowStatus.ABORTING)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.ABORTING);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.ACCEPTED, FirecloudWorkflowStatus.QUEUED)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.RUNNING);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.EVALUATING, FirecloudWorkflowStatus.RUNNING)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.RUNNING);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.SUBMITTED, FirecloudWorkflowStatus.SUBMITTED)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.RUNNING);
    expectedStatuses.put(
        createSubmissionAndMockMonitorCall(
                FirecloudSubmissionStatus.SUBMITTING, FirecloudWorkflowStatus.LAUNCHING)
            .getWgsExtractCromwellSubmissionId(),
        TerraJobStatus.RUNNING);

    genomicExtractionService
        .getGenomicExtractionJobs(
            targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName())
        .forEach(
            job -> {
              assertThat(job.getStatus())
                  .isEqualTo(expectedStatuses.get(job.getGenomicExtractionJobId()));
            });
  }

  @Test
  public void getExtractionJobs_saveSize() throws ApiException {
    Double expectedVcfSize = 54321.0;

    DbWgsExtractCromwellSubmission dbSubmission =
        createSubmissionAndMockMonitorCall(
            FirecloudSubmissionStatus.DONE, FirecloudWorkflowStatus.SUCCEEDED, expectedVcfSize);

    assertThat(dbSubmission.getVcfSizeMb()).isNull();

    genomicExtractionService.getGenomicExtractionJobs(
        targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName());

    assertThat(dbSubmission.getVcfSizeMb()).isEqualTo(expectedVcfSize.longValue());
  }

  @Test
  public void getExtractionJobs_reportJiraTicketOnFailure() throws Exception {
    workbenchConfig.wgsCohortExtraction.enableJiraTicketingOnFailure = true;
    DbWgsExtractCromwellSubmission dbSubmission =
        createSubmissionAndMockMonitorCall(
            FirecloudSubmissionStatus.DONE, FirecloudWorkflowStatus.FAILED);

    genomicExtractionService.getGenomicExtractionJobs(
        targetWorkspace.getWorkspaceNamespace(), targetWorkspace.getFirecloudName());

    verify(mockJiraService).createIssue(any(), any(), any());
  }

  private DbWgsExtractCromwellSubmission createDbWgsExtractCromwellSubmission() {
    DbWgsExtractCromwellSubmission dbWgsExtractCromwellSubmission =
        new DbWgsExtractCromwellSubmission();
    dbWgsExtractCromwellSubmission.setDataset(dataset);
    dbWgsExtractCromwellSubmission.setSubmissionId(UUID.randomUUID().toString());
    dbWgsExtractCromwellSubmission.setCreator(currentUser);
    dbWgsExtractCromwellSubmission.setWorkspace(targetWorkspace);
    dbWgsExtractCromwellSubmission.setTerraSubmissionDate(Timestamp.from(CLOCK.instant()));
    wgsExtractCromwellSubmissionDao.save(dbWgsExtractCromwellSubmission);

    return dbWgsExtractCromwellSubmission;
  }

  private DbWgsExtractCromwellSubmission createSubmissionAndMockMonitorCall(
      FirecloudSubmissionStatus submissionStatus,
      FirecloudWorkflowStatus workflowStatus,
      Double vcfSize)
      throws ApiException {
    DbWgsExtractCromwellSubmission dbWgsExtractCromwellSubmission =
        createDbWgsExtractCromwellSubmission();
    FirecloudSubmission firecloudSubmission =
        new FirecloudSubmission()
            .submissionId(dbWgsExtractCromwellSubmission.getSubmissionId())
            .addWorkflowsItem(
                new FirecloudWorkflow()
                    .workflowId(UUID.randomUUID().toString())
                    .statusLastChangedDate(OffsetDateTime.now())
                    .status(workflowStatus))
            .status(submissionStatus)
            .submissionDate(OffsetDateTime.now());

    doReturn(firecloudSubmission)
        .when(mockSubmissionsApi)
        .getSubmission(
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace,
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName,
            dbWgsExtractCromwellSubmission.getSubmissionId());

    mockWorkflowOutputVcfSize(firecloudSubmission, vcfSize);

    return dbWgsExtractCromwellSubmission;
  }

  private void mockWorkflowOutputVcfSize(FirecloudSubmission submission, Double vcfSize)
      throws ApiException {
    doReturn(
            new FirecloudWorkflowOutputsResponse()
                .tasks(
                    ImmutableMap.of(
                        EXTRACT_WORKFLOW_NAME,
                        new FirecloudWorkflowOutputs()
                            .outputs(
                                ImmutableMap.of(
                                    EXTRACT_WORKFLOW_NAME + ".total_vcfs_size_mb", vcfSize)))))
        .when(mockSubmissionsApi)
        .getWorkflowOutputs(
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace,
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName,
            submission.getSubmissionId(),
            submission.getWorkflows().get(0).getWorkflowId());
  }

  private DbWgsExtractCromwellSubmission createSubmissionAndMockMonitorCall(
      FirecloudSubmissionStatus submissionStatus, FirecloudWorkflowStatus workflowStatus)
      throws ApiException {
    return createSubmissionAndMockMonitorCall(submissionStatus, workflowStatus, 12345.0);
  }

  @Test
  public void submitExtractionJob() throws ApiException {
    when(mockGenomicDatasetService.getPersonIdsWithWholeGenome(any()))
        .thenReturn(List.of("1", "2", "3"));
    TanagraGenomicDataRequest tanagraRequest = null;
    genomicExtractionService.submitGenomicExtractionJob(targetWorkspace, dataset, tanagraRequest);

    ArgumentCaptor<FirecloudMethodConfiguration> argument =
        ArgumentCaptor.forClass(FirecloudMethodConfiguration.class);

    verify(mockMethodConfigurationsApi)
        .createWorkspaceMethodConfig(argument.capture(), any(), any());
    assertThat(argument.getValue().getMethodConfigVersion())
        .isEqualTo(workbenchConfig.wgsCohortExtraction.legacyVersions.methodRepoVersion);
    assertThat(argument.getValue().getNamespace())
        .isEqualTo(workbenchConfig.wgsCohortExtraction.legacyVersions.methodNamespace);

    verify(cloudStorageClient)
        .writeFile(
            eq(workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceBucket),
            matches("genomic-extractions\\/.*\\/person_ids.txt"),
            any());
    List<DbWgsExtractCromwellSubmission> dbSubmissions =
        ImmutableList.copyOf(wgsExtractCromwellSubmissionDao.findAll());
    assertThat(dbSubmissions.size()).isEqualTo(1);
    assertThat(dbSubmissions.get(0).getSubmissionId()).isEqualTo(FC_SUBMISSION_ID);
    assertThat(dbSubmissions.get(0).getSampleCount()).isEqualTo(3);
    assertThat(dbSubmissions.get(0).getTerraSubmissionDate())
        .isEqualTo(new Timestamp(CLOCK.instant().toEpochMilli()));
  }

  @Test
  public void submitExtractionJob_outputVcfsInCorrectBucket() throws ApiException {
    when(mockGenomicDatasetService.getPersonIdsWithWholeGenome(any())).thenReturn(List.of("1"));
    TanagraGenomicDataRequest tanagraRequest = null;
    genomicExtractionService.submitGenomicExtractionJob(targetWorkspace, dataset, tanagraRequest);

    ArgumentCaptor<FirecloudMethodConfiguration> argument =
        ArgumentCaptor.forClass(FirecloudMethodConfiguration.class);

    verify(mockMethodConfigurationsApi)
        .createWorkspaceMethodConfig(argument.capture(), any(), any());
    String actualOutputDir =
        argument.getValue().getInputs().get(EXTRACT_WORKFLOW_NAME + ".output_gcs_dir");

    assertThat(actualOutputDir)
        .matches("\"gs:\\/\\/user-bucket\\/genomic-extractions\\/.*\\/vcfs\\/\"");
  }

  @Test
  public void submitExtractionJob_many() throws ApiException {
    final List<String> largePersonIdList =
        LongStream.range(1, 376).boxed().map(Object::toString).toList();
    when(mockGenomicDatasetService.getPersonIdsWithWholeGenome(any()))
        .thenReturn(largePersonIdList);
    TanagraGenomicDataRequest tanagraRequest = null;
    genomicExtractionService.submitGenomicExtractionJob(targetWorkspace, dataset, tanagraRequest);

    ArgumentCaptor<FirecloudMethodConfiguration> argument =
        ArgumentCaptor.forClass(FirecloudMethodConfiguration.class);

    verify(mockMethodConfigurationsApi)
        .createWorkspaceMethodConfig(argument.capture(), any(), any());
    String actualScatter =
        argument.getValue().getInputs().get(EXTRACT_WORKFLOW_NAME + ".scatter_count");
    assertThat(actualScatter).isEqualTo("1500");
  }

  @Test
  public void submitExtractionJob_v8() throws ApiException {
    when(mockGenomicDatasetService.getPersonIdsWithWholeGenome(any()))
        .thenReturn(List.of("1", "2", "3"));

    DbCdrVersion cdrV8 =
        cdrVersionDao.save(
            new DbCdrVersion()
                .setCdrVersionId(8)
                .setPublicReleaseNumber(8)
                .setBigqueryProject("bigquery_project")
                .setWgsBigqueryDataset("wgs_dataset"));
    targetWorkspace = workspaceDao.save(targetWorkspace.setCdrVersion(cdrV8));

    TanagraGenomicDataRequest tanagraRequest = null;
    genomicExtractionService.submitGenomicExtractionJob(targetWorkspace, dataset, tanagraRequest);

    ArgumentCaptor<FirecloudMethodConfiguration> argument =
        ArgumentCaptor.forClass(FirecloudMethodConfiguration.class);

    verify(mockMethodConfigurationsApi)
        .createWorkspaceMethodConfig(argument.capture(), any(), any());
    assertThat(argument.getValue().getMethodConfigVersion())
        .isEqualTo(workbenchConfig.wgsCohortExtraction.cdrv8plus.methodRepoVersion);
    assertThat(argument.getValue().getNamespace())
        .isEqualTo(workbenchConfig.wgsCohortExtraction.cdrv8plus.methodNamespace);
  }

  @Test
  public void submitExtractionJob_noWgsData() {
    when(mockGenomicDatasetService.getPersonIdsWithWholeGenome(any()))
        .thenReturn(Collections.emptyList());

    TanagraGenomicDataRequest tanagraRequest = null;
    assertThrows(
        FailedPreconditionException.class,
        () ->
            genomicExtractionService.submitGenomicExtractionJob(
                targetWorkspace, dataset, tanagraRequest));
  }

  @Test
  public void submitExtractionJob_tooManySamples() {
    final List<String> largePersonIdList =
        LongStream.range(1, 6_000).boxed().map(Object::toString).toList();
    when(mockGenomicDatasetService.getPersonIdsWithWholeGenome(any()))
        .thenReturn(largePersonIdList);

    TanagraGenomicDataRequest tanagraRequest = null;
    assertThrows(
        FailedPreconditionException.class,
        () ->
            genomicExtractionService.submitGenomicExtractionJob(
                targetWorkspace, dataset, tanagraRequest));
  }

  @Test
  public void abortGenomicExtractionJob() throws ApiException {
    DbWgsExtractCromwellSubmission dbWgsExtractCromwellSubmission =
        createSubmissionAndMockMonitorCall(
            FirecloudSubmissionStatus.EVALUATING, FirecloudWorkflowStatus.RUNNING);

    doNothing()
        .when(mockSubmissionsApi)
        .abortSubmission(
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace,
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName,
            dbWgsExtractCromwellSubmission.getSubmissionId());

    genomicExtractionService.abortGenomicExtractionJob(
        targetWorkspace,
        String.valueOf(dbWgsExtractCromwellSubmission.getWgsExtractCromwellSubmissionId()));

    verify(mockSubmissionsApi, times(1))
        .abortSubmission(
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceNamespace,
            workbenchConfig.wgsCohortExtraction.operationalTerraWorkspaceName,
            dbWgsExtractCromwellSubmission.getSubmissionId());
  }

  private DbDataset createDataset() {
    DbDataset dataset = new DbDataset();
    dataset.setWorkspaceId(targetWorkspace.getWorkspaceId());
    dataset.setName("my dataset");
    return dataSetDao.save(dataset);
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    return userDao.save(user);
  }
}
