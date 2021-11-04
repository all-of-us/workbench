package org.pmiops.workbench.cohorts;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReviewStatus;

public class CohortFactoryTest {

  private CohortFactory cohortFactory;

  @BeforeEach
  public void setUp() {
    cohortFactory = new CohortFactoryImpl(Clock.systemUTC());
  }

  @Test
  public void createCohort() {
    org.pmiops.workbench.model.Cohort apiCohort = new org.pmiops.workbench.model.Cohort();
    apiCohort.setDescription("desc");
    apiCohort.setName("name");
    apiCohort.setType("type");
    apiCohort.setCriteria("criteria");

    DbUser user = mock(DbUser.class);

    long workspaceId = 1l;

    DbCohort dbCohort = cohortFactory.createCohort(apiCohort, user, workspaceId);

    assertThat(dbCohort.getDescription()).isEqualTo(apiCohort.getDescription());
    assertThat(dbCohort.getName()).isEqualTo(apiCohort.getName());
    assertThat(dbCohort.getType()).isEqualTo(apiCohort.getType());
    assertThat(dbCohort.getCriteria()).isEqualTo(apiCohort.getCriteria());
    assertThat(dbCohort.getCreator()).isSameAs(user);
    assertThat(dbCohort.getWorkspaceId()).isEqualTo(workspaceId);
  }

  @Test
  public void duplicateCohort() {
    DbCohort originalCohort = new DbCohort();
    originalCohort.setDescription("desc");
    originalCohort.setName("name");
    originalCohort.setType("type");
    originalCohort.setCriteria("criteria");
    originalCohort.setWorkspaceId(1l);
    originalCohort.setCohortReviews(Collections.singleton(mock(DbCohortReview.class)));

    DbUser user = mock(DbUser.class);
    DbWorkspace workspace = mock(DbWorkspace.class);
    doReturn(1l).when(workspace).getWorkspaceId();
    DbCohort dbCohort = cohortFactory.duplicateCohort("new name", user, workspace, originalCohort);

    assertThat(dbCohort.getDescription()).isEqualTo(originalCohort.getDescription());
    assertThat(dbCohort.getName()).isEqualTo("new name");
    assertThat(dbCohort.getType()).isEqualTo(originalCohort.getType());
    assertThat(dbCohort.getCriteria()).isEqualTo(originalCohort.getCriteria());
    assertThat(dbCohort.getCreator()).isSameAs(user);
    assertThat(dbCohort.getWorkspaceId()).isEqualTo(originalCohort.getWorkspaceId());
    assertThat(dbCohort.getCohortReviews()).isNull();
  }

  @Test
  public void duplicateCohortReview() {
    Timestamp now = new Timestamp(Clock.systemUTC().millis());

    DbCohortReview originalCohortReview = new DbCohortReview();
    originalCohortReview.setCohortId(1l);
    originalCohortReview.setCdrVersionId(2l);
    originalCohortReview.setMatchedParticipantCount(3l);
    originalCohortReview.setReviewSize(4l);
    originalCohortReview.setReviewedCount(5l);
    originalCohortReview.setReviewStatusEnum(ReviewStatus.CREATED);

    DbCohort cohort = mock(DbCohort.class);
    doReturn(1l).when(cohort).getCohortId();
    doReturn(now).when(cohort).getCreationTime();
    doReturn(now).when(cohort).getLastModifiedTime();
    DbCohortReview newReview = cohortFactory.duplicateCohortReview(originalCohortReview, cohort);

    assertThat(newReview.getCohortId()).isEqualTo(originalCohortReview.getCohortId());
    assertThat(newReview.getCreationTime()).isEqualTo(now);
    assertThat(newReview.getLastModifiedTime()).isEqualTo(now);
    assertThat(newReview.getCdrVersionId()).isEqualTo(originalCohortReview.getCdrVersionId());
    assertThat(newReview.getMatchedParticipantCount())
        .isEqualTo(originalCohortReview.getMatchedParticipantCount());
    assertThat(newReview.getReviewSize()).isEqualTo(originalCohortReview.getReviewSize());
    assertThat(newReview.getReviewedCount()).isEqualTo(originalCohortReview.getReviewedCount());
    assertThat(newReview.getReviewStatusEnum())
        .isEqualTo(originalCohortReview.getReviewStatusEnum());
  }
}
