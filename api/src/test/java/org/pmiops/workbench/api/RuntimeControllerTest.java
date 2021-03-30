package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.Date;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.model.UpdateRuntimeRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClientImpl;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RuntimeControllerTest {

  private static final String BILLING_PROJECT_ID = "aou-rw-1234";
  private static final String BILLING_PROJECT_ID_2 = "aou-rw-5678";
  // a workspace's namespace is always its billing project ID
  private static final String WORKSPACE_NS = BILLING_PROJECT_ID;
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
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String API_HOST = "api.stable.fake-research-aou.org";
  private static final String API_BASE_URL = "https://" + API_HOST;
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
    LeonardoNotebooksClientImpl.class,
    NotebooksRetryHandler.class,
    LeonardoRetryHandler.class,
    NoBackOffPolicy.class,
    AccessTierServiceImpl.class,
  })
  @MockBean({ConceptSetService.class, CohortService.class})
  static class Configuration {

    @Bean
    @Scope("prototype")
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return user;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }
  }

  @Captor private ArgumentCaptor<Localize> welderReqCaptor;
  @Captor private ArgumentCaptor<LeonardoCreateRuntimeRequest> createRuntimeRequestCaptor;
  @Captor private ArgumentCaptor<LeonardoUpdateRuntimeRequest> updateRuntimeRequestCaptor;

  @MockBean AdminActionHistoryDao mockAdminActionHistoryDao;
  @MockBean LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockBean ComplianceService mockComplianceService;
  @MockBean DirectoryService mockDirectoryService;
  @MockBean FireCloudService mockFireCloudService;
  @MockBean UserRecentResourceService mockUserRecentResourceService;
  @MockBean UserServiceAuditor mockUserServiceAuditor;
  @MockBean WorkspaceService mockWorkspaceService;
  @MockBean WorkspaceAuthService mockWorkspaceAuthService;

  @Qualifier(NotebooksConfig.USER_RUNTIMES_API)
  @MockBean
  RuntimesApi userRuntimesApi;

  @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API)
  @MockBean
  RuntimesApi serviceRuntimesApi;

  @MockBean ProxyApi proxyApi;

  @Autowired CdrVersionDao cdrVersionDao;
  @MockBean WorkspaceDao workspaceDao;
  @Autowired UserDao userDao;
  @Autowired RuntimeController runtimeController;
  @Autowired LeonardoMapper leonardoMapper;

  private DbCdrVersion cdrVersion;
  private LeonardoGetRuntimeResponse testLeoRuntime;
  private LeonardoGetRuntimeResponse testLeoRuntime2;
  private LeonardoGetRuntimeResponse testLeoRuntimeDifferentProject;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse2;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponseDifferentProject;

  private Runtime testRuntime;
  private DbWorkspace testWorkspace;

  private DataprocConfig dataprocConfig;
  private LinkedTreeMap<String, Object> dataprocConfigObj;

  private GceConfig gceConfig;
  private LinkedTreeMap<String, Object> gceConfigObj;

  @Before
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();
    config.server.apiBaseUrl = API_BASE_URL;
    config.firecloud.registeredDomainName = "";
    config.firecloud.notebookRuntimeDefaultMachineType = "n1-standard-4";
    config.firecloud.notebookRuntimeDefaultDiskSizeGb = 50;
    config.access.enableComplianceTraining = true;

    user = new DbUser();
    user.setUsername(LOGGED_IN_USER_EMAIL);
    user.setUserId(123L);

    createUser(OTHER_USER_EMAIL);

    cdrVersion = new DbCdrVersion();
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setBigqueryDataset(BIGQUERY_DATASET);

    String createdDate = Date.fromYearMonthDay(1988, 12, 26).toString();

    Runtime tmpRuntime = new Runtime();

    dataprocConfigObj = new LinkedTreeMap<>();
    dataprocConfigObj.put("cloudService", "DATAPROC");
    dataprocConfigObj.put("numberOfWorkers", 0);
    dataprocConfigObj.put("masterMachineType", "n1-standard-4");
    dataprocConfigObj.put("masterDiskSize", 50.0);

    leonardoMapper.mapRuntimeConfig(tmpRuntime, dataprocConfigObj);
    dataprocConfig = tmpRuntime.getDataprocConfig();

    gceConfigObj = new LinkedTreeMap<>();
    gceConfigObj.put("cloudService", "GCE");
    gceConfigObj.put("bootDiskSize", 10.0);
    gceConfigObj.put("diskSize", 100.0);
    gceConfigObj.put("machineType", "n1-standard-2");

    leonardoMapper.mapRuntimeConfig(tmpRuntime, gceConfigObj);
    gceConfig = tmpRuntime.getGceConfig();

    testLeoRuntime =
        new LeonardoGetRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(LeonardoRuntimeStatus.DELETING)
            .runtimeImages(Collections.singletonList(RUNTIME_IMAGE))
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .runtimeConfig(dataprocConfigObj)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));
    testLeoListRuntimeResponse =
        new LeonardoListRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);
    testRuntime =
        new Runtime()
            .runtimeName(getRuntimeName())
            .configurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS)
            .googleProject(BILLING_PROJECT_ID)
            .status(RuntimeStatus.DELETING)
            .toolDockerImage(TOOL_DOCKER_IMAGE)
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .dataprocConfig(dataprocConfig)
            .createdDate(createdDate);

    testLeoRuntime2 =
        new LeonardoGetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME)
            .googleProject(BILLING_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));

    testLeoListRuntimeResponse2 =
        new LeonardoListRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME)
            .googleProject(BILLING_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);

    testLeoRuntimeDifferentProject =
        new LeonardoGetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT)
            .googleProject(BILLING_PROJECT_ID_2)
            .status(LeonardoRuntimeStatus.RUNNING)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));

    testLeoListRuntimeResponseDifferentProject =
        new LeonardoListRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT)
            .googleProject(BILLING_PROJECT_ID_2)
            .status(LeonardoRuntimeStatus.RUNNING);

    testWorkspace = new DbWorkspace();
    testWorkspace.setWorkspaceNamespace(WORKSPACE_NS);
    testWorkspace.setName(WORKSPACE_NAME);
    testWorkspace.setFirecloudName(WORKSPACE_ID);
    testWorkspace.setCdrVersion(cdrVersion);
    doReturn(Optional.of(testWorkspace)).when(workspaceDao).getByNamespace(WORKSPACE_NS);
  }

  private FirecloudWorkspace createFcWorkspace(String ns, String name, String creator) {
    return new FirecloudWorkspace()
        .namespace(ns)
        .name(name)
        .createdBy(creator)
        .bucketName(BUCKET_NAME);
  }

  private void stubGetWorkspace(String workspaceNamespace, String firecloudName, String creator) {
    DbWorkspace w = new DbWorkspace();
    w.setWorkspaceNamespace(workspaceNamespace);
    w.setFirecloudName(firecloudName);
    w.setCdrVersion(cdrVersion);
    when(workspaceDao.getRequired(workspaceNamespace, firecloudName)).thenReturn(w);
    when(workspaceDao.getByNamespace(workspaceNamespace)).thenReturn(Optional.of(w));
    stubGetFcWorkspace(createFcWorkspace(workspaceNamespace, firecloudName, creator));
  }

  private void stubGetFcWorkspace(FirecloudWorkspace fcWorkspace) {
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
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

  @Test
  public void testGetRuntime() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody()).isEqualTo(testRuntime);
  }

  @Test
  public void testGetRuntime_noLabel() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of());

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.HAILGENOMICANALYSIS);
  }

  @Test
  public void testGetRuntime_defaultLabel_hail() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of("all-of-us-config", "preset-hail-genomic-analysis"));

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.HAILGENOMICANALYSIS);
  }

  @Test
  public void testGetRuntime_defaultLabel_generalAnalysis() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of("all-of-us-config", "preset-general-analysis"));

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.GENERALANALYSIS);
  }

  @Test
  public void testGetRuntime_overrideLabel() throws ApiException {
    testLeoRuntime.setLabels(ImmutableMap.of("all-of-us-config", "user-override"));

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getConfigurationType())
        .isEqualTo(RuntimeConfigurationType.USEROVERRIDE);
  }

  @Test
  public void testGetRuntime_noGetRuntime_emptyListRuntimes() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(Collections.emptyList());

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(BILLING_PROJECT_ID));
  }

  @Test
  public void testGetRuntime_fromListRuntimes() throws ApiException {
    String timestamp = "2020-09-13T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .googleProject("google-project")
                    .runtimeName("expected-runtime")
                    .status(LeonardoRuntimeStatus.CREATING)
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    Runtime runtime = runtimeController.getRuntime(BILLING_PROJECT_ID).getBody();

    assertThat(runtime.getRuntimeName()).isEqualTo("expected-runtime");
    assertThat(runtime.getGoogleProject()).isEqualTo("google-project");
    assertThat(runtime.getStatus()).isEqualTo(RuntimeStatus.DELETED);
    assertThat(runtime.getConfigurationType()).isEqualTo(RuntimeConfigurationType.USEROVERRIDE);
    assertThat(runtime.getCreatedDate()).isEqualTo(timestamp);
  }

  @Test
  public void testGetRuntime_fromListRuntimes_invalidRuntime() throws ApiException {
    dataprocConfigObj.put("cloudService", "notACloudService");
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeConfig(dataprocConfigObj)
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(BILLING_PROJECT_ID));
  }

  @Test
  public void testGetRuntime_fromListRuntimes_gceConfig() throws ApiException {
    String timestamp = "2020-09-13T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeConfig(gceConfigObj)
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    Runtime runtime = runtimeController.getRuntime(BILLING_PROJECT_ID).getBody();

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

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeConfig(dataProcConfigObj)
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override"))));

    Runtime runtime = runtimeController.getRuntime(BILLING_PROJECT_ID).getBody();

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

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
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

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent_nullAuditInfo() throws ApiException {
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("expected-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "user-override")),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent_nullTimestamp() throws ApiException {
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
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

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntimes_checkMostRecent_emptyTimestamp() throws ApiException {
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
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

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getRuntimeName())
        .isEqualTo("expected-runtime");
  }

  @Test
  public void testGetRuntime_fromListRuntime_mostRecentIsDefaultLabel() throws ApiException {
    String olderTimestamp = "2020-09-13T19:19:57.347Z";
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
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

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(BILLING_PROJECT_ID));
  }

  @Test
  public void testGetRuntime_fromListRuntime_mostRecentHasNoLabel() throws ApiException {
    String olderTimestamp = "2020-09-13T19:19:57.347Z";
    String newerTimestamp = "2020-09-14T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("override-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(newerTimestamp)),
                new LeonardoListRuntimeResponse()
                    .runtimeName("default-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(olderTimestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "default"))));

    assertThrows(NotFoundException.class, () -> runtimeController.getRuntime(BILLING_PROJECT_ID));
  }

  @Test
  public void testGetRuntime_fromListRuntime_returnPresets() throws ApiException {
    String timestamp = "2020-11-30T19:19:57.347Z";

    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new ApiException(404, "Not found"));
    when(userRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, true))
        .thenReturn(
            ImmutableList.of(
                new LeonardoListRuntimeResponse()
                    .runtimeName("preset-runtime")
                    .auditInfo(new LeonardoAuditInfo().createdDate(timestamp))
                    .labels(ImmutableMap.of("all-of-us-config", "preset-general-analysis"))));

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getRuntimeName())
        .isEqualTo("preset-runtime");
  }

  @Test
  public void testGetRuntime_gceConfig() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.runtimeConfig(gceConfigObj));

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody())
        .isEqualTo(testRuntime.dataprocConfig(null).gceConfig(gceConfig));
  }

  @Test
  public void testGetRuntime_UnknownStatus() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(null));

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getStatus())
        .isEqualTo(RuntimeStatus.UNKNOWN);
  }

  @Test(expected = NotFoundException.class)
  public void testGetRuntime_NullBillingProject() {
    runtimeController.getRuntime(null);
  }

  @Test
  public void testDeleteRuntimesInProject() throws ApiException {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(serviceRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, false))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID,
        new ListRuntimeDeleteRequest()
            .runtimesToDelete(ImmutableList.of(testLeoRuntime.getRuntimeName())));
    verify(serviceRuntimesApi)
        .deleteRuntime(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName(), false);
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(LeonardoListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteSome() throws ApiException {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete = ImmutableList.of(testLeoRuntime.getRuntimeName());
    when(serviceRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, false))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(runtimesToDelete));
    verify(serviceRuntimesApi, times(runtimesToDelete.size()))
        .deleteRuntime(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName(), false);
    verify(mockLeonardoRuntimeAuditor, times(1))
        .fireDeleteRuntimesInProject(BILLING_PROJECT_ID, runtimesToDelete);
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteDoesNotAffectOtherProjects() throws ApiException {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete =
        ImmutableList.of(testLeoRuntimeDifferentProject.getRuntimeName());
    when(serviceRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, false))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(runtimesToDelete));
    verify(serviceRuntimesApi, times(0))
        .deleteRuntime(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName(), false);
    verify(mockLeonardoRuntimeAuditor, times(0))
        .fireDeleteRuntimesInProject(BILLING_PROJECT_ID, runtimesToDelete);
  }

  @Test
  public void testDeleteRuntimesInProject_NoRuntimes() throws ApiException {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(serviceRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, false))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(ImmutableList.of()));
    verify(serviceRuntimesApi, never())
        .deleteRuntime(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName(), false);
    verify(mockLeonardoRuntimeAuditor, never())
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(LeonardoListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_NullRuntimesList() throws ApiException {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(serviceRuntimesApi.listRuntimesByProject(BILLING_PROJECT_ID, null, false))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(null));
    verify(serviceRuntimesApi)
        .deleteRuntime(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName(), false);
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(LeonardoListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testCreateRuntime_customRuntimeEnabled_noRuntimes() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    assertThrows(
        BadRequestException.class,
        () -> runtimeController.createRuntime(BILLING_PROJECT_ID, new Runtime()));
  }

  @Test
  public void testCreateRuntime_customRuntimeEnabled_twoRuntimes() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    assertThrows(
        BadRequestException.class,
        () ->
            runtimeController.createRuntime(
                BILLING_PROJECT_ID,
                new Runtime()
                    .dataprocConfig(new DataprocConfig().masterMachineType("standard"))
                    .gceConfig(new GceConfig().machineType("standard"))));
  }

  @Test
  public void testCreateRuntime_dataproc() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID,
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
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

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
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID,
        new Runtime().gceConfig(new GceConfig().diskSize(50).machineType("standard")));

    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

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
  }

  @Test
  public void testCreateRuntime_nullRuntime() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    BadRequestException e =
        assertThrows(
            BadRequestException.class,
            () -> runtimeController.createRuntime(BILLING_PROJECT_ID, null));

    assertThat(e)
        .hasMessageThat()
        .contains("Either a GceConfig or DataprocConfig must be provided");
  }

  @Test
  public void testCreateRuntime_emptyRuntime() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    BadRequestException e =
        assertThrows(
            BadRequestException.class,
            () -> runtimeController.createRuntime(BILLING_PROJECT_ID, new Runtime()));

    assertThat(e)
        .hasMessageThat()
        .contains("Either a GceConfig or DataprocConfig must be provided");
  }

  @Test
  public void testCreateRuntime_defaultLabel_hail() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID,
        new Runtime()
            .configurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS)
            .dataprocConfig(testRuntime.getDataprocConfig()));
    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("preset-hail-genomic-analysis");
  }

  @Test
  public void testCreateRuntime_defaultLabel_generalAnalysis() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID,
        new Runtime()
            .configurationType(RuntimeConfigurationType.GENERALANALYSIS)
            .dataprocConfig(dataprocConfig));
    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("preset-general-analysis");
  }

  @Test
  public void testCreateRuntime_overrideLabel() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID,
        new Runtime()
            .configurationType(RuntimeConfigurationType.USEROVERRIDE)
            .dataprocConfig(dataprocConfig));
    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("user-override");
  }

  @Test
  public void testUpdateRuntime() throws ApiException {
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.updateRuntime(
        BILLING_PROJECT_ID,
        new UpdateRuntimeRequest()
            .runtime(
                new Runtime()
                    .configurationType(RuntimeConfigurationType.USEROVERRIDE)
                    .dataprocConfig(dataprocConfig)));
    verify(userRuntimesApi)
        .updateRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), updateRuntimeRequestCaptor.capture());

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
                LeonardoMapper.RUNTIME_LABEL_AOU_CONFIG,
                LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP.get(
                    RuntimeConfigurationType.USEROVERRIDE)));
  }

  @Test
  public void testDeleteRuntime() throws ApiException {
    runtimeController.deleteRuntime(BILLING_PROJECT_ID);
    verify(userRuntimesApi).deleteRuntime(BILLING_PROJECT_ID, getRuntimeName(), false);
  }

  @Test
  public void testLocalize() throws org.pmiops.workbench.notebooks.ApiException {
    RuntimeLocalizeRequest req =
        new RuntimeLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize(BILLING_PROJECT_ID, req).getBody();
    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces/myfirstworkspace");

    verify(proxyApi)
        .welderLocalize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), welderReqCaptor.capture());

    Localize welderReq = welderReqCaptor.getValue();
    assertThat(
            welderReq.getEntries().stream()
                .map(e -> e.getLocalDestinationPath())
                .collect(Collectors.toList()))
        .containsExactly(
            "workspaces/myfirstworkspace/foo.ipynb",
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");

    assertThat(
            welderReq.getEntries().stream()
                .filter(
                    e ->
                        e.getSourceUri().equals("gs://workspace-bucket/notebooks/foo.ipynb")
                            && e.getLocalDestinationPath()
                                .equals("workspaces/myfirstworkspace/foo.ipynb"))
                .count())
        .isAtLeast(1L);

    JSONObject aouJson =
        dataUriToJson(
            welderReq.getEntries().stream()
                .filter(
                    e ->
                        e.getLocalDestinationPath()
                            .equals("workspaces/myfirstworkspace/.all_of_us_config.json"))
                .findFirst()
                .get()
                .getSourceUri());

    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(BILLING_PROJECT_ID);
    assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST);
    verify(mockUserRecentResourceService, times(1))
        .updateNotebookEntry(anyLong(), anyLong(), anyString());
  }

  @Test
  public void testLocalize_playgroundMode() throws org.pmiops.workbench.notebooks.ApiException {
    RuntimeLocalizeRequest req =
        new RuntimeLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(true);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize(BILLING_PROJECT_ID, req).getBody();
    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces_playground/myfirstworkspace");
    verify(proxyApi)
        .welderLocalize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), welderReqCaptor.capture());

    Localize welderReq = welderReqCaptor.getValue();

    assertThat(
            welderReq.getEntries().stream()
                .map(e -> e.getLocalDestinationPath())
                .collect(Collectors.toList()))
        .containsExactly(
            "workspaces_playground/myfirstworkspace/foo.ipynb",
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");

    assertThat(
            welderReq.getEntries().stream()
                .filter(
                    e ->
                        e.getLocalDestinationPath()
                                .equals("workspaces_playground/myfirstworkspace/foo.ipynb")
                            && e.getSourceUri().equals("gs://workspace-bucket/notebooks/foo.ipynb"))
                .count())
        .isAtLeast(1L);
  }

  @Test
  public void testLocalize_differentNamespace() throws org.pmiops.workbench.notebooks.ApiException {
    RuntimeLocalizeRequest req =
        new RuntimeLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    stubGetWorkspace("other-proj", "myotherworkspace", LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize("other-proj", req).getBody();
    verify(proxyApi)
        .welderLocalize(eq("other-proj"), eq(getRuntimeName()), welderReqCaptor.capture());

    Localize welderReq = welderReqCaptor.getValue();

    assertThat(
            welderReq.getEntries().stream()
                .map(e -> e.getLocalDestinationPath())
                .collect(Collectors.toList()))
        .containsExactly(
            "workspaces/myotherworkspace/foo.ipynb",
            "workspaces/myotherworkspace/.all_of_us_config.json",
            "workspaces_playground/myotherworkspace/.all_of_us_config.json");

    assertThat(
            welderReq.getEntries().stream()
                .filter(
                    e ->
                        e.getLocalDestinationPath().equals("workspaces/myotherworkspace/foo.ipynb")
                            && e.getSourceUri().equals("gs://workspace-bucket/notebooks/foo.ipynb"))
                .count())
        .isAtLeast(1L);

    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces/myotherworkspace");
    JSONObject aouJson =
        dataUriToJson(
            welderReq.getEntries().stream()
                .filter(
                    e ->
                        e.getLocalDestinationPath()
                            .equals("workspaces/myotherworkspace/.all_of_us_config.json"))
                .findFirst()
                .get()
                .getSourceUri());
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo("other-proj");
  }

  @Test
  public void testLocalize_noNotebooks() throws org.pmiops.workbench.notebooks.ApiException {
    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize(BILLING_PROJECT_ID, req).getBody();
    verify(proxyApi)
        .welderLocalize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), welderReqCaptor.capture());

    // Config files only.
    Localize welderReq = welderReqCaptor.getValue();

    assertThat(
            welderReq.getEntries().stream()
                .map(e -> e.getLocalDestinationPath())
                .collect(Collectors.toList()))
        .containsExactly(
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");

    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces/myfirstworkspace");
  }

  @Test
  public void getRuntime_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceAuthService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    assertThrows(ForbiddenException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void getRuntime_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    assertThrows(ForbiddenException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
    verify(mockWorkspaceAuthService, never()).validateActiveBilling(anyString(), anyString());
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
