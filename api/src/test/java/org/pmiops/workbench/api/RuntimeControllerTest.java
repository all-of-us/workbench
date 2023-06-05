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
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;

import com.google.cloud.Date;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
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
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClientFactory;
import org.pmiops.workbench.leonardo.LeonardoApiClientImpl;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.LeonardoConfig;
import org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoCloudProvider;
import org.pmiops.workbench.leonardo.model.LeonardoClusterError;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoDiskConfig;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGceWithPdConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.GpuConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.RuntimeError;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.model.UpdateRuntimeRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
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
  private static final String GOOGLE_PROJECT_ID_2 = "aou-gcp-id-2";
  // Workspace ID is also known as firecloud_name. This identifier is generated by
  // Firecloud, based on the name of the workspace upon first creation. Firecloud
  // tends to remove whitespace and punctuation, lowercase everything, and concatenate
  // it together. Note that when a workspace name changes, the Firecloud name stays
  // the same.
  private static final String WORKSPACE_ID = "myfirstworkspace";
  private static final String WORKSPACE_NAME = "My First Workspace";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String OTHER_USER_EMAIL = "alice@gmail.com";
  private static final String BUCKET_NAME = "workspace-bucket";
  private static final String API_HOST = "api.stable.fake-research-aou.org";
  private static final String API_BASE_URL = "https://" + API_HOST;
  private static final String LEONARDO_URL = "https://leonardo.dsde-dev.broadinstitute.org";
  private static final String BIGQUERY_DATASET = "dataset-name";
  private static final String EXTRA_RUNTIME_NAME = "all-of-us-extra";
  private static final String EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT = "all-of-us-different-project";
  private static final String TOOL_DOCKER_IMAGE = "docker-image";
  private static final LeonardoRuntimeImage RUNTIME_IMAGE =
      new LeonardoRuntimeImage().imageType("Jupyter").imageUrl(TOOL_DOCKER_IMAGE);
  private static final int AUTOPAUSE_THRESHOLD = 10;

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    RuntimeController.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceMapperImpl.class,
    CommonMappers.class,
    PublicInstitutionDetailsMapperImpl.class,
    UserServiceTestConfiguration.class,
    LeonardoMapperImpl.class,
    LeonardoApiClientImpl.class,
    NotebooksRetryHandler.class,
    LeonardoRetryHandler.class,
    NoBackOffPolicy.class,
    AccessTierServiceImpl.class,
    LeonardoApiHelper.class,
  })
  @MockBean({
    AccessModuleService.class,
    ConceptSetService.class,
    CohortService.class,
    MailService.class,
    InteractiveAnalysisService.class,
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

  @MockBean LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockBean ComplianceService mockComplianceService;
  @MockBean DirectoryService mockDirectoryService;
  @MockBean FireCloudService mockFireCloudService;
  @MockBean UserRecentResourceService mockUserRecentResourceService;
  @MockBean UserServiceAuditor mockUserServiceAuditor;
  @MockBean WorkspaceAuthService mockWorkspaceAuthService;
  @MockBean LeonardoApiClientFactory mockLeonardoApiClientFactory;

  @Qualifier(LeonardoConfig.USER_RUNTIMES_API)
  @MockBean
  RuntimesApi userRuntimesApi;

  @Qualifier(LeonardoConfig.SERVICE_RUNTIMES_API)
  @MockBean
  RuntimesApi serviceRuntimesApi;

  @MockBean ProxyApi proxyApi;

  @MockBean
  @Qualifier(LeonardoConfig.USER_DISKS_API)
  DisksApi userDisksApi;

  @MockBean WorkspaceDao workspaceDao;
  @MockBean WorkspaceService workspaceService;

  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired UserDao userDao;
  @Autowired AccessTierDao accessTierDao;
  @Autowired RuntimeController runtimeController;
  @Autowired LeonardoMapper leonardoMapper;
  @Autowired FirecloudMapper firecloudMapper;

  private DbCdrVersion cdrVersion;
  private LeonardoGetRuntimeResponse testLeoRuntime;

  private Runtime testRuntime;
  private DbWorkspace testWorkspace;

  private DataprocConfig dataprocConfig;
  private LinkedTreeMap<String, Object> dataprocConfigObj;

  private GceConfig gceConfig;
  private LinkedTreeMap<String, Object> gceConfigObj;

  @BeforeEach
  public void setUp() throws Exception {
    config = WorkbenchConfig.createEmptyConfig();
    config.firecloud.leoBaseUrl = LEONARDO_URL;
    config.server.apiBaseUrl = API_BASE_URL;
    config.server.apiAssetsBaseUrl = API_BASE_URL;
    config.access.enableComplianceTraining = true;
    config.firecloud.gceVmZone = "us-central-1";

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

    dataprocConfigObj = new LinkedTreeMap<>();
    dataprocConfigObj.put("cloudService", "DATAPROC");
    dataprocConfigObj.put("numberOfWorkers", 0);
    dataprocConfigObj.put("masterMachineType", "n1-standard-4");
    dataprocConfigObj.put("masterDiskSize", 50.0);

    leonardoMapper.mapRuntimeConfig(tmpRuntime, dataprocConfigObj, null);
    dataprocConfig = tmpRuntime.getDataprocConfig();

    gceConfigObj = new LinkedTreeMap<>();
    gceConfigObj.put("cloudService", "GCE");
    gceConfigObj.put("bootDiskSize", 10.0);
    gceConfigObj.put("diskSize", 100.0);
    gceConfigObj.put("machineType", "n1-standard-2");

    leonardoMapper.mapRuntimeConfig(tmpRuntime, gceConfigObj, null);
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
            .runtimeConfig(dataprocConfigObj)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));

    testRuntime =
        new Runtime()
            .runtimeName(getRuntimeName())
            .configurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(RuntimeStatus.DELETING)
            .toolDockerImage(TOOL_DOCKER_IMAGE)
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .dataprocConfig(dataprocConfig)
            .createdDate(createdDate);

    testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID)
            .setCdrVersion(cdrVersion);
    doReturn(testWorkspace).when(workspaceService).lookupWorkspaceByNamespace(WORKSPACE_NS);
    doReturn(Optional.of(testWorkspace)).when(workspaceDao).getByNamespace(WORKSPACE_NS);

    when(userDisksApi.listDisksByProject(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
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

  private void stubGetWorkspace(
      String workspaceNamespace, String googleProject, String firecloudName, String creator) {
    stubGetWorkspace(
        workspaceNamespace, googleProject, firecloudName, creator, WorkspaceAccessLevel.OWNER);
  }

  private void stubGetWorkspace(
      String workspaceNamespace,
      String googleProject,
      String firecloudName,
      String creator,
      WorkspaceAccessLevel accessLevel) {
    DbWorkspace w = new DbWorkspace();
    w.setWorkspaceNamespace(workspaceNamespace);
    w.setFirecloudName(firecloudName);
    w.setCdrVersion(cdrVersion);
    w.setGoogleProject(googleProject);
    when(workspaceDao.getRequired(workspaceNamespace, firecloudName)).thenReturn(w);
    when(workspaceDao.getByNamespace(workspaceNamespace)).thenReturn(Optional.of(w));
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

  private JSONObject dataUriToJson(String dataUri) {
    String b64 = dataUri.substring(dataUri.indexOf(',') + 1);
    byte[] raw = Base64.getUrlDecoder().decode(b64);
    return new JSONObject(new String(raw));
  }

  private String getRuntimeName() {
    return "all-of-us-".concat(Long.toString(user.getUserId()));
  }

  private String getPdName() {
    return "all-of-us-pd-".concat(Long.toString(user.getUserId()));
  }

  @Test
  public void testGetRuntime() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody()).isEqualTo(testRuntime);
  }

  @Test
  public void testGetRuntime_error() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
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
  public void testGetRuntime_errorNoMessages() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(LeonardoRuntimeStatus.ERROR).errors(null));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody())
        .isEqualTo(testRuntime.status(RuntimeStatus.ERROR));
  }

  @Test
  public void testGetRuntime_securitySuspended() throws ApiException {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThrows(
        FailedPreconditionException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_securitySuspendedElapsed() throws ApiException {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().minus(Duration.ofMinutes(20))));
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    runtimeController.getRuntime(WORKSPACE_NS);
  }

  @Test
  public void testGetRuntime_noLabel() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of());

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.HAILGENOMICANALYSIS);
  }

  @Test
  public void testGetRuntime_defaultLabel_hail() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of("all-of-us-config", "preset-hail-genomic-analysis"));

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.HAILGENOMICANALYSIS);
  }

  @Test
  public void testGetRuntime_defaultLabel_generalAnalysis() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of("all-of-us-config", "preset-general-analysis"));

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.GENERALANALYSIS);
  }

  @Test
  public void testGetRuntime_overrideLabel() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of("all-of-us-config", "user-override"));

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.USEROVERRIDE);
  }

  @Test
  public void testGetRuntime_noGetRuntime_emptyListRuntimes() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(Collections.emptyList());

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_fromListRuntimes() throws ApiException {
    String timestamp = "2020-09-13T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .cloudContext(
                        new LeonardoCloudContext()
                            .cloudProvider(LeonardoCloudProvider.GCP)
                            .cloudResource("google-project"))
                    .runtimeName("expected-runtime")
                    .status(LeonardoRuntimeStatus.CREATING)
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    Runtime runtime = runtimeController.getRuntime(WORKSPACE_NS).getBody();

    assertThat(runtime.getRuntimeName()).isEqualTo("expected-runtime");
    assertThat(runtime.getGoogleProject()).isEqualTo("google-project");
    assertThat(runtime.getStatus()).isEqualTo(RuntimeStatus.DELETED);
    assertThat(runtime.getConfigurationType()).isEqualTo(RuntimeConfigurationType.USEROVERRIDE);
    assertThat(runtime.getCreatedDate()).isEqualTo(timestamp);
  }

  @Test
  public void testGetRuntime_fromListRuntimes_invalidRuntime() throws ApiException {
    dataprocConfigObj.put("cloudService", "notACloudService");
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeConfig(dataprocConfigObj)
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_fromListRuntimes_gceConfig() throws ApiException {
    String timestamp = "2020-09-13T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeConfig(gceConfigObj)
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    Runtime runtime = runtimeController.getRuntime(WORKSPACE_NS).getBody();

    assertThat(runtime.getGceConfig()).isEqualTo(gceConfig);
    assertThat(runtime.getDataprocConfig()).isNull();
  }

  @Test
  public void testGetRuntime_fromListRuntimes_dataprocConfig() throws ApiException {
    String timestamp = "2020-09-13T19:19:57.347Z";

    LinkedTreeMap<String, Object> dataProcConfigObj = new LinkedTreeMap<>();
    dataProcConfigObj.put("cloudService", "DATAPROC");
    dataProcConfigObj.put("masterDiskSize", 50.0);
    dataProcConfigObj.put("masterMachineType", "n1-standard-4");
    dataProcConfigObj.put("numberOfPreemptibleWorkers", 4);
    dataProcConfigObj.put("numberOfWorkerLocalSSDs", 8);
    dataProcConfigObj.put("numberOfWorkers", 3);
    dataProcConfigObj.put("workerDiskSize", 30);
    dataProcConfigObj.put("workerMachineType", "n1-standard-2");

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeConfig(dataProcConfigObj)
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    Runtime runtime = runtimeController.getRuntime(WORKSPACE_NS).getBody();

    assertThat(runtime.getDataprocConfig().getMasterDiskSize()).isEqualTo(50);
    assertThat(runtime.getDataprocConfig().getMasterMachineType()).isEqualTo("n1-standard-4");
    assertThat(runtime.getDataprocConfig().getNumberOfPreemptibleWorkers()).isEqualTo(4);
    assertThat(runtime.getDataprocConfig().getNumberOfWorkerLocalSSDs()).isEqualTo(8);
    assertThat(runtime.getDataprocConfig().getNumberOfWorkers()).isEqualTo(3);
    assertThat(runtime.getDataprocConfig().getWorkerDiskSize()).isEqualTo(30);
    assertThat(runtime.getDataprocConfig().getWorkerMachineType()).isEqualTo("n1-standard-2");
    assertThat(runtime.getGceConfig()).isNull();
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent() throws ApiException {
    String olderTimestamp = "2020-09-13T19:19:57.347Z";
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("expected-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override")),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(olderTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent_nullAuditInfo() throws ApiException {
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("expected-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override")),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent_nullTimestamp() throws ApiException {
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("expected-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override")),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(null))
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent_emptyTimestamp() throws ApiException {
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("expected-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override")),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(""))
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntime_mostRecentIsDefaultLabel() throws ApiException {
    String olderTimestamp = "2020-09-13T19:19:57.347Z";
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("override-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(olderTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override")),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_fromListRuntime_mostRecentHasNoLabel() throws ApiException {
    String olderTimestamp = "2020-09-13T19:19:57.347Z";
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("override-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp)),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(olderTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void testGetRuntime_fromListRuntime_returnPresets() throws ApiException {
    String timestamp = "2020-11-30T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(GOOGLE_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("preset-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "preset-general-analysis"))));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getRuntimeName())
        .isEqualTo("preset-runtime");
  }

  @Test
  public void testGetRuntime_gceConfig() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.runtimeConfig(gceConfigObj));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody())
        .isEqualTo(testRuntime.dataprocConfig(null).gceConfig(gceConfig));
  }

  @Test
  public void testGetRuntime_diskConfig() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(
            testLeoRuntime
                .runtimeConfig(gceConfigObj)
                .diskConfig(
                    new LeonardoDiskConfig()
                        .diskType(LeonardoDiskType.SSD)
                        .name("pd")
                        .blockSize(100)
                        .size(200)));

    Runtime runtime = runtimeController.getRuntime(WORKSPACE_NS).getBody();

    assertThat(runtime.getGceWithPdConfig().getPersistentDisk())
        .isEqualTo(new PersistentDiskRequest().diskType(DiskType.SSD).name("pd").size(200));
  }

  @Test
  public void testGetRuntime_UnknownStatus() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(null));

    assertThat(runtimeController.getRuntime(WORKSPACE_NS).getBody().getStatus())
        .isEqualTo(RuntimeStatus.UNKNOWN);
  }

  @Test
  public void testGetRuntime_NullBillingProject() {
    doThrow(new NotFoundException()).when(workspaceService).lookupWorkspaceByNamespace("123");
    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime("123"));
  }

  @Test
  public void testCreateRuntime_customRuntimeEnabled_noRuntimes() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    assertThrows(
        BadRequestException.class,
        () -> runtimeController.createRuntime(WORKSPACE_NS, new Runtime()));
  }

  @Test
  public void testCreateRuntime_customRuntimeEnabled_twoRuntimes() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

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
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

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

    verify(userRuntimesApi)
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
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard")));

    verify(userRuntimesApi)
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
    assertThat(createLeonardoGceConfig.getZone()).isEqualTo("us-central-1");
  }

  @Test
  public void testCreateRuntime_gceWithPD() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

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
                            .size(500))));

    Map<String, String> diskLabels = new HashMap<>();
    diskLabels.put(LEONARDO_LABEL_IS_RUNTIME, LEONARDO_LABEL_IS_RUNTIME_TRUE);

    verify(userRuntimesApi)
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
    assertThat(createLeonardoGceWithPdConfig.getZone()).isEqualTo("us-central-1");
  }

  @Test
  public void testCreateRuntimeFail_newPdp_pdAlreadyExist() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    LeonardoListPersistentDiskResponse gceDisk =
        new LeonardoListPersistentDiskResponse()
            .name("123")
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(LeonardoDiskStatus.READY);
    when(userDisksApi.listDisksByProject(any(), any(), any(), any(), any()))
        .thenReturn(ImmutableList.of(gceDisk));

    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

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
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    BadRequestException e =
        assertThrows(
            BadRequestException.class, () -> runtimeController.createRuntime(WORKSPACE_NS, null));

    assertThat(e)
        .hasMessageThat()
        .contains("Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
  }

  @Test
  public void testCreateRuntime_emptyRuntime() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    BadRequestException e =
        assertThrows(
            BadRequestException.class,
            () -> runtimeController.createRuntime(WORKSPACE_NS, new Runtime()));

    assertThat(e)
        .hasMessageThat()
        .contains("Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
  }

  @Test
  public void testCreateRuntime_defaultLabel_hail() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .configurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS)
            .dataprocConfig(testRuntime.getDataprocConfig()));
    verify(userRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("preset-hail-genomic-analysis");
  }

  @Test
  public void testCreateRuntime_defaultLabel_generalAnalysis() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .configurationType(RuntimeConfigurationType.GENERALANALYSIS)
            .dataprocConfig(dataprocConfig));
    verify(userRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("preset-general-analysis");
  }

  @Test
  public void testCreateRuntime_overrideLabel() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .configurationType(RuntimeConfigurationType.USEROVERRIDE)
            .dataprocConfig(dataprocConfig));
    verify(userRuntimesApi)
        .createRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("user-override");
  }

  @Test
  public void testCreateRuntime_gceWithGpu() throws ApiException {
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime()
            .gceConfig(
                new GceConfig()
                    .diskSize(50)
                    .machineType("standard")
                    .gpuConfig(new GpuConfig().gpuType("nvidia-tesla-t4").numOfGpus(2))));

    verify(userRuntimesApi)
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
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

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
                    .gpuConfig(new GpuConfig().gpuType("nvidia-tesla-t4").numOfGpus(2))));

    verify(userRuntimesApi)
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
                new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard"))));
  }

  @Test
  public void testCreateRuntime_terraWorkspaceV1() throws ApiException {
    // namespace == project, for v1 workspaces
    when(userRuntimesApi.getRuntime(WORKSPACE_NS, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_NS, WORKSPACE_ID, "test", WorkspaceAccessLevel.WRITER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard")));

    verify(userRuntimesApi)
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
    // namespace == project, for v1 workspaces
    when(userRuntimesApi.getRuntime(WORKSPACE_NS, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_NS, WORKSPACE_ID, "test", WorkspaceAccessLevel.OWNER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard")));

    verify(userRuntimesApi)
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
    when(userRuntimesApi.getRuntime(GOOGLE_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(
        WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test", WorkspaceAccessLevel.WRITER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard")));

    verify(userRuntimesApi)
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
    // namespace == project, for v1 workspaces
    when(userRuntimesApi.getRuntime(WORKSPACE_NS, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_NS, WORKSPACE_ID, "test", WorkspaceAccessLevel.WRITER);

    runtimeController.createRuntime(
        WORKSPACE_NS,
        new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard")));

    verify(userRuntimesApi)
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
    stubGetWorkspace(WORKSPACE_NS, GOOGLE_PROJECT_ID, WORKSPACE_ID, "test");

    runtimeController.updateRuntime(
        WORKSPACE_NS,
        new UpdateRuntimeRequest()
            .runtime(
                new Runtime()
                    .configurationType(RuntimeConfigurationType.USEROVERRIDE)
                    .dataprocConfig(dataprocConfig)));
    verify(userRuntimesApi)
        .updateRuntime(
            eq(GOOGLE_PROJECT_ID), eq(getRuntimeName()), updateRuntimeRequestCaptor.capture());

    LeonardoMachineConfig actualRuntimeConfig =
        (LeonardoMachineConfig) updateRuntimeRequestCaptor.getValue().getRuntimeConfig();
    assertThat(actualRuntimeConfig.getCloudService().getValue()).isEqualTo("DATAPROC");
    assertThat(actualRuntimeConfig.getNumberOfWorkers())
        .isEqualTo(dataprocConfig.getNumberOfWorkers());
    assertThat(actualRuntimeConfig.getMasterMachineType())
        .isEqualTo(dataprocConfig.getMasterMachineType());
    assertThat(actualRuntimeConfig.getMasterDiskSize())
        .isEqualTo(dataprocConfig.getMasterDiskSize());

    assertThat(updateRuntimeRequestCaptor.getValue().getLabelsToUpsert())
        .isEqualTo(
            Collections.singletonMap(
                LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG,
                LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP.get(
                    RuntimeConfigurationType.USEROVERRIDE)));
  }

  @Test
  public void testDeleteRuntime() throws ApiException {
    runtimeController.deleteRuntime(WORKSPACE_NS, false);
    verify(userRuntimesApi).deleteRuntime(GOOGLE_PROJECT_ID, getRuntimeName(), false);
  }

  @Test
  public void testLocalize_securitySuspended() {
    user.setComputeSecuritySuspendedUntil(
        Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(Duration.ofMinutes(5))));

    assertThrows(
        FailedPreconditionException.class,
        () -> runtimeController.localize(WORKSPACE_NS, new RuntimeLocalizeRequest()));
  }

  @Test
  public void localize_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceAuthService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();
    assertThrows(ForbiddenException.class, () -> runtimeController.localize(WORKSPACE_NS, req));
  }

  @Test
  public void localize_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();

    assertThrows(ForbiddenException.class, () -> runtimeController.localize(WORKSPACE_NS, req));
    verify(mockWorkspaceAuthService, never()).validateActiveBilling(anyString(), anyString());
  }

  private void createUser(String email) {
    DbUser user = new DbUser();
    user.setGivenName("first");
    user.setFamilyName("last");
    user.setUsername(email);
    userDao.save(user);
  }
}
