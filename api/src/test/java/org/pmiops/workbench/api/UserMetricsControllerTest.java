package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
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
  UserRecentResourceService userRecentResourceService;
  @Mock
  Provider<User> userProvider;
  @Mock
  CohortService cohortService;
  @Mock
  FireCloudService fireCloudService;
  @Mock
  WorkspaceService workspaceService;

  private UserMetricsController userMetricsController;
  private static final Instant NOW = Instant.now();
  private FakeClock clock = new FakeClock(NOW);

  @Before
  public void setUp() {
    User user = new User();
    user.setUserId(123l);
    List<UserRecentResource> userRecentResources = new ArrayList<>();

    UserRecentResource resource1 = new UserRecentResource();
    resource1.setNotebookName("gs://bucketFile/notebooks/notebook1.ipynb");
    resource1.setCohort(null);
    resource1.setLastAccessDate(new Timestamp(clock.millis()));
    resource1.setUserId(123l);
    resource1.setWorkspaceId(2l);


    userRecentResources.add(resource1);

    Cohort cohort = new Cohort();
    cohort.setName("Cohort Name");
    cohort.setCohortId(1l);
    cohort.setDescription("Cohort description");
    cohort.setLastModifiedTime(new Timestamp(clock.millis()));
    cohort.setCreationTime(new Timestamp(clock.millis()));
    UserRecentResource resource2 = new UserRecentResource();
    resource2.setNotebookName(null);
    resource2.setCohort(cohort);
    resource2.setLastAccessDate(new Timestamp(clock.millis() - 10000));
    resource2.setUserId(123l);
    resource2.setWorkspaceId(2l);

    userRecentResources.add(resource2);

    Workspace workspace  = new Workspace();
    workspace.setWorkspaceId(2l);
    workspace.setWorkspaceNamespace("workspaceNamespace");
    workspace.setFirecloudName("Firecloudname");
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel("READER");

    when(userProvider.get()).thenReturn(user);
    when(userRecentResourceService.findAllResourcesByUser(123l))
        .thenReturn(userRecentResources);
    when(workspaceService.findByWorkspaceId(2l)).thenReturn(workspace);
    when(fireCloudService.getWorkspace("workspaceNamespace", "Firecloudname"))
        .thenReturn(workspaceResponse);

    userMetricsController = new UserMetricsController(
        userProvider,
        userRecentResourceService,
        workspaceService,
        fireCloudService);
  }

  @Test
  public void testGetUserRecentResource() {
    RecentResourceResponse recentResources = userMetricsController
        .getUserRecentResources().getBody();
    assertNotNull(recentResources);
    assertEquals(2, recentResources.size());
    assertNull(recentResources.get(0).getCohort());
    assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/");

    assertEquals(recentResources.get(0).getNotebook().getName(), "notebook1.ipynb");
    assertNotNull(recentResources.get(1).getCohort());
    assertEquals(recentResources.get(1).getCohort().getName(), "Cohort Name");


  }
}

