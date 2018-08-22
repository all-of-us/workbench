package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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
    resource1.setNotebookName("notebook1");
    resource1.setCohortId(null);
    resource1.setLastAccessDate(new Timestamp(clock.millis()));
    resource1.setUserId(123l);
    resource1.setWorkspaceId(2l);

    UserRecentResource resource2 = new UserRecentResource();
    resource2.setNotebookName(null);
    resource2.setCohortId(1l);
    resource2.setLastAccessDate(new Timestamp(clock.millis() - 10000));
    resource2.setUserId(123l);
    resource2.setWorkspaceId(2l);

    userRecentResources.add(resource1);
    userRecentResources.add(resource2);

    Cohort cohort = new Cohort();
    cohort.setName("Cohort Name");
    cohort.setCohortId(1l);
    cohort.setDescription("Cohort description");

    Workspace workspace  = new Workspace();
    workspace.setWorkspaceId(2l);
    workspace.setWorkspaceNamespace("workspaceNamespace");
    workspace.setFirecloudName("Firecloudname");
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel("READER");

    when(userProvider.get()).thenReturn(user);
    when(cohortService.findCohortByWorkspaceIdAndCohortId(2l, 1l))
        .thenReturn(cohort);
    when(userRecentResourceService.findAllResourcesByUser(123l))
        .thenReturn(userRecentResources);
    when(workspaceService.findByWorkspaceId(2l)).thenReturn(workspace);
    when(fireCloudService.getWorkspace("workspaceNamespace", "Firecloudname"))
        .thenReturn(workspaceResponse);

    userMetricsController = new UserMetricsController(
        userProvider,
        userRecentResourceService,
        cohortService,
        workspaceService,
        fireCloudService);
  }

  @Test
  public void testGetUserMetrics() {
    RecentResourceResponse recentResources = userMetricsController
        .getUserMetrics().getBody();
    assertNotNull(recentResources);
    assertEquals(2, recentResources.size());
    assertEquals(recentResources.get(0).getName(), "notebook1");
    assertEquals(recentResources.get(0).getType(), "notebook");
    assertEquals(recentResources.get(1).getName(), "Cohort Name");
    assertEquals(recentResources.get(1).getDescription(), "Cohort description");
    assertEquals(recentResources.get(1).getType(), "cohort");
  }
}

