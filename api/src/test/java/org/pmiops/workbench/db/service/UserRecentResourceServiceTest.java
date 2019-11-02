package org.pmiops.workbench.db.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceDao;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserRecentResourceServiceTest {

  UserRecentResourceServiceImpl userRecentResourceService;

  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired CohortDao cohortDao;
  @Autowired UserRecentResourceDao notebookCohortCacheDao;
  @Autowired ConceptSetDao conceptSetDao;

  private DbUser newUser = new DbUser();
  private DbWorkspace newWorkspace = new DbWorkspace();
  private Long cohortId;
  private Long conceptSetId;
  private long workspaceId = 1l;
  private long userId = 1l;
  private FakeClock clock;
  private static final Instant NOW = Instant.now();

  @Before
  public void setUp() {
    newUser.setUserId(userId);
    userDao.save(newUser);
    newWorkspace.setWorkspaceId(workspaceId);
    workspaceDao.save(newWorkspace);
    DbCohort cohort = new DbCohort();
    cohort.setWorkspaceId(workspaceId);
    cohortId = cohortDao.save(cohort).getCohortId();
    DbConceptSet conceptSet = new DbConceptSet();
    conceptSet.setWorkspaceId(workspaceId);
    conceptSetId = conceptSetDao.save(conceptSet).getConceptSetId();
    userRecentResourceService = new UserRecentResourceServiceImpl();
    userRecentResourceService.setDao(notebookCohortCacheDao);
    userRecentResourceService.setCohortDao(cohortDao);
    userRecentResourceService.setConceptSetDao(conceptSetDao);
    clock = new FakeClock(NOW);
  }

  @Test
  public void testInsertCohortEntry() {
    userRecentResourceService.updateCohortEntry(
        workspaceId, userId, cohortId, new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    DbCohort cohort = new DbCohort();
    cohort.setWorkspaceId(workspaceId);
    cohortId = cohortDao.save(cohort).getCohortId();
    userRecentResourceService.updateCohortEntry(
        workspaceId, userId, cohortId, new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 2);
  }

  @Test
  public void testInsertConceptSetEntry() {
    userRecentResourceService.updateConceptSetEntry(
        workspaceId, userId, conceptSetId, new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    DbConceptSet conceptSet = new DbConceptSet();
    conceptSet.setWorkspaceId(workspaceId);
    conceptSetId = conceptSetDao.save(conceptSet).getConceptSetId();
    userRecentResourceService.updateConceptSetEntry(
        workspaceId, userId, conceptSetId, new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 2);
  }

  @Test
  public void testInsertNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory/notebooks/notebook1",
        new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory/notebooks/notebook2",
        new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 2);
  }

  @Test
  public void testUpdateCohortAccessTime() {
    userRecentResourceService.updateCohortEntry(
        workspaceId, userId, cohortId, new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    clock.increment(20000);
    userRecentResourceService.updateCohortEntry(
        workspaceId, userId, cohortId, new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
  }

  @Test
  public void testUpdateConceptSetAccessTime() {
    userRecentResourceService.updateConceptSetEntry(
        workspaceId, userId, conceptSetId, new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    clock.increment(20000);
    userRecentResourceService.updateConceptSetEntry(
        workspaceId, userId, conceptSetId, new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
  }

  @Test
  public void testUpdateNotebookAccessTime() {

    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory/notebooks/notebook1",
        new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    clock.increment(200);
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory/notebooks/notebook1",
        new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
  }

  @Test
  public void testUserLimit() {
    DbWorkspace newWorkspace = new DbWorkspace();
    newWorkspace.setWorkspaceId(2l);
    workspaceDao.save(newWorkspace);
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory1/notebooks/notebook",
        new Timestamp(clock.millis()));
    clock.increment(2000);
    userRecentResourceService.updateNotebookEntry(
        2l, userId, "notebooks", new Timestamp(clock.millis()));
    userRecentResourceService.updateCohortEntry(
        workspaceId, userId, cohortId, new Timestamp(clock.millis()));
    int count = userRecentResourceService.getUserEntryCount() - 3;
    while (count-- >= 0) {
      clock.increment(2000);
      userRecentResourceService.updateNotebookEntry(
          workspaceId,
          userId,
          "gs://someDirectory1/notebooks/notebook" + count,
          new Timestamp(clock.millis()));
    }

    clock.increment(2000);
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, userRecentResourceService.getUserEntryCount());

    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory/notebooks/notebookExtra",
        new Timestamp(clock.millis()));
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, userRecentResourceService.getUserEntryCount());
    DbUserRecentResource cache =
        userRecentResourceService
            .getDao()
            .findByUserIdAndWorkspaceIdAndNotebookName(
                workspaceId, userId, "gs://someDirectory1/notebooks/notebook");
    assertNull(cache);
  }

  //  We do test notebook deletion because it is a path reference
  //  We do not test cohort or concept deletion because these are fk refs with
  //  on delete cascade rule in place (no need to test db functionality)
  @Test
  public void testDeleteNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory1/notebooks/notebook1",
        new Timestamp(clock.millis()));
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 1);
    userRecentResourceService.deleteNotebookEntry(
        workspaceId, userId, "gs://someDirectory1/notebooks/notebook1");
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 0);
  }

  @Test
  public void testDeleteNonExistentNotebookEntry() {
    long rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 0);
    userRecentResourceService.deleteNotebookEntry(
        workspaceId, userId, "gs://someDirectory1/notebooks/notebook");
    rowsCount = userRecentResourceService.getDao().count();
    assertEquals(rowsCount, 0);
  }

  @Test
  public void testFindAllResources() {
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory1/notebooks/notebook1",
        new Timestamp(clock.millis() - 10000));
    userRecentResourceService.updateNotebookEntry(
        workspaceId,
        userId,
        "gs://someDirectory1/notebooks/notebook2",
        new Timestamp(clock.millis() + 10000));
    userRecentResourceService.updateCohortEntry(
        workspaceId, userId, cohortId, new Timestamp(clock.millis()));
    newUser.setUserId(78l);
    userDao.save(newUser);
    userRecentResourceService.updateCohortEntry(
        workspaceId, 78l, cohortId, new Timestamp(clock.millis()));
    List<DbUserRecentResource> resources = userRecentResourceService.findAllResourcesByUser(userId);
    assertEquals(resources.size(), 3);
    assertEquals(resources.get(0).getNotebookName(), "gs://someDirectory1/notebooks/notebook2");
    assertEquals(resources.get(1).getCohort().getCohortId(), cohortId.longValue());
    assertEquals(resources.get(2).getNotebookName(), "gs://someDirectory1/notebooks/notebook1");
  }
}
