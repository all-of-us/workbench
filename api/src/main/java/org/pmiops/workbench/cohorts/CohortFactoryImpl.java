package org.pmiops.workbench.cohorts;

import java.sql.Timestamp;
import java.time.Clock;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortFactoryImpl implements CohortFactory {

  private final Clock clock;

  @Autowired
  public CohortFactoryImpl(Clock clock) {
    this.clock = clock;
  }

  @Override
  public DbCohort createCohort(
      org.pmiops.workbench.model.Cohort apiCohort, DbUser creator, long workspaceId) {
    return createCohort(
        apiCohort.getDescription(),
        apiCohort.getName(),
        apiCohort.getType(),
        apiCohort.getCriteria(),
        creator,
        workspaceId);
  }

  @Override
  public DbCohort duplicateCohort(
      String newName, DbUser creator, DbWorkspace workspace, DbCohort original) {
    return createCohort(
        original.getDescription(),
        newName,
        original.getType(),
        original.getCriteria(),
        creator,
        workspace.getWorkspaceId());
  }

  @Override
  public DbCohortReview duplicateCohortReview(DbCohortReview original, DbCohort targetCohort) {
    DbCohortReview newCohortReview = new DbCohortReview();

    newCohortReview.setCohortId(targetCohort.getCohortId());
    newCohortReview.creationTime(targetCohort.getCreationTime());
    newCohortReview.setLastModifiedTime(targetCohort.getLastModifiedTime());
    newCohortReview.setCdrVersionId(original.getCdrVersionId());
    newCohortReview.setMatchedParticipantCount(original.getMatchedParticipantCount());
    newCohortReview.setReviewSize(original.getReviewSize());
    newCohortReview.setReviewedCount(original.getReviewedCount());
    newCohortReview.setReviewStatusEnum(original.getReviewStatusEnum());
    newCohortReview.setCohortName(original.getCohortName());
    newCohortReview.setCohortDefinition(original.getCohortDefinition());
    newCohortReview.setDescription(original.getDescription());
    newCohortReview.setCreator(original.getCreator());

    return newCohortReview;
  }

  private DbCohort createCohort(
      String desc, String name, String type, String criteria, DbUser creator, long workspaceId) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    DbCohort cohort = new DbCohort();

    cohort.setDescription(desc);
    cohort.setName(name);
    cohort.setType(type);
    cohort.setCriteria(criteria);
    cohort.setCreationTime(now);
    cohort.setLastModifiedTime(now);
    cohort.setLastModifiedBy(creator.getUsername());
    cohort.setVersion(1);
    cohort.setCreator(creator);
    cohort.setWorkspaceId(workspaceId);

    return cohort;
  }
}
