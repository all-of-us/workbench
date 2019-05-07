package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.Date;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import javax.inject.Provider;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterConfig;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ClusterControllerTest {
  private static final String WORKSPACE_NS = "proj";
  private static final String WORKSPACE_ID = "wsid";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String OTHER_USER_EMAIL = "alice@gmail.com";
  private static final String BUCKET_NAME = "workspace-bucket";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String API_HOST = "api.stable.fake-research-aou.org";
  private static final String API_BASE_URL = "https://" + API_HOST;

  @TestConfiguration
  @Import({
    ClusterController.class
  })
  @MockBean({
    FireCloudService.class,
    LeonardoNotebooksClient.class,
    WorkspaceService.class,
    UserService.class,
    UserRecentResourceService.class
  })
  static class Configuration {
    @Bean
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = new WorkbenchConfig();
      config.server = new WorkbenchConfig.ServerConfig();
      config.server.apiBaseUrl = API_BASE_URL;
      config.access = new WorkbenchConfig.AccessConfig();
      config.access.enableComplianceTraining = true;
      return config;
    }
    @Bean
    Clock clock() {
      return CLOCK;
    }
    @Bean
    User user() {
      // Allows for wiring of the initial Provider<User>; actual mocking of the
      // user is achieved via setUserProvider().
      return null;
    }
  }

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @Autowired
  LeonardoNotebooksClient notebookService;
  @Mock
  private AdminActionHistoryDao adminActionHistoryDao;
  @Autowired
  FireCloudService fireCloudService;
  @Autowired
  UserDao userDao;
  @Autowired
  WorkspaceService workspaceService;
  @Mock
  DirectoryService directoryService;
  @Mock
  Provider<User> userProvider;
  @Mock
  ComplianceService complianceService;
  @Autowired
  ClusterController clusterController;
  @Autowired
  UserRecentResourceService userRecentResourceService;
  @Autowired
  Clock clock;

  private CdrVersion cdrVersion;
  private org.pmiops.workbench.notebooks.model.Cluster testFcCluster;
  private Cluster testCluster;

  @Before
  public void setUp() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new WorkbenchConfig.FireCloudConfig();
    config.firecloud.registeredDomainName = "";
    config.access = new WorkbenchConfig.AccessConfig();
    config.access.enableComplianceTraining = true;
    User user = new User();
    user.setEmail(LOGGED_IN_USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName(WORKSPACE_NS);
    user.setFreeTierBillingProjectStatusEnum(BillingProjectStatus.READY);
    when(userProvider.get()).thenReturn(user);
    clusterController.setUserProvider(userProvider);

    createUser(OTHER_USER_EMAIL);

    UserService userService = new UserService(
        userProvider, userDao, adminActionHistoryDao, CLOCK, new FakeLongRandom(123),
        fireCloudService, Providers.of(config), complianceService, directoryService);
    clusterController.setUserService(userService);

    cdrVersion = new CdrVersion();
    cdrVersion.setName("1");
    //set the db name to be empty since test cases currently
    //run in the workbench schema only.
    cdrVersion.setCdrDbName("");

    String createdDate = Date.fromYearMonthDay(1988, 12, 26).toString();
    testFcCluster = new org.pmiops.workbench.notebooks.model.Cluster()
        .clusterName("all-of-us")
        .googleProject(WORKSPACE_NS)
        .status(org.pmiops.workbench.notebooks.model.ClusterStatus.DELETING)
        .createdDate(createdDate);
    testCluster = new Cluster()
        .clusterName("all-of-us")
        .clusterNamespace(WORKSPACE_NS)
        .status(ClusterStatus.DELETING)
        .createdDate(createdDate);
  }

  private org.pmiops.workbench.firecloud.model.Workspace createFcWorkspace(
      String ns, String name, String creator) {
    return new org.pmiops.workbench.firecloud.model.Workspace()
        .namespace(ns)
        .name(name)
        .createdBy(creator)
        .bucketName(BUCKET_NAME);
  }

  private void stubGetWorkspace(String ns, String name, String creator) throws Exception {
    Workspace w = new Workspace();
    w.setWorkspaceNamespace(ns);
    w.setFirecloudName(name);
    w.setCdrVersion(cdrVersion);
    when(workspaceService.getRequired(ns, name)).thenReturn(w);
    stubGetFcWorkspace(createFcWorkspace(ns, name, creator));
  }

  private void stubGetFcWorkspace(org.pmiops.workbench.firecloud.model.Workspace fcWorkspace)
      throws Exception {
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
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

  @Test
  public void testListClusters() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenReturn(testFcCluster);

    assertThat(clusterController.listClusters(WORKSPACE_NS).getBody().getDefaultCluster())
        .isEqualTo(testCluster);
  }

  @Test
  public void testListClustersUnknownStatus() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenReturn(
        testFcCluster.status(null));

    assertThat(clusterController.listClusters(WORKSPACE_NS).getBody().getDefaultCluster().getStatus())
        .isEqualTo(ClusterStatus.UNKNOWN);
  }

  @Test(expected = FailedPreconditionException.class)
  public void testListClustersWrongBillingProject() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenReturn(
            testFcCluster.status(null));

    clusterController.listClusters("not the right project");
  }

  @Test
  public void testListClustersLazyCreate() throws Exception {
    when(notebookService.getCluster(WORKSPACE_NS, "all-of-us")).thenThrow(new NotFoundException());
    when(notebookService.createCluster(eq(WORKSPACE_NS), eq("all-of-us")))
        .thenReturn(testFcCluster);

    assertThat(clusterController.listClusters(WORKSPACE_NS).getBody().getDefaultCluster())
        .isEqualTo(testCluster);
  }

  @Test
  public void testDeleteCluster() throws Exception {
    clusterController.deleteCluster(WORKSPACE_NS, "cluster");
    verify(notebookService).deleteCluster(WORKSPACE_NS, "cluster");
  }

  @Test
  public void testSetClusterConfig() {
    ResponseEntity<EmptyResponse> response = this.clusterController.updateClusterConfig(
        new UpdateClusterConfigRequest()
            .userEmail(OTHER_USER_EMAIL)
            .clusterConfig(new ClusterConfig()
                .machineType("n1-standard-4")
                .masterDiskSize(100)));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    User updatedUser = userDao.findUserByEmail(OTHER_USER_EMAIL);
    assertThat(updatedUser.getClusterConfigDefault().machineType).isEqualTo("n1-standard-4");
    assertThat(updatedUser.getClusterConfigDefault().masterDiskSize).isEqualTo(100);
  }

  @Test
  public void testUpdateClusterConfigClear() {
    ResponseEntity<EmptyResponse> response = this.clusterController.updateClusterConfig(
        new UpdateClusterConfigRequest()
            .userEmail(OTHER_USER_EMAIL)
            .clusterConfig(new ClusterConfig()
                .machineType("n1-standard-4")
                .masterDiskSize(100)));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    response = this.clusterController.updateClusterConfig(
        new UpdateClusterConfigRequest()
            .userEmail(OTHER_USER_EMAIL)
            .clusterConfig(null));
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    User updatedUser = userDao.findUserByEmail(OTHER_USER_EMAIL);
    assertThat(updatedUser.getClusterConfigDefault()).isNull();
  }

  @Test(expected=NotFoundException.class)
  public void testUpdateClusterConfigUserNotFound() {
    this.clusterController.updateClusterConfig(
        new UpdateClusterConfigRequest().userEmail("not-found@researchallofus.org"));
  }

  @Test
  public void testLocalize() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setNotebookNames(ImmutableList.of("foo.ipynb"));
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(WORKSPACE_NS, "cluster", req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid");

    verify(notebookService).localize(eq(WORKSPACE_NS), eq("cluster"), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap).containsEntry(
        "~/workspaces/wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    JSONObject delocJson = dataUriToJson(localizeMap.get("~/workspaces/wsid/.delocalize.json"));
    assertThat(delocJson.getString("destination")).isEqualTo("gs://workspace-bucket/notebooks");
    JSONObject aouJson = dataUriToJson(localizeMap.get("~/workspaces/wsid/.all_of_us_config.json"));
    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(WORKSPACE_NS);
    assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST);
    verify(userRecentResourceService, times(1)).updateNotebookEntry(anyLong(), anyLong() , anyString(), any(Timestamp.class));
  }

  @Test
  public void testLocalizePlaygroundMode() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setNotebookNames(ImmutableList.of("foo.ipynb"));
    req.setPlaygroundMode(true);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
      clusterController.localize(WORKSPACE_NS, "cluster", req).getBody();
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces_playground/wsid");
    verify(notebookService).localize(eq(WORKSPACE_NS), eq("cluster"), mapCaptor.capture());
    Map<String, String> localizeMap = mapCaptor.getValue();
    JSONObject aouJson = dataUriToJson(localizeMap.get("~/workspaces_playground/wsid/.all_of_us_config.json"));
    assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID);
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(WORKSPACE_NS);
    assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST);
  }

  @Test
  public void testLocalize_differentNamespace() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setNotebookNames(ImmutableList.of("foo.ipynb"));
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize("other-proj", "cluster", req).getBody();
    verify(notebookService).localize(eq("other-proj"), eq("cluster"), mapCaptor.capture());

    Map<String, String> localizeMap = mapCaptor.getValue();
    assertThat(localizeMap).containsEntry(
        "~/workspaces/proj__wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb");
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/proj__wsid");
    JSONObject aouJson = dataUriToJson(localizeMap.get("~/workspaces/proj__wsid/.all_of_us_config.json"));
    assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo("other-proj");
  }

  @Test
  public void testLocalize_noNotebooks() throws Exception {
    ClusterLocalizeRequest req = new ClusterLocalizeRequest();
    req.setWorkspaceNamespace(WORKSPACE_NS);
    req.setWorkspaceId(WORKSPACE_ID);
    req.setPlaygroundMode(false);
    stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL);
    ClusterLocalizeResponse resp =
        clusterController.localize(WORKSPACE_NS, "cluster", req).getBody();
    verify(notebookService).localize(eq(WORKSPACE_NS), eq("cluster"), mapCaptor.capture());

    // Config files only.
    assertThat(mapCaptor.getValue().size()).isEqualTo(2);
    assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid");
  }

    private void createUser(String email) {
      User user = new User();
      user.setGivenName("first");
      user.setFamilyName("last");
      user.setEmail(email);
      userDao.save(user);
    }
}
