package org.pmiops.workbench.db.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.UserRecentResourceDao;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Timestamp;
import java.util.Date;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserRecentResourceServiceTest {

  UserRecentResourceServiceImpl userRecentResourceService;

  @Autowired
  UserDao userDao;
  @Autowired
  WorkspaceDao workspaceDao;
  @Autowired
  CohortDao cohortDao;
  @Autowired
  UserRecentResourceDao notebookCohortCacheDao;
  @Mock
  WorkbenchConfig config;

  private User newUser = new User();
  private Workspace newWorkspace = new Workspace();
  private Long cohortId;
  private long workspaceId = 1l;
  private long userId = 1l;

  @Before
  public void setup() {
    try {
      newUser.setUserId(userId);
      userDao.save(newUser);
      newWorkspace.setWorkspaceId(workspaceId);
      workspaceDao.save(newWorkspace);
      Cohort cohort = new Cohort();
      cohort.setWorkspaceId(workspaceId);
      cohortId = cohortDao.save(cohort).getCohortId();
      config.userRecentResourceConfig = new WorkbenchConfig.UserRecentResourceConfig();
      config.userRecentResourceConfig.userEntrycount = 3;
      userRecentResourceService = new UserRecentResourceServiceImpl(Providers.of(config));
      userRecentResourceService.setDao(notebookCohortCacheDao);
    } catch (Exception ex) {
      System.out.println(ex.getLocalizedMessage());
    }
  }


  @Test
  public void testInsertCohortEntry() {
    userRecentResourceService.updateCohortEntry(workspaceId, userId, cohortId, new Timestamp(new Date().getTime()));
    long noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
    Cohort cohort = new Cohort();
    cohort.setWorkspaceId(1l);
    cohortId = cohortDao.save(cohort).getCohortId();
    userRecentResourceService.updateCohortEntry(workspaceId, userId, cohortId, new Timestamp(new Date().getTime()));
    noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 2);
  }

  @Test
  public void testInsertNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook1", new Timestamp(new Date().getTime()));
    long noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook2", new Timestamp(new Date().getTime()));
    noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 2);

  }

  @Test
  public void testUpdateCohortAccessTime() {
    userRecentResourceService.updateCohortEntry(workspaceId, userId, cohortId, new Timestamp(new Date(2018, 3, 3).getTime()));
    long noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
    userRecentResourceService.updateCohortEntry(workspaceId, userId, cohortId, new Timestamp(new Date().getTime()));
    noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
  }

  @Test
  public void testUpdateNotebookAccessTime() {
    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook1", new Timestamp(new Date(2018, 3, 3).getTime()));
    long noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook1", new Timestamp(new Date().getTime()));
    noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
  }

  @Test
  public void testUserLimit() {
    Workspace newWorkspace = new Workspace();
    newWorkspace.setWorkspaceId(2l);
    workspaceDao.save(newWorkspace);

    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook1", new Timestamp(new Date(2018, 3, 3).getTime()));
    userRecentResourceService.updateNotebookEntry(2l, userId, "notebook2", new Timestamp(new Date(2018, 4, 3).getTime()));
    userRecentResourceService.updateCohortEntry(workspaceId, userId, cohortId, new Timestamp(new Date(2018, 5, 3).getTime()));
    long noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 3);

    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook4", new Timestamp(new Date(2018, 6, 3).getTime()));
    noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 3);
    UserRecentResource cache = userRecentResourceService.getDao().findByUserIdAndWorkspaceIdAndNotebookName(1l, 1l, "notebook1");
    assertNull(cache);
  }

  @Test
  public void testDeleteNotebookEntry() {
    userRecentResourceService.updateNotebookEntry(workspaceId, userId, "notebook1", new Timestamp(new Date(2018, 3, 3).getTime()));
    long noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 1);
    userRecentResourceService.deleteNotebookEntry(workspaceId, userId, "notebook1");
    noOfRows = userRecentResourceService.getDao().count();
    assertEquals(noOfRows, 0);
  }
}

