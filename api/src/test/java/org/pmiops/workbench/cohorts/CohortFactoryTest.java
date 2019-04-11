package org.pmiops.workbench.cohorts;

import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.ReviewStatus;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class CohortFactoryTest {

  private CohortFactory cohortFactory;

  @Before
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

    User user = mock(User.class);

    long workspaceId = 1l;

    Cohort dbCohort = cohortFactory.createCohort(apiCohort, user, workspaceId);

    assertThat(dbCohort.getDescription()).isEqualTo(apiCohort.getDescription());
    assertThat(dbCohort.getName()).isEqualTo(apiCohort.getName());
    assertThat(dbCohort.getType()).isEqualTo(apiCohort.getType());
    assertThat(dbCohort.getCriteria()).isEqualTo(apiCohort.getCriteria());
    assertThat(dbCohort.getCreator()).isSameAs(user);
    assertThat(dbCohort.getWorkspaceId()).isEqualTo(workspaceId);
  }

  @Test
  public void duplicateCohort() {
    Cohort originalCohort = new Cohort();
    originalCohort.setDescription("desc");
    originalCohort.setName("name");
    originalCohort.setType("type");
    originalCohort.setCriteria("criteria");
    originalCohort.setWorkspaceId(1l);
    originalCohort.setCohortReviews(Collections.singleton(mock(CohortReview.class)));

    User user = mock(User.class);
    Workspace workspace = mock(Workspace.class);
    doReturn(1l).when(workspace).getWorkspaceId();
    Cohort dbCohort = cohortFactory.duplicateCohort("new name", user, workspace, originalCohort);

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

    CohortReview originalCohortReview = new CohortReview();
    originalCohortReview.setCohortId(1l);
    originalCohortReview.setCdrVersionId(2l);
    originalCohortReview.setMatchedParticipantCount(3l);
    originalCohortReview.setReviewSize(4l);
    originalCohortReview.setReviewedCount(5l);
    originalCohortReview.setReviewStatusEnum(ReviewStatus.CREATED);

    Cohort cohort = mock(Cohort.class);
    doReturn(1l).when(cohort).getCohortId();
    doReturn(now).when(cohort).getCreationTime();
    doReturn(now).when(cohort).getLastModifiedTime();
    CohortReview newReview = cohortFactory.duplicateCohortReview(originalCohortReview, cohort);

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
