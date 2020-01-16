package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
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
import java.util.Random;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.auditors.ClusterAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FeatureFlagsConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
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
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterConfig;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListClusterDeleteRequest;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
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
public class ClusterControllerTest {

  private static final String BILLING_PROJECT_ID = "proj";
  private static final String BILLING_PROJECT_ID_2 = "proj2";
  // a workspace's namespace is always its billing project ID
  private static final String WORKSPACE_NS = BILLING_PROJECT_ID;
  private static final String WORKSPACE_ID = "wsid";
  private static final String WORKSPACE_NAME = "wsn";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String OTHER_USER_EMAIL = "alice@gmail.com";
  private static final String BUCKET_NAME = "workspace-bucket";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String API_HOST = "api.stable.fake-research-aou.org";
  private static final String API_BASE_URL = "https://" + API_HOST;
  private static final String BIGQUERY_DATASET = "dataset-name";
  private static final String EXTRA_CLUSTER_NAME = "all-of-us-extra";
  private static final String EXTRA_CLUSTER_NAME_DIFFERENT_PROJECT = "all-of-us-different-project";

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  @TestConfiguration
  @Import({ClusterController.class, UserServiceImpl.class})
  @MockBean({
    ClusterAuditor.class,
    FireCloudService.class,
    LeonardoNotebooksClient.class,
    WorkspaceService.class,
    UserRecentResourceService.class,
    ComplianceService.class,
    DirectoryService.class,
    AdminActionHistoryDao.class,
    UserServiceAuditor.class
  })
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

  @Autowired ClusterAuditor clusterAuditor;
  @Autowired LeonardoNotebooksClient notebookService;
  @Autowired FireCloudService fireCloudService;
  @Autowired UserDao userDao;
  @Autowired WorkspaceService workspaceService;
  @Autowired ClusterController clusterController;
  @Autowired UserRecentResourceService userRecentResourceService;
  @Autowired Clock clock;

  private DbCdrVersion cdrVersion;
  private org.pmiops.workbench.notebooks.model.Cluster testFcCluster;
  private org.pmiops.workbench.notebooks.model.Cluster testFcCluster2;
  private org.pmiops.workbench.notebooks.model.Cluster testFcClusterDifferentProject;
  private org.pmiops.workbench.notebooks.model.ListClusterResponse testFcClusterListResponse;
  private org.pmiops.workbench.notebooks.model.ListClusterResponse testFcClusterListResponse2;
  private org.pmiops.workbench.notebooks.model.ListClusterResponse
      testFcClusterListResponseDifferentProject;

  private Cluster testCluster;
  private DbWorkspace testWorkspace;

