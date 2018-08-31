package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;

import org.pmiops.workbench.test.FakeClock;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserMetricsControllerTest {

  @Mock
  private UserRecentResourceService userRecentResourceService;
  @Mock
  private Provider<User> userProvider;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private WorkspaceService workspaceService;

  private UserMetricsController userMetricsController;
  private static final Instant NOW = Instant.now();
  private FakeClock clock = new FakeClock(NOW);
  private static final long WORKSPACE_1_ID = 1l;
  private static final long WORKSPACE_2_ID = 2l;
  private static final long COHORT_ID = 1l;
  private static final long USER_ID = 123l;
  private static final String WORKSPACE_NAMESPACE = "workspaceNamespace";
  private static final String FIRECLOUD_WORKSPACE_ID = "Firecloudname";

  @Before
  public void setUp() {
    User user = new User();
    user.setUserId(USER_ID);
    List<UserRecentResource> userRecentResources = new ArrayList<>();

    UserRecentResource resource1 = new UserRecentResource();
    resource1.setNotebookName("gs://bucketFile/notebooks/notebook1.ipynb");
    resource1.setCohort(null);
    resource1.setLastAccessDate(new Timestamp(clock.millis()));
    resource1.setUserId(USER_ID);
    resource1.setWorkspaceId(WORKSPACE_1_ID);


    userRecentResources.add(resource1);

    Cohort cohort = new Cohort();
    cohort.setName("Cohort Name");
    cohort.setCohortId(COHORT_ID);
    cohort.setDescription("Cohort description");
    cohort.setLastModifiedTime(new Timestamp(clock.millis()));
    cohort.setCreationTime(new Timestamp(clock.millis()));

    UserRecentResource resource2 = new UserRecentResource();
    resource2.setNotebookName(null);
    resource2.setCohort(cohort);
    resource2.setLastAccessDate(new Timestamp(clock.millis() - 10000));
    resource2.setUserId(USER_ID);
    resource2.setWorkspaceId(WORKSPACE_2_ID);

    userRecentResources.add(resource2);

    UserRecentResource resource3 = new UserRecentResource();
    resource3.setNotebookName("gs://bucketFile/notebooks/notebook2.ipynb");
    resource3.setCohort(null);
    resource3.setLastAccessDate(new Timestamp(clock.millis() - 10000));
    resource3.setUserId(USER_ID);
    resource3.setWorkspaceId(WORKSPACE_2_ID);

    userRecentResources.add(resource3);

    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(WORKSPACE_1_ID);
    workspace.setWorkspaceNamespace("workspaceNamespace1");
    workspace.setFirecloudName("Firecloudname1");


    Workspace workspace2 = new Workspace();
    workspace2.setWorkspaceId(WORKSPACE_2_ID);
    workspace2.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace2.setFirecloudName(FIRECLOUD_WORKSPACE_ID);

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel("OWNER");

    WorkspaceResponse workspaceResponse2 = new WorkspaceResponse();
    workspaceResponse2.setAccessLevel("READER");

    when(userProvider.get()).thenReturn(user);
    when(userRecentResourceService.findAllResourcesByUser(USER_ID))
        .thenReturn(userRecentResources);
    when(workspaceService.findByWorkspaceId(WORKSPACE_1_ID)).thenReturn(workspace);

    when(workspaceService.findByWorkspaceId(WORKSPACE_2_ID)).thenReturn(workspace2);

    when(workspaceService.getRequired(WORKSPACE_NAMESPACE, FIRECLOUD_WORKSPACE_ID))
        .thenReturn(workspace2);

    when(fireCloudService.getWorkspace("workspaceNamespace1", "Firecloudname1"))
        .thenReturn(workspaceResponse);

    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, FIRECLOUD_WORKSPACE_ID))
        .thenReturn(workspaceResponse2);

    userMetricsController = new UserMetricsController(
        userProvider,
        userRecentResourceService,
        workspaceService,
        fireCloudService,
        clock);
    userMetricsController.setDistinctWorkspaceLimit(5);

  }

  @Test
  public void testGetUserRecentResource() {
    RecentResourceResponse recentResources = userMetricsController
        .getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(3, recentResources.size());
    assertNull(recentResources.get(0).getCohort());
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/");

    assertEquals(recentResources.get(0).getNotebook().getName(), "notebook1.ipynb");
    assertNotNull(recentResources.get(1).getCohort());
    assertEquals(recentResources.get(1).getCohort().getName(), "Cohort Name");
  }

  @Test
  public void testWorkspaceLimit() {
    userMetricsController.setDistinctWorkspaceLimit(1);
    RecentResourceResponse recentResources = userMetricsController
        .getUserRecentResources().getBody();

    assertNotNull(recentResources);
    assertEquals(1, recentResources.size());
    assertNull(recentResources.get(0).getCohort());
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/");
  }

  @Test
  public void testDeleteResource() {
    RecentResourceRequest request = new RecentResourceRequest();
    request.setNotebookName("gs://bucketFile/notebooks/notebook1.ipynb");
    userMetricsController.deleteRecentResource(WORKSPACE_NAMESPACE, FIRECLOUD_WORKSPACE_ID, request);
    verify(userRecentResourceService).deleteNotebookEntry(WORKSPACE_2_ID, USER_ID, "gs://bucketFile/notebooks/notebook1.ipynb");
  }

  @Test
  public void testUpdateRecentResource() {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    UserRecentResource mockUserRecentResource = new UserRecentResource();
    mockUserRecentResource.setCohort(null);
    mockUserRecentResource.setWorkspaceId(WORKSPACE_2_ID);
    mockUserRecentResource.setUserId(USER_ID);
    mockUserRecentResource.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");
    mockUserRecentResource.setLastAccessDate(now);
    when(userRecentResourceService.updateNotebookEntry(WORKSPACE_2_ID, USER_ID, "gs://newBucket/notebooks/notebook.ipynb", now))
        .thenReturn(mockUserRecentResource);

    RecentResourceRequest request = new RecentResourceRequest();
    request.setNotebookName("gs://newBucket/notebooks/notebook.ipynb");

    RecentResource addedEntry = userMetricsController
        .updateRecentResource(WORKSPACE_NAMESPACE, FIRECLOUD_WORKSPACE_ID, request)
        .getBody();

    assertNotNull(addedEntry);
    assertEquals((long) addedEntry.getWorkspaceId(), WORKSPACE_2_ID);
    assertNull(addedEntry.getCohort());
    assertNotNull(addedEntry.getNotebook());
    assertEquals(addedEntry.getNotebook().getName(), "notebook.ipynb");
    assertEquals(addedEntry.getNotebook().getPath(), "gs://newBucket/notebooks/");
  }
}

