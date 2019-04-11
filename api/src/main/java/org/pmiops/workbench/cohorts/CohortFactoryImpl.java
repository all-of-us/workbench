package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;

@Service
public class CohortFactoryImpl implements CohortFactory {

  private final Clock clock;

  @Autowired
  public CohortFactoryImpl(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator,
      long workspaceId) {
    return createCohort(
        apiCohort.getDescription(),
        apiCohort.getName(),
        apiCohort.getType(),
        apiCohort.getCriteria(),
        creator,
        workspaceId
    );
  }

  @Override
  public Cohort duplicateCohort(String newName, User creator, Workspace workspace,
      Cohort original) {
    return createCohort(
        original.getDescription(),
        newName,
        original.getType(),
        original.getCriteria(),
        creator,
        workspace.getWorkspaceId()
    );
  }

  @Override
  public CohortReview duplicateCohortReview(CohortReview original, Cohort targetCohort) {
    CohortReview newCohortReview = new CohortReview();

    newCohortReview.setCohortId(targetCohort.getCohortId());
    newCohortReview.creationTime(targetCohort.getCreationTime());
    newCohortReview.setLastModifiedTime(targetCohort.getLastModifiedTime());
    newCohortReview.setCdrVersionId(original.getCdrVersionId());
    newCohortReview.setMatchedParticipantCount(original.getMatchedParticipantCount());
    newCohortReview.setReviewSize(original.getReviewSize());
    newCohortReview.setReviewedCount(original.getReviewedCount());
    newCohortReview.setReviewStatusEnum(original.getReviewStatusEnum());

    return newCohortReview;
  }

  private Cohort createCohort(String desc, String name, String type, String criteria, User creator,
      long workspaceId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    Cohort cohort = new Cohort();

    cohort.setDescription(desc);
    cohort.setName(name);
    cohort.setType(type);
    cohort.setCriteria(criteria);
    cohort.setCreationTime(now);
    cohort.setLastModifiedTime(now);
    cohort.setVersion(1);
    cohort.setCreator(creator);
    cohort.setWorkspaceId(workspaceId);

    return cohort;
  }

}
