package org.pmiops.workbench.db.dao;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortReviewDaoTest {

  private static long CDR_VERSION_ID = 1;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  private DbCohortReview cohortReview;
  private long cohortId;

  @Before
  public void setUp() {
    DbCohort cohort = new DbCohort();
    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceNamespace("namespace");
    workspace.setFirecloudName("firecloudName");
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    cohort.setWorkspaceId(workspaceDao.save(workspace).getWorkspaceId());
    cohortId = cohortDao.save(cohort).getCohortId();
    cohortReview = cohortReviewDao.save(createCohortReview());
  }

  @Test
  public void save() {
    assertEquals(cohortReview, cohortReviewDao.findOne(cohortReview.getCohortReviewId()));
  }

  @Test
  public void update() {
    cohortReview = cohortReviewDao.findOne(cohortReview.getCohortReviewId());
    cohortReview.setReviewedCount(3);
    cohortReviewDao.saveAndFlush(cohortReview);
    assertEquals(cohortReview, cohortReviewDao.findOne(cohortReview.getCohortReviewId()));
  }

  @Test
  public void findCohortReviewByCohortIdAndCdrVersionId() {
    assertEquals(
        cohortReview,
        cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
            cohortReview.getCohortId(), cohortReview.getCdrVersionId()));
  }

  @Test
  public void findByFirecloudNameAndActiveStatus() {
    assertEquals(
        cohortReview,
        cohortReviewDao
            .findByFirecloudNameAndActiveStatus(
                "namespace",
                "firecloudName",
                DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE))
            .get(0));
  }

  private DbCohortReview createCohortReview() {
    return new DbCohortReview()
        .cohortId(cohortId)
        .cdrVersionId(CDR_VERSION_ID)
        .creationTime(new Timestamp(Calendar.getInstance().getTimeInMillis()))
        .lastModifiedTime(new Timestamp(Calendar.getInstance().getTimeInMillis()))
        .matchedParticipantCount(100)
        .reviewedCount(10)
        .cohortDefinition("{'name':'test'}")
        .cohortName("test");
  }
}
