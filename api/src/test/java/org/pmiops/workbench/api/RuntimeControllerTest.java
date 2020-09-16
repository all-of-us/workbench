package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
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
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.RuntimeStatus;
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
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
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
    NoBackOffPolicy.class
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

  @MockBean AdminActionHistoryDao mockAdminActionHistoryDao;
  @MockBean LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockBean ComplianceService mockComplianceService;
  @MockBean DirectoryService mockDirectoryService;
  @MockBean FireCloudService mockFireCloudService;
  @MockBean UserRecentResourceService mockUserRecentResourceService;
  @MockBean UserServiceAuditor mockUserServiceAuditor;
  @MockBean WorkspaceService mockWorkspaceService;

  @Qualifier(NotebooksConfig.USER_RUNTIMES_API)
  @MockBean
  RuntimesApi userRuntimesApi;

  @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API)
  @MockBean
  RuntimesApi serviceRuntimesApi;

  @MockBean ProxyApi proxyApi;

  @Autowired UserDao userDao;
  @Autowired RuntimeController runtimeController;

  private DbCdrVersion cdrVersion;
  private LeonardoGetRuntimeResponse testLeoRuntime;
  private LeonardoGetRuntimeResponse testLeoRuntime2;
  private LeonardoGetRuntimeResponse testLeoRuntimeDifferentProject;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse2;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponseDifferentProject;

  private Runtime testRuntime;
  private DbWorkspace testWorkspace;

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

    DataprocConfig dataprocConfig =
        new DataprocConfig()
            .numberOfWorkers(0)
            .masterMachineType("n1-standard-4")
            .masterDiskSize(50);

    LinkedTreeMap<String, Object> dataProcConfigObj = new LinkedTreeMap<>();
    dataProcConfigObj.put("cloudService", "DATAPROC");
    dataProcConfigObj.put("numberOfWorkers", 0);
    dataProcConfigObj.put("masterMachineType", "n1-standard-4");
    dataProcConfigObj.put("masterDiskSize", 50.0);

    testLeoRuntime =
        new LeonardoGetRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(LeonardoRuntimeStatus.DELETING)
            .runtimeImages(Collections.singletonList(RUNTIME_IMAGE))
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .runtimeConfig(dataProcConfigObj)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));
    testLeoListRuntimeResponse =
        new LeonardoListRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);
    testRuntime =
        new Runtime()
            .runtimeName(getRuntimeName())
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
    doReturn(testWorkspace).when(mockWorkspaceService).get(WORKSPACE_NS, WORKSPACE_ID);
    doReturn(Optional.of(testWorkspace)).when(mockWorkspaceService).getByNamespace(WORKSPACE_NS);
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
    when(mockWorkspaceService.getRequired(workspaceNamespace, firecloudName)).thenReturn(w);
    when(mockWorkspaceService.getByNamespace(workspaceNamespace)).thenReturn(Optional.of(w));
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
  public void testGetRuntime_gceConfig() throws ApiException {
    GceConfig gceConfig =
        new GceConfig().bootDiskSize(10).diskSize(100).machineType("n1-standard-2");

    LinkedTreeMap<String, Object> gceConfigObj = new LinkedTreeMap<>();
    gceConfigObj.put("bootDiskSize", 10.0);
    gceConfigObj.put("diskSize", 100.0);
    gceConfigObj.put("machineType", "n1-standard-2");
    gceConfigObj.put("cloudService", "GCE");

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
  public void testCreateRuntime() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(BILLING_PROJECT_ID, null);
    verify(userRuntimesApi).createRuntime(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("default");
  }

  @Test
  public void testCreateRuntime_defaultLabel_dataproc() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID,
        new Runtime().configurationType(RuntimeConfigurationType.DEFAULTDATAPROC));
    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("default");
  }

  @Test
  public void testCreateRuntime_defaultLabel_gce() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID, new Runtime().configurationType(RuntimeConfigurationType.DEFAULTGCE));
    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("default");
  }

  @Test
  public void testCreateRuntime_overrideLabel() throws ApiException {
    when(userRuntimesApi.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(
        BILLING_PROJECT_ID, new Runtime().configurationType(RuntimeConfigurationType.USEROVERRIDE));
    verify(userRuntimesApi)
        .createRuntime(
            eq(BILLING_PROJECT_ID), eq(getRuntimeName()), createRuntimeRequestCaptor.capture());

    LeonardoCreateRuntimeRequest createRuntimeRequest = createRuntimeRequestCaptor.getValue();
    assertThat(((Map<String, String>) createRuntimeRequest.getLabels()).get("all-of-us-config"))
        .isEqualTo("user-override");
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
        .when(mockWorkspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    assertThrows(ForbiddenException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
  }

  @Test
  public void getRuntime_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
            WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    assertThrows(ForbiddenException.class, () -> runtimeController.getRuntime(WORKSPACE_NS));
    verify(mockWorkspaceService, never()).validateActiveBilling(anyString(), anyString());
  }

  @Test
  public void localize_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();
    assertThrows(ForbiddenException.class, () -> runtimeController.localize(WORKSPACE_NS, req));
  }

  @Test
  public void localize_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
            WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();

    assertThrows(ForbiddenException.class, () -> runtimeController.localize(WORKSPACE_NS, req));
    verify(mockWorkspaceService, never()).validateActiveBilling(anyString(), anyString());
  }

  private void createUser(String email) {
    DbUser user = new DbUser();
    user.setGivenName("first");
    user.setFamilyName("last");
    user.setUsername(email);
    userDao.save(user);
  }
}