  @Before
  public void setUp() {
    config = new WorkbenchConfig();
    config.server = new WorkbenchConfig.ServerConfig();
    config.server.apiBaseUrl = API_BASE_URL;
    config.firecloud = new WorkbenchConfig.FireCloudConfig();
    config.firecloud.registeredDomainName = "";
    config.firecloud.clusterDefaultMachineType = "n1-standard-4";
    config.firecloud.clusterDefaultDiskSizeGb = 50;
    config.access = new WorkbenchConfig.AccessConfig();
    config.access.enableComplianceTraining = true;
    config.featureFlags = new FeatureFlagsConfig();

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

    testFcCluster =
        new org.pmiops.workbench.notebooks.model.Cluster()
            .clusterName(getClusterName())
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.notebooks.model.ClusterStatus.DELETING)
            .createdDate(createdDate);
    testFcClusterListResponse =
        new org.pmiops.workbench.notebooks.model.ListClusterResponse()
            .clusterName(getClusterName())
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING)
            .createdDate(createdDate);
    testCluster =
        new Cluster()
            .clusterName(getClusterName())
            .clusterNamespace(BILLING_PROJECT_ID)
            .status(ClusterStatus.DELETING)
            .createdDate(createdDate);

    testFcCluster2 =
        new org.pmiops.workbench.notebooks.model.Cluster()
            .clusterName(EXTRA_CLUSTER_NAME)
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING)
            .createdDate(createdDate);

    testFcClusterListResponse2 =
        new org.pmiops.workbench.notebooks.model.ListClusterResponse()
            .clusterName(EXTRA_CLUSTER_NAME)
            .googleProject(BILLING_PROJECT_ID)
            .status(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING)
            .createdDate(createdDate);

    testFcClusterDifferentProject =
        new org.pmiops.workbench.notebooks.model.Cluster()
            .clusterName(EXTRA_CLUSTER_NAME_DIFFERENT_PROJECT)
            .googleProject(BILLING_PROJECT_ID_2)
            .status(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING)
            .createdDate(createdDate);

    testFcClusterListResponseDifferentProject =
        new org.pmiops.workbench.notebooks.model.ListClusterResponse()
            .clusterName(EXTRA_CLUSTER_NAME_DIFFERENT_PROJECT)
            .googleProject(BILLING_PROJECT_ID_2)
            .status(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING)
            .createdDate(createdDate);

    testWorkspace = new DbWorkspace();
    testWorkspace.setWorkspaceNamespace(WORKSPACE_NS);
    testWorkspace.setName(WORKSPACE_NAME);
    testWorkspace.setFirecloudName(WORKSPACE_ID);
    testWorkspace.setCdrVersion(cdrVersion);
    doReturn(testWorkspace).when(workspaceService).get(WORKSPACE_NS, WORKSPACE_ID);
  }

  private FirecloudWorkspace createFcWorkspace(String ns, String name, String creator) {
    return new FirecloudWorkspace()
        .namespace(ns)
        .name(name)
        .createdBy(creator)
        .bucketName(BUCKET_NAME);
  }

  private void stubGetWorkspace(String ns, String name, String creator) {
    DbWorkspace w = new DbWorkspace();
    w.setWorkspaceNamespace(ns);
    w.setFirecloudName(name);
    w.setCdrVersion(cdrVersion);
    when(workspaceService.getRequired(ns, name)).thenReturn(w);
    stubGetFcWorkspace(createFcWorkspace(ns, name, creator));
  }

  private void stubGetFcWorkspace(FirecloudWorkspace fcWorkspace) {
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    when(fireCloudService.getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName()))
        .thenReturn(fcResponse);
  }

  private JSONObject dataUriToJson(String dataUri) {
    String b64 = dataUri.substring(dataUri.indexOf(',') + 1);
    byte[] raw = Base64.getUrlDecoder().decode(b64);
    return new JSONObject(new String(raw));
  }

  private String getClusterName() {
    return "all-of-us-".concat(Long.toString(user.getUserId()));
  }

  @Test
  public void testListClusters() {
    when(notebookService.getCluster(BILLING_PROJECT_ID, getClusterName()))
        .thenReturn(testFcCluster);

    assertThat(
            clusterController
                .listClusters(BILLING_PROJECT_ID, WORKSPACE_NAME)
                .getBody()
                .getDefaultCluster())
        .isEqualTo(testCluster);
  }

  @Test
  public void testDeleteClustersInProject() {
    List<ListClusterResponse> listClusterResponseList = ImmutableList.of(testFcClusterListResponse);
    when(notebookService.listClustersByProjectAsAdmin(BILLING_PROJECT_ID))
        .thenReturn(listClusterResponseList);

    clusterController.deleteClustersInProject(
        BILLING_PROJECT_ID,
        new ListClusterDeleteRequest()
            .clustersToDelete(ImmutableList.of(testFcCluster.getClusterName())));
    verify(notebookService)
        .deleteClusterAsAdmin(BILLING_PROJECT_ID, testFcCluster.getClusterName());
    verify(clusterAuditor)
        .fireDeleteClustersInProject(
            BILLING_PROJECT_ID,
            listClusterResponseList.stream()
                .map(ListClusterResponse::getClusterName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteClustersInProjectDeleteSome() {
    List<ListClusterResponse> listClusterResponseList =
        ImmutableList.of(testFcClusterListResponse, testFcClusterListResponse2);
    List<String> clustersToDelete = ImmutableList.of(testFcCluster.getClusterName());
    when(notebookService.listClustersByProjectAsAdmin(BILLING_PROJECT_ID))
        .thenReturn(listClusterResponseList);

    clusterController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(clustersToDelete));
    verify(notebookService, times(clustersToDelete.size()))
        .deleteClusterAsAdmin(BILLING_PROJECT_ID, testFcCluster.getClusterName());
    verify(clusterAuditor, times(1))
        .fireDeleteClustersInProject(BILLING_PROJECT_ID, clustersToDelete);
  }

  @Test
  public void testDeleteClustersInProjectDeleteDoesNotAffectOtherProjects() {
    List<ListClusterResponse> listClusterResponseList =
        ImmutableList.of(testFcClusterListResponse, testFcClusterListResponse2);
    List<String> clustersToDelete =
        ImmutableList.of(testFcClusterDifferentProject.getClusterName());
    when(notebookService.listClustersByProjectAsAdmin(BILLING_PROJECT_ID))
        .thenReturn(listClusterResponseList);

    clusterController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(clustersToDelete));
    verify(notebookService, times(0))
        .deleteClusterAsAdmin(BILLING_PROJECT_ID, testFcCluster.getClusterName());
    verify(clusterAuditor, times(0))
        .fireDeleteClustersInProject(BILLING_PROJECT_ID, clustersToDelete);
  }

  @Test
  public void testDeleteClustersInProjectNoClusters() {
    List<ListClusterResponse> listClusterResponseList = ImmutableList.of(testFcClusterListResponse);
    when(notebookService.listClustersByProjectAsAdmin(BILLING_PROJECT_ID))
        .thenReturn(listClusterResponseList);

    clusterController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(ImmutableList.of()));
    verify(notebookService, never())
        .deleteClusterAsAdmin(BILLING_PROJECT_ID, testFcCluster.getClusterName());
    verify(clusterAuditor, never())
        .fireDeleteClustersInProject(
            BILLING_PROJECT_ID,
            listClusterResponseList.stream()
                .map(ListClusterResponse::getClusterName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteClustersInProjectNullClustersList() {
    List<ListClusterResponse> listClusterResponseList = ImmutableList.of(testFcClusterListResponse);
    when(notebookService.listClustersByProjectAsAdmin(BILLING_PROJECT_ID))
        .thenReturn(listClusterResponseList);

    clusterController.deleteClustersInProject(
        BILLING_PROJECT_ID, new ListClusterDeleteRequest().clustersToDelete(null));
    verify(notebookService)
        .deleteClusterAsAdmin(BILLING_PROJECT_ID, testFcCluster.getClusterName());
    verify(clusterAuditor)
        .fireDeleteClustersInProject(
            BILLING_PROJECT_ID,
            listClusterResponseList.stream()
                .map(ListClusterResponse::getClusterName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testListClustersUnknownStatus() {
    when(notebookService.getCluster(BILLING_PROJECT_ID, getClusterName()))
        .thenReturn(testFcCluster.status(null));

    assertThat(
            clusterController
                .listClusters(BILLING_PROJECT_ID, WORKSPACE_NAME)
                .getBody()
                .getDefaultCluster()
                .getStatus())
        .isEqualTo(ClusterStatus.UNKNOWN);
  }

  @Test(expected = BadRequestException.class)
  public void testListClustersNullBillingProject() {
    clusterController.listClusters(null, WORKSPACE_NAME);
  }

  @Test
  public void testListClustersLazyCreate() {
    when(notebookService.getCluster(BILLING_PROJECT_ID, getClusterName()))
        .thenThrow(new NotFoundException());
    when(notebookService.createCluster(
            eq(BILLING_PROJECT_ID), eq(getClusterName()), eq(WORKSPACE_NAME)))
        .thenReturn(testFcCluster);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_NAME, "test");

    assertThat(
            clusterController
                .listClusters(BILLING_PROJECT_ID, WORKSPACE_NAME)
                .getBody()
                .getDefaultCluster())
        .isEqualTo(testCluster);
  }

  @Test
  public void testDeleteCluster() {
    clusterController.deleteCluster(BILLING_PROJECT_ID, "cluster");
    verify(notebookService).deleteCluster(BILLING_PROJECT_ID, "cluster");
  }

  @Test
  public void testSetClusterConfig() {
    ResponseEntity<EmptyResponse> response =
        this.clusterController.updateClusterConfig(
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
        this.clusterController.updateClusterConfig(
            new UpdateClusterConfigRequest()
                .userEmail(OTHER_USER_EMAIL)
                .clusterConfig(
                    new ClusterConfig().machineType("n1-standard-16").masterDiskSize(100)));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    response =
        this.clusterController.updateClusterConfig(
            new UpdateClusterConfigRequest().userEmail(OTHER_USER_EMAIL).clusterConfig(null));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    DbUser updatedUser = userDao.findUserByUsername(OTHER_USER_EMAIL);
    assertThat(updatedUser.getClusterConfigDefault()).isNull();
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateClusterConfigUserNotFound() {
    this.clusterController.updateClusterConfig(
        new UpdateClusterConfigRequest().userEmail("not-found@researchallofus.org"));
  }

  @Test
  public void testLocalize() {
    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest()
            .workspaceNamespace(WORKSPACE_NS)
            .workspaceId(WORKSPACE_ID)
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(BILLING_PROJECT_ID, "cluster", req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid");

    verify(notebookService).localize(eq(BILLING_PROJECT_ID), eq("cluster"), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces/wsid/foo.ipynb",
            "workspaces_playground/wsid/.all_of_us_config.json",
            "workspaces/wsid/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry("workspaces/wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    JSONObject aouJson = dataUriToJson(localizeMap.get("workspaces/wsid/.all_of_us_config.json"));
    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(BILLING_PROJECT_ID);
    assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST);
    verify(userRecentResourceService, times(1))
        .updateNotebookEntry(anyLong(), anyLong(), anyString());
  }

  @Test
  public void testLocalize_playgroundMode() {
    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest()
            .workspaceNamespace(WORKSPACE_NS)
            .workspaceId(WORKSPACE_ID)
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(true);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(BILLING_PROJECT_ID, "cluster", req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces_playground/wsid");
    verify(notebookService).localize(eq(BILLING_PROJECT_ID), eq("cluster"), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces_playground/wsid/foo.ipynb",
            "workspaces_playground/wsid/.all_of_us_config.json",
            "workspaces/wsid/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces_playground/wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
  }

  @Test
  public void testLocalize_differentNamespace() {
    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest()
            .workspaceNamespace(WORKSPACE_NS)
            .workspaceId(WORKSPACE_ID)
            .notebookNames(ImmutableList.of("foo.ipynb"))
            .playgroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize("other-proj", "cluster", req).getBody();
    verify(notebookService).localize(eq("other-proj"), eq("cluster"), mapCaptor.capture());

    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces/proj__wsid/foo.ipynb",
            "workspaces/proj__wsid/.all_of_us_config.json",
            "workspaces_playground/proj__wsid/.all_of_us_config.json");
    assertThat(localizeMap)
        .containsEntry(
            "workspaces/proj__wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/proj__wsid");
    JSONObject aouJson =
        dataUriToJson(localizeMap.get("workspaces/proj__wsid/.all_of_us_config.json"));
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo("other-proj");
  }

  @Test
  public void testLocalize_noNotebooks() {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(BILLING_PROJECT_ID, "cluster", req).getBody();
    verify(notebookService).localize(eq(BILLING_PROJECT_ID), eq("cluster"), mapCaptor.capture());

    // Config files only.
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap.keySet())
        .containsExactly(
            "workspaces_playground/wsid/.all_of_us_config.json",
            "workspaces/wsid/.all_of_us_config.json");
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid");
  }

  @Test
  public void listCluster_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(workspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    assertThrows(
        ForbiddenException.class, () -> clusterController.listClusters(WORKSPACE_NS, WORKSPACE_ID));
  }

  @Test
  public void listCluster_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(workspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    doThrow(ForbiddenException.class)
        .when(workspaceService)
        .enforceWorkspaceAccessLevel(WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.READER);

    assertThrows(
        ForbiddenException.class, () -> clusterController.listClusters(WORKSPACE_NS, WORKSPACE_ID));
    verify(workspaceService, never()).validateActiveBilling(anyString(), anyString());
  }

  @Test
  public void localize_validateActiveBilling() {
    doThrow(ForbiddenException.class)
        .when(workspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest().workspaceNamespace(WORKSPACE_NS).workspaceId(WORKSPACE_ID);

    assertThrows(ForbiddenException.class, () -> clusterController.localize("y", "z", req));
  }

  @Test
  public void localize_validateActiveBilling_checkAccessFirst() {
    doThrow(ForbiddenException.class)
        .when(workspaceService)
        .validateActiveBilling(WORKSPACE_NS, WORKSPACE_ID);

    doThrow(ForbiddenException.class)
        .when(workspaceService)
        .enforceWorkspaceAccessLevel(WORKSPACE_NS, WORKSPACE_ID, WorkspaceAccessLevel.READER);

    ClusterLocalizeRequest req =
        new ClusterLocalizeRequest().workspaceNamespace(WORKSPACE_NS).workspaceId(WORKSPACE_ID);

    assertThrows(ForbiddenException.class, () -> clusterController.localize("y", "z", req));
    verify(workspaceService, never()).validateActiveBilling(anyString(), anyString());
  }

  private void createUser(String email) {
    DbUser user = new DbUser();
    user.setGivenName("first");
    user.setFamilyName("last");
    user.setUsername(email);
    userDao.save(user);
  }
}
