package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
public class CohortReviewDaoTest {

  private static long CDR_VERSION_ID = 1;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  private DbCohortReview cohortReview;
  private long cohortId;

  @BeforeEach
  public void setUp() {
    DbCohort cohort = new DbCohort();
    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceNamespace("namespace");
    workspace.setFirecloudName("firecloudName");
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    cohort.setWorkspaceId(workspaceDao.save(workspace).getWorkspaceId());
    cohortId = cohortDao.save(cohort).getCohortId();
    cohortReview = cohortReviewDao.save(createCohortReview());
    cohortReviewDao.save(createCohortReview());
  }

  @Test
  public void save() {
    assertThat(cohortReviewDao.findById(cohortReview.getCohortReviewId()).get())
        .isEqualTo(cohortReview);
  }

  @Test
  public void update() {
    cohortReview = cohortReviewDao.findById(cohortReview.getCohortReviewId()).get();
    cohortReview.setReviewedCount(3);
    cohortReviewDao.saveAndFlush(cohortReview);
    assertThat(cohortReviewDao.findById(cohortReview.getCohortReviewId()).get())
        .isEqualTo(cohortReview);
  }

  @Test
  public void findCohortReviewByCohortIdAndCdrVersionIdOrderByCohortReviewId() {
    assertThat(
            cohortReviewDao
                .findCohortReviewByCohortIdAndCdrVersionIdOrderByCohortReviewId(
                    cohortReview.getCohortId(), cohortReview.getCdrVersionId())
                .stream()
                .findFirst()
                .orElse(null))
        .isEqualTo(cohortReview);
  }

  @Test
  public void findByFirecloudNameAndActiveStatus() {
    assertThat(
            cohortReviewDao
                .findByFirecloudNameAndActiveStatus(
                    "namespace",
                    "firecloudName",
                    DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE))
                .get(0))
        .isEqualTo(cohortReview);
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
