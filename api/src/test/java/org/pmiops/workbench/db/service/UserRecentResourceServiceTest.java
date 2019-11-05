package org.pmiops.workbench.db.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserRecentResourceServiceTest {

  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired CohortDao cohortDao;
  @Autowired UserRecentResourceDao userRecentResourceDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired UserRecentResourceService userRecentResourceService;

  private DbUser user;
  private DbWorkspace workspace;
  private DbCohort cohort;
  private DbConceptSet conceptSet;

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @TestConfiguration
  @Import({UserRecentResourceServiceImpl.class})
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    user = userDao.save(new DbUser());

    workspace = workspaceDao.save(new DbWorkspace());

    cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort = cohortDao.save(cohort);

    conceptSet = new DbConceptSet();
    conceptSet.setWorkspaceId(workspace.getWorkspaceId());
    conceptSet = conceptSetDao.save(conceptSet);
  }

  @Test
  public void testInsertCohortEntry() {
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);

    DbCohort newCohort = new DbCohort();
    newCohort.setWorkspaceId(workspace.getWorkspaceId());
    newCohort = cohortDao.save(newCohort);

    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), newCohort.getCohortId());
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 2);
  }

  @Test
  public void testInsertConceptSetEntry() {
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
    DbConceptSet newConceptSet = new DbConceptSet();
    newConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    newConceptSet = conceptSetDao.save(newConceptSet);
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), newConceptSet.getConceptSetId());
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 2);
  }

  @Test
  public void testInsertNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook1");
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook2");
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 2);
  }

  @Test
  public void testUpdateCohortAccessTime() {
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
    CLOCK.increment(20000);
    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
  }

  @Test
  public void testUpdateConceptSetAccessTime() {
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
    CLOCK.increment(20000);
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), user.getUserId(), conceptSet.getConceptSetId());
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
  }

  @Test
  public void testUpdateNotebookAccessTime() {

    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook1");
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
    CLOCK.increment(200);
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory/notebooks/notebook1");
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
  }

  @Test
  public void testUserLimit() {
    DbWorkspace newWorkspace = new DbWorkspace();
    newWorkspace.setWorkspaceId(2l);
    workspaceDao.save(newWorkspace);
    userRecentResourceService.updateNotebookEntry(
        newWorkspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook");
    CLOCK.increment(2000);
    userRecentResourceService.updateNotebookEntry(2l, user.getUserId(), "notebooks");
    userRecentResourceService.updateCohortEntry(
        newWorkspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    int count = UserRecentResourceService.USER_ENTRY_COUNT - 3;
    while (count-- >= 0) {
      CLOCK.increment(2000);
      userRecentResourceService.updateNotebookEntry(
          newWorkspace.getWorkspaceId(),
          user.getUserId(),
          "gs://someDirectory1/notebooks/notebook" + count);
    }

    CLOCK.increment(2000);
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, UserRecentResourceService.USER_ENTRY_COUNT);

    userRecentResourceService.updateNotebookEntry(
        newWorkspace.getWorkspaceId(),
        user.getUserId(),
        "gs://someDirectory/notebooks/notebookExtra");
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, UserRecentResourceService.USER_ENTRY_COUNT);
    DbUserRecentResource cache =
        userRecentResourceDao.findByUserIdAndWorkspaceIdAndNotebookName(
            newWorkspace.getWorkspaceId(),
            user.getUserId(),
            "gs://someDirectory1/notebooks/notebook");

    assertNull(cache);
  }

  //  We do test notebook deletion because it is a path reference
  //  We do not test cohort or concept deletion because these are fk refs with
  //  on delete cascade rule in place (no need to test db functionality)
  @Test
  public void testDeleteNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook1");
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 1);
    userRecentResourceService.deleteNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook1");
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 0);
  }

  @Test
  public void testDeleteNonExistentNotebookEntry() {
    long rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 0);
    userRecentResourceService.deleteNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook");
    rowsCount = userRecentResourceDao.count();
    assertEquals(rowsCount, 0);
  }

  @Test
  public void testFindAllResources() {
    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook1");
    CLOCK.increment(10000);

    userRecentResourceService.updateCohortEntry(
        workspace.getWorkspaceId(), user.getUserId(), cohort.getCohortId());
    CLOCK.increment(10000);

    userRecentResourceService.updateNotebookEntry(
        workspace.getWorkspaceId(), user.getUserId(), "gs://someDirectory1/notebooks/notebook2");

    List<DbUserRecentResource> resources =
        userRecentResourceService.findAllResourcesByUser(user.getUserId());

    assertEquals(resources.size(), 3);
    assertEquals(resources.get(0).getNotebookName(), "gs://someDirectory1/notebooks/notebook2");
    assertEquals(resources.get(1).getCohort().getCohortId(), cohort.getCohortId());
    assertEquals(resources.get(2).getNotebookName(), "gs://someDirectory1/notebooks/notebook1");
  }
}
