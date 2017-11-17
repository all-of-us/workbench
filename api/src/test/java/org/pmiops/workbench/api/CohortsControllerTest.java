package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
public class CohortsControllerTest {
  @TestConfiguration
  @Import(WorkspaceServiceImpl.class)
  @MockBean(FireCloudService.class)
  static class Configuration {}

  private static final Instant NOW = Instant.now();
  private static final Clock CLOCK = FakeClock.fixed(NOW, ZoneId.systemDefault());

  Workspace workspace;
  @Autowired
  WorkspaceService workspaceService;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Autowired
  CohortDao cohortDao;
  @Autowired
  UserDao userDao;
  @Mock
  Provider<User> userProvider;
  @Autowired
  FireCloudService fireCloudService;

  private CohortsController cohortsController;

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

    workspace = new Workspace();
    workspace.setName("test");
    workspace.setNamespace("ns");
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());

    WorkspacesController workspacesController = new WorkspacesController(workspaceService,
        cdrVersionDao, userDao, userProvider, fireCloudService, CLOCK);
    workspace = workspacesController.createWorkspace(workspace).getBody();

    this.cohortsController = new CohortsController(
        workspaceService, cohortDao, userProvider, CLOCK);
  }

  public Cohort createDefaultCohort() {
    Cohort cohort = new Cohort();
    cohort.setName("name");
    return cohort;
  }

  @Test
  public void testUpdateCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    cohort.setName("updated-name");
    Cohort updated = cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort).getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    cohort.setName("updated-name2");
    updated = cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort).getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    // Verify that we can update without an etag.
    cohort.setEtag(null);
    cohort.setName("updated-name3");
    updated = cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort).getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    Cohort got = cohortsController.getCohort(workspace.getNamespace(), workspace.getId(), cohort.getId()).getBody();
    assertThat(got).isEqualTo(cohort);
  }

  @Test(expected = ConflictException.class)
  public void testUpdateCohortStaleThrows() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(),
        new Cohort().name("updated-name").etag(cohort.getEtag())).getBody();

    // Still using the initial etag.
    cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(),
        new Cohort().name("updated-name2").etag(cohort.getEtag())).getBody();
  }

  @Test
  public void testUpdateCohortInvalidEtagsThrow() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    // TODO: Refactor to be a @Parameterized test case.
    List<String> cases = ImmutableList.of("hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"");
    for (String etag : cases) {
      try {
        cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(),
            new Cohort().name("updated-name").etag(etag));
        fail(String.format("expected BadRequestException for etag: %s", etag));
      } catch(BadRequestException e) {
        // expected
      }
    }
  }
}
