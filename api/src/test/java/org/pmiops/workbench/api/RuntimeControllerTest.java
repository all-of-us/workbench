package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME_TRUE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAME;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAMESPACE;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;

import com.google.cloud.Date;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.VwbAccessService;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.api.RuntimesApi;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAuditInfo;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoClusterError;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskType;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoMachineConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeImage;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.leonardo.LegacyLeonardoRetryHandler;
import org.pmiops.workbench.leonardo.LeonardoApiClientFactory;
import org.pmiops.workbench.leonardo.LeonardoApiClientImpl;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.LeonardoConfig;
import org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.GpuConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeError;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.model.UpdateRuntimeRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RuntimeControllerTest {
  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  private static final String WORKSPACE_DISPLAY_NAME = "My First Workspace";
  // The Terra Name (or Firecloud Name) identifier is generated based on the display name of the
  // workspace upon first creation, removing whitespace and punctuation, and lowercasing everything.
  // Note that when a workspace name changes, the Terra Name stays the same.
  private static final String WORKSPACE_TERRA_NAME = "myfirstworkspace";
  private static final String WORKSPACE_CREATOR = "test-user@whatever.com";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String OTHER_USER_EMAIL = "alice@gmail.com";
  private static final String BUCKET_NAME = "workspace-bucket";
  private static final String API_HOST = "api.stable.fake-research-aou.org";
  private static final String API_BASE_URL = "https://" + API_HOST;
  private static final String LEONARDO_URL = "https://leonardo.dsde-dev.broadinstitute.org";
  private static final String BIGQUERY_DATASET = "dataset-name";
  private static final String TOOL_DOCKER_IMAGE = "docker-image";
  private static final LeonardoRuntimeImage RUNTIME_IMAGE =
      new LeonardoRuntimeImage().imageType("Jupyter").imageUrl(TOOL_DOCKER_IMAGE);
  private static final int AUTOPAUSE_THRESHOLD = 10;

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FakeClockConfiguration.class,
    FirecloudMapperImpl.class,
    LeonardoApiClientImpl.class,
    LeonardoApiHelper.class,
    LeonardoMapperImpl.class,
    LegacyLeonardoRetryHandler.class,
    LeonardoRetryHandler.class,
    NoBackOffPolicy.class,
    NotebooksRetryHandler.class,
    PublicInstitutionDetailsMapperImpl.class,
    RuntimeController.class,
    UserServiceTestConfiguration.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    AccessModuleService.class,
    CohortService.class,
    ConceptSetService.class,
    DirectoryService.class,
    InteractiveAnalysisService.class,
    LeonardoApiClientFactory.class,
    MailService.class,
    UserServiceAuditor.class,
    InitialCreditsService.class,
    VwbAccessService.class,
    VwbUserService.class,
    TaskQueueService.class
  })
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return user;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }
  }

  @Captor private ArgumentCaptor<LeonardoCreateRuntimeRequest> createRuntimeRequestCaptor;
  @Captor private ArgumentCaptor<LeonardoUpdateRuntimeRequest> updateRuntimeRequestCaptor;

  @Qualifier(LeonardoConfig.USER_RUNTIMES_API)
  @MockBean
  RuntimesApi mockUserRuntimesApi;

  @MockBean
  @Qualifier(LeonardoConfig.USER_DISKS_API)
  DisksApi mockUserDisksApi;

  @MockBean FireCloudService mockFireCloudService;
  @MockBean WorkspaceAuthService mockWorkspaceAuthService;
  @MockBean WorkspaceDao mockWorkspaceDao;
  @MockBean WorkspaceService mockWorkspaceService;

  @Autowired AccessTierDao accessTierDao;
  @Autowired FirecloudMapper firecloudMapper;
  @Autowired LeonardoMapper leonardoMapper;
  @Autowired UserDao userDao;

  @Autowired RuntimeController runtimeController;

  private DbCdrVersion cdrVersion;
  private LeonardoGetRuntimeResponse testLeoRuntime;

  private Runtime testRuntime;

  private DataprocConfig dataprocConfig;
  private LeonardoMachineConfig leoDataprocConfig;

  private GceConfig gceConfig;
  private LeonardoGceWithPdConfig leoGceConfig;

  @BeforeEach
  public void setUp() throws Exception {
    config = WorkbenchConfig.createEmptyConfig();
    config.billing.accountId = "initial-credits";
    config.firecloud.leoBaseUrl = LEONARDO_URL;
    config.firecloud.gceVmZones = ImmutableList.of("us-central1-a", "us-central1-b");
    config.server.apiBaseUrl = API_BASE_URL;
    config.server.apiAssetsBaseUrl = API_BASE_URL;
    config.access.enableComplianceTraining = true;

    user = new DbUser().setUsername(LOGGED_IN_USER_EMAIL).setUserId(123L);

    createUser(OTHER_USER_EMAIL);

    cdrVersion =
        new DbCdrVersion()
            .setName("1")
            // set the db name to be empty since test cases currently
            // run in the workbench schema only.
            .setCdrDbName("")
            .setBigqueryDataset(BIGQUERY_DATASET)
            .setAccessTier(
                accessTierDao.save(createControlledTier()).setDatasetsBucket("gs://cdr-bucket"))
            .setStorageBasePath("v99")
            .setWgsCramManifestPath("wgs/cram/manifest.csv");

    String createdDate = Date.fromYearMonthDay(1988, 12, 26).toString();

    Runtime tmpRuntime = new Runtime();

    leoDataprocConfig =
        new LeonardoMachineConfig()
            .cloudService(LeonardoMachineConfig.CloudServiceEnum.DATAPROC)
            .numberOfWorkers(0)
            .masterMachineType("n1-standard-4")
            .masterDiskSize(50);

    leonardoMapper.mapRuntimeConfig(tmpRuntime, leoDataprocConfig, null);
    dataprocConfig = tmpRuntime.getDataprocConfig();

    leoGceConfig =
        new LeonardoGceWithPdConfig()
            .cloudService(LeonardoGceWithPdConfig.CloudServiceEnum.GCE)
            .bootDiskSize(10)
            .machineType("n1-standard-2");

    leonardoMapper.mapRuntimeConfig(tmpRuntime, leoGceConfig, null);
    gceConfig = tmpRuntime.getGceConfig();

    testLeoRuntime =
        new LeonardoGetRuntimeResponse()
            .runtimeName(getRuntimeName())
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(LeonardoRuntimeStatus.DELETING)
            .runtimeImages(Collections.singletonList(RUNTIME_IMAGE))
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .runtimeConfig(leoDataprocConfig)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));

    testRuntime =
        new Runtime()
            .runtimeName(getRuntimeName())
            .googleProject(GOOGLE_PROJECT_ID)
            .status(RuntimeStatus.DELETING)
            .toolDockerImage(TOOL_DOCKER_IMAGE)
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .dataprocConfig(dataprocConfig)
            .createdDate(createdDate)
            .errors(Collections.emptyList());

    DbWorkspace testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_DISPLAY_NAME)
            .setFirecloudName(WORKSPACE_TERRA_NAME)
            .setCdrVersion(cdrVersion)
            .setBillingAccountName(config.billing.initialCreditsBillingAccountName());
    doReturn(testWorkspace).when(mockWorkspaceService).lookupWorkspaceByNamespace(WORKSPACE_NS);
    doReturn(Optional.of(testWorkspace)).when(mockWorkspaceDao).getByNamespace(WORKSPACE_NS);

    when(mockUserDisksApi.listDisksByProject(any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
  }

  private static RawlsWorkspaceDetails createFcWorkspace(
      String ns, String googleProject, String name, String creator) {
    return new RawlsWorkspaceDetails()
        .namespace(ns)
        .name(name)
        .createdBy(creator)
        .googleProject(googleProject)
        .bucketName(BUCKET_NAME);
  }

  private void stubGetWorkspace() {
    stubGetWorkspace(WorkspaceAccessLevel.OWNER);
  }

  private void stubGetWorkspace(WorkspaceAccessLevel accessLevel) {
    stubGetWorkspace(
        WORKSPACE_NS,
        GOOGLE_PROJECT_ID,
        WORKSPACE_DISPLAY_NAME,
        WORKSPACE_TERRA_NAME,
        WORKSPACE_CREATOR,
        accessLevel);
  }

  private void stubGetV1Workspace(WorkspaceAccessLevel accessLevel) {
    stubGetWorkspace(
        WORKSPACE_NS,
        WORKSPACE_NS, // namespace == project, for v1 workspaces
        WORKSPACE_DISPLAY_NAME,
        WORKSPACE_TERRA_NAME,
        WORKSPACE_CREATOR,
        accessLevel);
  }

  private void stubGetWorkspace(
      String workspaceNamespace,
      String googleProject,
      String displayName,
      String firecloudName,
      String creator,
      WorkspaceAccessLevel accessLevel) {
    DbWorkspace w = new DbWorkspace();
    w.setWorkspaceNamespace(workspaceNamespace);
    w.setName(displayName);
    w.setFirecloudName(firecloudName);
    w.setCdrVersion(cdrVersion);
    w.setGoogleProject(googleProject);
    when(mockWorkspaceDao.getRequired(workspaceNamespace, firecloudName)).thenReturn(w);
    when(mockWorkspaceDao.getByNamespace(workspaceNamespace)).thenReturn(Optional.of(w));
    stubGetFcWorkspace(
        createFcWorkspace(workspaceNamespace, googleProject, firecloudName, creator), accessLevel);
  }

  private void stubGetFcWorkspace(
      RawlsWorkspaceDetails fcWorkspace, WorkspaceAccessLevel accessLevel) {
    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(accessLevel));
    when(mockFireCloudService.getWorkspace(any())).thenReturn(Optional.of(fcResponse));
    when(mockFireCloudService.getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName()))
        .thenReturn(fcResponse);
  }

  private String getRuntimeName() {
    return "all-of-us-".concat(Long.toString(user.getUserId()));
  }

  private String getPdName() {
    return "all-of-us-pd-".concat(Long.toString(user.getUserId()));
  }

  @Test
  public void testGetRuntime() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody()).isEqualTo(testRuntime);
  }

  @Test
  public void testGetRuntime_error() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(
            testLeoRuntime
                .status(LeonardoRuntimeStatus.ERROR)
                .errors(
                    ImmutableList.of(
                        new LeonardoClusterError().errorCode(1).errorMessage("foo"),
                        new LeonardoClusterError().errorCode(2).errorMessage(null))));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody())
        .isEqualTo(
            testRuntime
                .status(RuntimeStatus.ERROR)
                .errors(
                    ImmutableList.of(
                        new RuntimeError().errorCode(1).errorMessage("foo"),
                        new RuntimeError().errorCode(2).errorMessage(null))));
  }

  @Test
  public void testGetRuntime_error_nullMessages() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(LeonardoRuntimeStatus.ERROR).errors(null));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody())
        .isEqualTo(testRuntime.status(RuntimeStatus.ERROR).errors(null));
  }

  @Test
  public void testGetRuntime_error_emptyMessageList() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(
            testLeoRuntime.status(LeonardoRuntimeStatus.ERROR).errors(Collections.emptyList()));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody())
        .isEqualTo(testRuntime.status(RuntimeStatus.ERROR).errors(Collections.emptyList()));
  }

  @Test
  public void testGetRuntime_securitySuspended() throws ApiException {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThrows(
        FailedPreconditionException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_securitySuspendedElapsed() throws ApiException {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().minus(Duration.ofMinutes(20))));
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    runtimeController.getRuntime(WORKSPACE_NS);
  }

  @Test
  public void testGetRuntime_noGetRuntime() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_gceConfig() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.runtimeConfig(leoGceConfig));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody())
        .isEqualTo(testRuntime.dataprocConfig(null).gceConfig(gceConfig));
  }

  @Test
  public void testGetRuntime_diskConfig() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(
            testLeoRuntime
                .runtimeConfig(leoGceConfig)
                .diskConfig(
                    new LeonardoDiskConfig().diskType(LeonardoDiskType.SSD).name("pd").size(200)));

    Runtime runtime = runtimeController.getRuntime(WORKSPACE_NS).getBody();

    assertThat(runtime.getGceWithPdConfig().getPersistentDisk())
        .isEqualTo(new PersistentDiskRequest().diskType(DiskType.SSD).name("pd").size(200));
  }

  @Test
  public void testGetRuntime_UnknownStatus() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(null));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getStatus())
        .isEqualTo(RuntimeStatus.UNKNOWN);
  }

  @Test
  public void testGetRuntime_NullBillingProject() {
    doThrow(new NotFoundException()).when(mockWorkspaceService).lookupWorkspaceByNamespace("123");
    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime("123"));
  }

  @Test
  public void testCreateRuntime_customRuntimeEnabled_noRuntimes() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    assertThrows(
        BadRequestException.class,
        () -> runtimeController.createRuntime(WORKSPACE_NS, new Runtime()));
  }

  @Test
  public void testCreateRuntime_customRuntimeEnabled_twoRuntimes() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    assertThrows(
        BadRequestException.class,
        () ->
            runtimeController.createRuntime(
                WORKSPACE_NS,
                new Runtime()
                    .dataprocConfig(new DataprocConfig().masterMachineType("standard"))
                    .gceConfig(new GceConfig().machineType("standard"))));
  }

  @Test
  public void testCreateRuntime_dataproc() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .dataprocConfig(
                new DataprocConfig()
                    .numberOfWorkers(5)
                    .workerMachineType("worker")
                    .workerDiskSize(10)
                    .numberOfWorkerLocalSSDs(1)
                    .numberOfPreemptibleWorkers(3)
                    .masterDiskSize(100)
                    .masterMachineType("standard")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();

    Gson gson = new Gson();
    LeonardoMachineConfig createLeonardoMachineConfig =
        gson.fromJson(
            gson.toJson(createRuntimeRequest.getRuntimeConfig()), LeonardoMachineConfig.class);

    assertThat(
            gson.fromJson(
                    gson.toJson(createRuntimeRequest.getRuntimeConfig()),
                    LeonardoRuntimeConfig.class)
                .getCloudService())
        .isEqualTo(LeonardoRuntimeConfig.CloudServiceEnum.DATAPROC);
    assertThat(createLeonardoMachineConfig.getNumberOfWorkers()).isEqualTo(5);
    assertThat(createLeonardoMachineConfig.getWorkerMachineType()).isEqualTo("worker");
    assertThat(createLeonardoMachineConfig.getWorkerDiskSize()).isEqualTo(10);
    assertThat(createLeonardoMachineConfig.getNumberOfWorkerLocalSSDs()).isEqualTo(1);
    assertThat(createLeonardoMachineConfig.getNumberOfPreemptibleWorkers()).isEqualTo(3);
    assertThat(createLeonardoMachineConfig.getMasterDiskSize()).isEqualTo(100);
    assertThat(createLeonardoMachineConfig.getMasterMachineType()).isEqualTo("standard");
  }

  @Test
  public void testCreateRuntime_gce() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(new GceConfig().diskSize(50).machineType("standard").zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();

    Gson gson = new Gson();
    LeonardoGceConfig createLeonardoGceConfig =
        gson.fromJson(
            gson.toJson(createRuntimeRequest.getRuntimeConfig()), LeonardoGceConfig.class);

    assertThat(
            gson.fromJson(
                    gson.toJson(createRuntimeRequest.getRuntimeConfig()),
                    LeonardoRuntimeConfig.class)
                .getCloudService())
        .isEqualTo(LeonardoRuntimeConfig.CloudServiceEnum.GCE);
    assertThat(createLeonardoGceConfig.getDiskSize()).isEqualTo(50);

    assertThat(createLeonardoGceConfig.getMachineType()).isEqualTo("standard");
    assertThat(createLeonardoGceConfig.getZone()).isEqualTo("us-central1-a");
  }

  @Test
  public void testCreateRuntime_gce_invalidZone() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                runtimeController.createRuntime(
                    WORKSPACE_NS,
                    new Runtime()
                        .gceConfig(
                            new GceConfig()
                                .diskSize(50)
                                .machineType("standard")
                                .zone("us-central1-x"))));

    assertThat(exception.getMessage()).contains("Invalid zone");
  }

  @Test
  public void testCreateRuntime_gceWithPD() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceWithPdConfig(
                new GceWithPdConfig()
                    .machineType("standard")
                    .persistentDisk(
                        new PersistentDiskRequest()
                            .diskType(DiskType.SSD)
                            .name(getPdName())
                            .size(500))
                    .zone("us-central1-a")));

    Map<String, String> diskLabels = new HashMap<>();
    diskLabels.put(LEONARDO_LABEL_IS_RUNTIME, LEONARDO_LABEL_IS_RUNTIME_TRUE);
    diskLabels.put(LEONARDO_LABEL_WORKSPACE_NAMESPACE, WORKSPACE_NS);
    diskLabels.put(LEONARDO_LABEL_WORKSPACE_NAME, WORKSPACE_DISPLAY_NAME);

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();

    Gson gson = new Gson();
    LeonardoGceWithPdConfig createLeonardoGceWithPdConfig =
        gson.fromJson(
            gson.toJson(createRuntimeRequest.getRuntimeConfig()), LeonardoGceWithPdConfig.class);

    assertThat(
            gson.fromJson(
                    gson.toJson(createRuntimeRequest.getRuntimeConfig()),
                    LeonardoRuntimeConfig.class)
                .getCloudService())
        .isEqualTo(LeonardoRuntimeConfig.CloudServiceEnum.GCE);

    assertThat(createLeonardoGceWithPdConfig.getMachineType()).isEqualTo("standard");
    assertThat(createLeonardoGceWithPdConfig.getPersistentDisk().getDiskType())
        .isEqualTo(LeonardoDiskType.SSD);
    assertThat(createLeonardoGceWithPdConfig.getPersistentDisk().getName()).isEqualTo(getPdName());
    assertThat(createLeonardoGceWithPdConfig.getPersistentDisk().getSize()).isEqualTo(500);
    assertThat(createLeonardoGceWithPdConfig.getPersistentDisk().getLabels()).isEqualTo(diskLabels);
    assertThat(createLeonardoGceWithPdConfig.getZone()).isEqualTo("us-central1-a");
  }

  @Test
  public void testCreateRuntime_gceWithPD_noPDRequest() {
    // don't add the required persistentDisk field
    var gceWithPdConfig_lacksPD =
        new Runtime().gceWithPdConfig(new GceWithPdConfig().machineType("standard"));
    assertThrows(
        BadRequestException.class,
        () -> runtimeController.createRuntime(WORKSPACE_NS, gceWithPdConfig_lacksPD));
  }

  @Test
  public void testCreateRuntimeFail_newPdp_pdAlreadyExist()
      throws ApiException, org.broadinstitute.dsde.workbench.client.leonardo.ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    ListPersistentDiskResponse gceDisk =
        new ListPersistentDiskResponse()
            .name("123")
            .cloudContext(
                new CloudContext()
                    .cloudProvider(CloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(DiskStatus.READY);
    when(mockUserDisksApi.listDisksByProject(any(), any(), any(), any(), any()))
        .thenReturn(List.of(gceDisk));

    stubGetWorkspace();

    assertThrows(
        BadRequestException.class,
        () ->
            runtimeController.createRuntime(
                WORKSPACE_NS,
                new Runtime()
                    .gceWithPdConfig(
                        new GceWithPdConfig()
                            .machineType("standard")
                            .persistentDisk(
                                new PersistentDiskRequest().diskType(DiskType.SSD).size(500)))));
  }

  @Test
  public void testCreateRuntime_nullRuntime() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    BadRequestException e =
        assertThrows(
            BadRequestException.class, () -> runtimeController.createRuntime(WORKSPACE_NS, null));

    assertThat(e)
        .hasMessageThat()
        .contains("Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
  }

  @Test
  public void testCreateRuntime_emptyRuntime() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    BadRequestException e =
        assertThrows(
            BadRequestException.class,
            () -> runtimeController.createRuntime(WORKSPACE_NS, new Runtime()));

    assertThat(e)
        .hasMessageThat()
        .contains("Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
  }

  @Test
  public void testCreateRuntime_gceWithGpu() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(
                new GceConfig()
                    .diskSize(50)
                    .machineType("standard")
                    .gpuConfig(new GpuConfig().gpuType("nvidia-tesla-t4").numOfGpus(2))
                    .zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();

    Gson gson = new Gson();
    LeonardoGceConfig createLeonardoGceConfig =
        gson.fromJson(
            gson.toJson(createRuntimeRequest.getRuntimeConfig()), LeonardoGceConfig.class);

    assertThat(
            gson.fromJson(
                    gson.toJson(createRuntimeRequest.getRuntimeConfig()),
                    LeonardoRuntimeConfig.class)
                .getCloudService())
        .isEqualTo(LeonardoRuntimeConfig.CloudServiceEnum.GCE);
    assertThat(createLeonardoGceConfig.getDiskSize()).isEqualTo(50);
    assertThat(createLeonardoGceConfig.getMachineType()).isEqualTo("standard");
    assertThat(createLeonardoGceConfig.getGpuConfig().getGpuType()).isEqualTo("nvidia-tesla-t4");
    assertThat(createLeonardoGceConfig.getGpuConfig().getNumOfGpus()).isEqualTo(2);
  }

  @Test
  public void testCreateRuntime_gceWithPD_wihGpu() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace();

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceWithPdConfig(
                new GceWithPdConfig()
                    .machineType("standard")
                    .persistentDisk(
                        new PersistentDiskRequest()
                            .diskType(DiskType.SSD)
                            .name(getPdName())
                            .size(500))
                    .gpuConfig(new GpuConfig().gpuType("nvidia-tesla-t4").numOfGpus(2))
                    .zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();

    Gson gson = new Gson();
    LeonardoGceWithPdConfig createLeonardoGceWithPdConfig =
        gson.fromJson(
            gson.toJson(createRuntimeRequest.getRuntimeConfig()), LeonardoGceWithPdConfig.class);

    assertThat(
            gson.fromJson(
                    gson.toJson(createRuntimeRequest.getRuntimeConfig()),
                    LeonardoRuntimeConfig.class)
                .getCloudService())
        .isEqualTo(LeonardoRuntimeConfig.CloudServiceEnum.GCE);
    assertThat(createLeonardoGceWithPdConfig.getGpuConfig().getGpuType())
        .isEqualTo("nvidia-tesla-t4");
    assertThat(createLeonardoGceWithPdConfig.getGpuConfig().getNumOfGpus()).isEqualTo(2);
  }

  @Test
  public void testCreateRuntime_securitySuspended() {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));

    assertThrows(
        FailedPreconditionException.class,
        () ->
            runtimeController.createRuntime(
                WORKSPACE_NS,
                new Runtime()
                    .gceConfig(
                        new GceConfig()
                            .diskSize(50)
                            .machineType("standard")
                            .zone("us-central1-a"))));
  }

  @Test
  public void testCreateRuntime_terraWorkspaceV1() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(WORKSPACE_NS, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetV1Workspace(WorkspaceAccessLevel.WRITER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(new GceConfig().diskSize(50).machineType("standard").zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    Gson gson = new Gson();
    assertThat(
            gson.toJsonTree(createRuntimeRequest.getCustomEnvironmentVariables())
                .getAsJsonObject()
                .has(LeonardoCustomEnvVarUtils.BIGQUERY_STORAGE_API_ENABLED_ENV_KEY))
        .isFalse();
  }

  @Test
  public void testCreateRuntime_terraWorkspaceV1Owner() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(WORKSPACE_NS, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetV1Workspace(WorkspaceAccessLevel.OWNER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(new GceConfig().diskSize(50).machineType("standard").zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    Gson gson = new Gson();
    assertThat(
            gson.toJsonTree(createRuntimeRequest.getCustomEnvironmentVariables())
                .getAsJsonObject()
                .getAsJsonPrimitive(LeonardoCustomEnvVarUtils.BIGQUERY_STORAGE_API_ENABLED_ENV_KEY)
                .isString())
        .isTrue();
  }

  @Test
  public void testCreateRuntime_terraWorkspaceV2() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WorkspaceAccessLevel.WRITER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(new GceConfig().diskSize(50).machineType("standard").zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    Gson gson = new Gson();
    assertThat(
            gson.toJsonTree(createRuntimeRequest.getCustomEnvironmentVariables())
                .getAsJsonObject()
                .getAsJsonPrimitive(LeonardoCustomEnvVarUtils.BIGQUERY_STORAGE_API_ENABLED_ENV_KEY)
                .isString())
        .isTrue();
  }

  @Test
  public void testCreateRuntime_cdrEnvVars() throws ApiException {
    when(mockUserRuntimesApi.getRuntime(WORKSPACE_NS, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetV1Workspace(WorkspaceAccessLevel.WRITER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(new GceConfig().diskSize(50).machineType("standard").zone("us-central1-a")));

    verify(mockUserRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    JsonObject envVars =
        new Gson()
            .toJsonTree(createRuntimeRequest.getCustomEnvironmentVariables())
            .getAsJsonObject();
    assertThat(envVars.get(LeonardoCustomEnvVarUtils.WORKSPACE_CDR_ENV_KEY).getAsString())
        .isEqualTo(cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset());
    assertThat(envVars.get(LeonardoCustomEnvVarUtils.WGS_CRAM_MANIFEST_PATH_KEY).getAsString())
        .isEqualTo("gs://cdr-bucket/v99/wgs/cram/manifest.csv");
  }

  @Test
  public void testUpdateRuntime() throws ApiException {
    stubGetWorkspace();

    runtimeController.updateRuntime(
        WORKSPACE_NS,
        new UpdateRuntimeRequest().runtime(new Runtime().dataprocConfig(dataprocConfig)));
    verify(mockUserRuntimesApi)
        .updateRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), updateRuntimeRequestCaptor.capture());

    LeonardoUpdateDataprocConfig actualRuntimeConfig =
        (LeonardoUpdateDataprocConfig) updateRuntimeRequestCaptor.getValue().getRuntimeConfig();
    assertThat(actualRuntimeConfig.getCloudService().getValue()).isEqualTo("DATAPROC");
    assertThat(actualRuntimeConfig.getNumberOfWorkers())
        .isEqualTo(dataprocConfig.getNumberOfWorkers());
    assertThat(actualRuntimeConfig.getMasterMachineType())
        .isEqualTo(dataprocConfig.getMasterMachineType());
    assertThat(actualRuntimeConfig.getMasterDiskSize())
        .isEqualTo(dataprocConfig.getMasterDiskSize());
  }

  @Test
  public void testDeleteRuntime() throws ApiException {
    runtimeController.deleteRuntime(WORKSPACE_NS, false);
    verify(mockUserRuntimesApi).deleteRuntime(GOOGLE_PROJECT_ID, getRuntimeName(), false);
  }

  @Test
  public void testLocalize_securitySuspended() {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));

    assertThrows(
        FailedPreconditionException.class,
        () -> runtimeController.localize(WORKSPACE_NS, false, new RuntimeLocalizeRequest()));
  }

  @Test
  public void localize_validateInitialCreditUsage() {
    doThrow(new ForbiddenException())
        .when(mockWorkspaceAuthService)
        .validateInitialCreditUsage(eq(WORKSPACE_NS), eq(WORKSPACE_TERRA_NAME));

    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();
    assertThrows(
        ForbiddenException.class, () -> runtimeController.localize(WORKSPACE_NS, false, req));
  }

  @Test
  public void localize_validateInitialCreditUsage_checkAccessFirst() {
    doThrow(new ForbiddenException())
        .when(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(
            WORKSPACE_NS, WORKSPACE_TERRA_NAME, WorkspaceAccessLevel.WRITER);

    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();

    assertThrows(
        ForbiddenException.class, () -> runtimeController.localize(WORKSPACE_NS, false, req));
    verify(mockWorkspaceAuthService, never()).validateInitialCreditUsage(anyString(), anyString());
  }

  private void createUser(String email) {
    DbUser user = new DbUser();
    user.setGivenName("first");
    user.setFamilyName("last");
    user.setUsername(email);
    userDao.save(user);
  }
}
