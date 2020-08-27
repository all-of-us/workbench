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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
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
import org.pmiops.workbench.leonardo.model.AuditInfo;
import org.pmiops.workbench.leonardo.model.GetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.ListRuntimeResponse;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterConfig;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListClusterDeleteRequest;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
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
  private static final String EXTRA_CLUSTER_NAME_DIFFERENT_PROJECT = "all-of-us-different-project";

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
    LeonardoMapperImpl.class
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

  @Captor private ArgumentCaptor<Map<String, String>> mapCaptor;

  @MockBean AdminActionHistoryDao mockAdminActionHistoryDao;
  @MockBean LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockBean ComplianceService mockComplianceService;
  @MockBean DirectoryService mockDirectoryService;
  @MockBean FireCloudService mockFireCloudService;
  @MockBean LeonardoNotebooksClient mockLeoNotebooksClient;
  @MockBean UserRecentResourceService mockUserRecentResourceService;
  @MockBean UserServiceAuditor mockUserServiceAuditor;
  @MockBean WorkspaceService mockWorkspaceService;

  @Autowired UserDao userDao;
  @Autowired WorkspaceMapper workspaceMapper;
  @Autowired RuntimeController runtimeController;

  private DbCdrVersion cdrVersion;
  private GetRuntimeResponse testLeoRuntime;
  private GetRuntimeResponse testLeoRuntime2;
  private GetRuntimeResponse testLeoRuntimeDifferentProject;
  private org.pmiops.workbench.leonardo.model.ListRuntimeResponse testLeoListRuntimeResponse;
  private org.pmiops.workbench.leonardo.model.ListRuntimeResponse testLeoListRuntimeResponse2;
  private org.pmiops.workbench.leonardo.model.ListRuntimeResponse
      testLeoListRuntimeResponseDifferentProject;

  private Cluster testCluster;
  private Runtime testRuntime;
  private DbWorkspace testWorkspace;

  @Before
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();
    config.server.apiBaseUrl = API_BASE_URL;
    config.firecloud.registeredDomainName = "";
    config.firecloud.clusterDefaultMachineType = "n1-standard-4";
    config.firecloud.clusterDefaultDiskSizeGb = 50;
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

    testLeoRuntime =
        new GetRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.leonardo.model.RuntimeStatus.DELETING)
            .auditInfo(new AuditInfo().createdDate(createdDate));
    testLeoListRuntimeResponse =
        new org.pmiops.workbench.leonardo.model.ListRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.leonardo.model.RuntimeStatus.RUNNING);
    testCluster =
        new Cluster()
            .clusterName(getRuntimeName())
            .clusterNamespace(BILLING_PROJECT_ID)
            .status(ClusterStatus.DELETING)
            .createdDate(createdDate);
    testRuntime =
        new Runtime()
            .runtimeName(getRuntimeName())
            .googleProject(BILLING_PROJECT_ID)
            .status(RuntimeStatus.DELETING)
            .createdDate(createdDate);

    testLeoRuntime2 =
        new GetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME)
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.leonardo.model.RuntimeStatus.RUNNING)
            .auditInfo(new AuditInfo().createdDate(createdDate));

    testLeoListRuntimeResponse2 =
        new org.pmiops.workbench.leonardo.model.ListRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME)
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.leonardo.model.RuntimeStatus.RUNNING);

    testLeoRuntimeDifferentProject =
        new GetRuntimeResponse()
            .runtimeName(EXTRA_CLUSTER_NAME_DIFFERENT_PROJECT)
            .googleProject(BILLING_PROJECT_ID_2)
            .status(org.pmiops.workbench.leonardo.model.RuntimeStatus.RUNNING)
            .auditInfo(new AuditInfo().createdDate(createdDate));

    testLeoListRuntimeResponseDifferentProject =
        new org.pmiops.workbench.leonardo.model.ListRuntimeResponse()
            .runtimeName(EXTRA_CLUSTER_NAME_DIFFERENT_PROJECT)
            .googleProject(BILLING_PROJECT_ID_2)
            .status(org.pmiops.workbench.leonardo.model.RuntimeStatus.RUNNING);

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
  public void testGetRuntime() {
    when(mockLeoNotebooksClient.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody()).isEqualTo(testRuntime);
  }

  @Test
  public void testGetRuntime_UnknownStatus() {
    when(mockLeoNotebooksClient.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(null));

    assertThat(runtimeController.getRuntime(BILLING_PROJECT_ID).getBody().getStatus())
        .isEqualTo(RuntimeStatus.UNKNOWN);
  }

  @Test(expected = NotFoundException.class)
  public void testGetRuntime_NullBillingProject() {
    runtimeController.getRuntime(null);
  }

  @Test
  public void testDeleteRuntimesInProject() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID,
        new ListRuntimeDeleteRequest()
            .runtimesToDelete(ImmutableList.of(testLeoRuntime.getRuntimeName())));
    verify(mockLeoNotebooksClient)
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteSome() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete = ImmutableList.of(testLeoRuntime.getRuntimeName());
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(runtimesToDelete));
    verify(mockLeoNotebooksClient, times(runtimesToDelete.size()))
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, times(1))
        .fireDeleteRuntimesInProject(BILLING_PROJECT_ID, runtimesToDelete);
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteDoesNotAffectOtherProjects() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete =
        ImmutableList.of(testLeoRuntimeDifferentProject.getRuntimeName());
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(runtimesToDelete));
    verify(mockLeoNotebooksClient, times(0))
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, times(0))
        .fireDeleteRuntimesInProject(BILLING_PROJECT_ID, runtimesToDelete);
  }

  @Test
  public void testDeleteRuntimesInProject_NoRuntimes() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(ImmutableList.of()));
    verify(mockLeoNotebooksClient, never())
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, never())
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_NullRuntimesList() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteRuntimesInProject(
        BILLING_PROJECT_ID, new ListRuntimeDeleteRequest().runtimesToDelete(null));
    verify(mockLeoNotebooksClient)
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testCreateRuntime() {
    when(mockLeoNotebooksClient.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenThrow(new NotFoundException());
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    runtimeController.createRuntime(BILLING_PROJECT_ID);
    verify(mockLeoNotebooksClient)
        .createRuntime(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), eq(WORKSPACE_ID));
  }

  @Test
  public void testDeleteRuntime() {
    runtimeController.deleteRuntime(BILLING_PROJECT_ID);
    verify(mockLeoNotebooksClient).deleteRuntime(BILLING_PROJECT_ID, getRuntimeName());
  }

  @Test
  public void testLocalize() {
    RuntimeLocalizeRequest req =
        new RuntimeLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize(BILLING_PROJECT_ID, req).getBody();
    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces/myfirstworkspace");

    verify(mockLeoNotebooksClient)
        .localize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces/myfirstworkspace/foo.ipynb",
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces/myfirstworkspace/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    JSONObject aouJson =
        dataUriToJson(localizeMap.get("workspaces/myfirstworkspace/.all_of_us_config.json"));
    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(BILLING_PROJECT_ID);
    assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST);
    verify(mockUserRecentResourceService, times(1))
        .updateNotebookEntry(anyLong(), anyLong(), anyString());
  }

  @Test
  public void testLocalize_playgroundMode() {
    RuntimeLocalizeRequest req =
        new RuntimeLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(true);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize(BILLING_PROJECT_ID, req).getBody();
    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces_playground/myfirstworkspace");
    verify(mockLeoNotebooksClient)
        .localize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces_playground/myfirstworkspace/foo.ipynb",
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces_playground/myfirstworkspace/foo.ipynb",
            "gs://workspace-bucket/notebooks/foo.ipynb");
  }

  @Test
  public void testLocalize_differentNamespace() {
    RuntimeLocalizeRequest req =
        new RuntimeLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    stubGetWorkspace("other-proj", "myotherworkspace", LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize("other-proj", req).getBody();
    verify(mockLeoNotebooksClient)
        .localize(eq("other-proj"), eq(getRuntimeName()), mapCaptor.capture());

    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces/myotherworkspace/foo.ipynb",
            "workspaces/myotherworkspace/.all_of_us_config.json",
            "workspaces_playground/myotherworkspace/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces/myotherworkspace/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    assertThat(resp.getRuntimeLocalDirectory()).isEqualTo("workspaces/myotherworkspace");
    JSONObject aouJson =
        dataUriToJson(localizeMap.get("workspaces/myotherworkspace/.all_of_us_config.json"));
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo("other-proj");
  }

  @Test
  public void testLocalize_noNotebooks() {
    RuntimeLocalizeRequest req = new RuntimeLocalizeRequest();
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    RuntimeLocalizeResponse resp = runtimeController.localize(BILLING_PROJECT_ID, req).getBody();
    verify(mockLeoNotebooksClient)
        .localize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), mapCaptor.capture());

    // Config files only.
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
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

  // TODO(RW-5405): all below tests are deprecated, and will be removed after the next release.

  @Test
  public void testGetCluster() {
    when(mockLeoNotebooksClient.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);

    assertThat(runtimeController.getCluster(BILLING_PROJECT_ID).getBody()).isEqualTo(testCluster);
  }

  @Test
  public void testGetCluster_UnknownStatus() {
    when(mockLeoNotebooksClient.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime.status(null));

    assertThat(runtimeController.getCluster(BILLING_PROJECT_ID).getBody().getStatus())
        .isEqualTo(ClusterStatus.UNKNOWN);
  }

  @Test(expected = NotFoundException.class)
  public void testGetCluster_NullBillingProject() {
    runtimeController.getCluster(null);
  }

  @Test
  public void testDeleteClustersInProject() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteClustersInProject(
        BILLING_PROJECT_ID,
        new ListClusterDeleteRequest()
            .clustersToDelete(ImmutableList.of(testLeoRuntime.getRuntimeName())));
    verify(mockLeoNotebooksClient)
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteClustersInProject_DeleteSome() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> clustersToDelete = ImmutableList.of(testLeoRuntime.getRuntimeName());
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(clustersToDelete));
    verify(mockLeoNotebooksClient, times(clustersToDelete.size()))
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, times(1))
        .fireDeleteRuntimesInProject(BILLING_PROJECT_ID, clustersToDelete);
  }

  @Test
  public void testDeleteClustersInProject_DeleteDoesNotAffectOtherProjects() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> clustersToDelete =
        ImmutableList.of(testLeoRuntimeDifferentProject.getRuntimeName());
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(clustersToDelete));
    verify(mockLeoNotebooksClient, times(0))
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, times(0))
        .fireDeleteRuntimesInProject(BILLING_PROJECT_ID, clustersToDelete);
  }

  @Test
  public void testDeleteClustersInProject_NoClusters() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(ImmutableList.of()));
    verify(mockLeoNotebooksClient, never())
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, never())
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteClustersInProject_NullClustersList() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeoNotebooksClient.listRuntimesByProjectAsService(BILLING_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    runtimeController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(null));
    verify(mockLeoNotebooksClient)
        .deleteRuntimeAsService(BILLING_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            BILLING_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testCreateCluster() {
    when(mockLeoNotebooksClient.getRuntime(BILLING_PROJECT_ID, getRuntimeName()))
        .thenReturn(testLeoRuntime);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, "test");

    assertThat(runtimeController.createCluster(BILLING_PROJECT_ID).getBody())
        .isEqualTo(testCluster);
    verify(mockLeoNotebooksClient)
        .createRuntime(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), eq(WORKSPACE_ID));
  }

  @Test
  public void testDeleteCluster() {
    runtimeController.deleteCluster(BILLING_PROJECT_ID);
    verify(mockLeoNotebooksClient).deleteRuntime(BILLING_PROJECT_ID, getRuntimeName());
  }

  @Test
  public void testSetClusterConfig() {
    ResponseEntity<EmptyResponse> response =
        this.runtimeController.updateClusterConfig(
            new UpdateClusterConfigRequest()
                .userEmail(OTHER_USER_EMAIL)
                .clusterConfig(
                    new ClusterConfig().machineType("n1-standard-16").masterDiskSize(100)));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    DbUser updatedUser = userDao.findUserByUsername(OTHER_USER_EMAIL);
    assertThat(updatedUser.getClusterConfigDefault().machineType).isEqualTo("n1-standard-16");
    assertThat(updatedUser.getClusterConfigDefault().masterDiskSize).isEqualTo(100);
  }

  @Test
  public void testUpdateClusterConfigClear() {
    ResponseEntity<EmptyResponse> response =
        this.runtimeController.updateClusterConfig(
            new UpdateClusterConfigRequest()
                .userEmail(OTHER_USER_EMAIL)
                .clusterConfig(
                    new ClusterConfig().machineType("n1-standard-16").masterDiskSize(100)));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    response =
        this.runtimeController.updateClusterConfig(
            new UpdateClusterConfigRequest().userEmail(OTHER_USER_EMAIL).clusterConfig(null));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    DbUser updatedUser = userDao.findUserByUsername(OTHER_USER_EMAIL);
    assertThat(updatedUser.getClusterConfigDefault()).isNull();
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateClusterConfigUserNotFound() {
    this.runtimeController.updateClusterConfig(
        new UpdateClusterConfigRequest().userEmail("not-found@researchallofus.org"));
  }

  @Test
  public void testDeprecatedLocalize() {
    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        runtimeController.deprecatedLocalize(BILLING_PROJECT_ID, req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/myfirstworkspace");

    verify(mockLeoNotebooksClient)
        .localize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces/myfirstworkspace/foo.ipynb",
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces/myfirstworkspace/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    JSONObject aouJson =
        dataUriToJson(localizeMap.get("workspaces/myfirstworkspace/.all_of_us_config.json"));
    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(BILLING_PROJECT_ID);
    assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST);
    verify(mockUserRecentResourceService, times(1))
        .updateNotebookEntry(anyLong(), anyLong(), anyString());
  }

  @Test
  public void testDeprecatedLocalize_playgroundMode() {
    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(true);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        runtimeController.deprecatedLocalize(BILLING_PROJECT_ID, req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces_playground/myfirstworkspace");
    verify(mockLeoNotebooksClient)
        .localize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces_playground/myfirstworkspace/foo.ipynb",
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces_playground/myfirstworkspace/foo.ipynb",
            "gs://workspace-bucket/notebooks/foo.ipynb");
  }

  @Test
  public void testDeprecatedLocalize_differentNamespace() {
    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest()
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    stubGetWorkspace("other-proj", "myotherworkspace", LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        runtimeController.deprecatedLocalize("other-proj", req).getBody();
    verify(mockLeoNotebooksClient)
        .localize(eq("other-proj"), eq(getRuntimeName()), mapCaptor.capture());

    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces/myotherworkspace/foo.ipynb",
            "workspaces/myotherworkspace/.all_of_us_config.json",
            "workspaces_playground/myotherworkspace/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces/myotherworkspace/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/myotherworkspace");
    JSONObject aouJson =
        dataUriToJson(localizeMap.get("workspaces/myotherworkspace/.all_of_us_config.json"));
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo("other-proj");
  }

  @Test
  public void testDeprecatedLocalize_noNotebooks() {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        runtimeController.deprecatedLocalize(BILLING_PROJECT_ID, req).getBody();
    verify(mockLeoNotebooksClient)
        .localize(eq(BILLING_PROJECT_ID), eq(getRuntimeName()), mapCaptor.capture());

    // Config files only.
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces_playground/myfirstworkspace/.all_of_us_config.json",
            "workspaces/myfirstworkspace/.all_of_us_config.json");
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/myfirstworkspace");
  }

  @Test
  public void GetCluster_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    assertThrows(ForbiddenException.class, () -> runtimeController.getCluster(WORKSPACE_NS));
  }

  @Test
  public void getCluster_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
            WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    assertThrows(ForbiddenException.class, () -> runtimeController.getCluster(WORKSPACE_NS));
    verify(mockWorkspaceService, never()).validateActiveBilling(anyString(), anyString());
  }

  @Test
  public void deprecatedLocalize_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    assertThrows(
        ForbiddenException.class, () -> runtimeController.deprecatedLocalize(WORKSPACE_NS, req));
  }

  @Test
  public void deprecatedLocalize_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(mockWorkspaceService)
        .enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
            WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.WRITER);

    ClusterLocalizeRequest req = new ClusterLocalizeRequest();

    assertThrows(
        ForbiddenException.class, () -> runtimeController.deprecatedLocalize(WORKSPACE_NS, req));
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
