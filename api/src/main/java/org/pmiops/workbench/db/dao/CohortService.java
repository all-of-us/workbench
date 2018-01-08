package org.pmiops.workbench.db.dao;

import java.util.logging.Logger;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class CohortService {
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  @Autowired private CohortDao cohortDao;
  @Autowired private CohortReviewDao cohortReviewDao;

  @Transactional
  public Cohort saveAndCloneReviews(Cohort from, Cohort to) {
    Cohort saved = cohortDao.save(to);
    for (CohortReview fromReview : from.getCohortReviews()) {
      CohortReview cr = new CohortReview();
      cr.setCohortId(saved.getCohortId());
      cr.creationTime(saved.getCreationTime());
      cr.setLastModifiedTime(saved.getLastModifiedTime());
      cr.setCdrVersionId(fromReview.getCdrVersionId());
      cr.setMatchedParticipantCount(fromReview.getMatchedParticipantCount());
      cr.setReviewSize(fromReview.getReviewSize());
      cr.setReviewedCount(fromReview.getReviewedCount());
      cr.setReviewStatus(fromReview.getReviewStatus());
      cohortReviewDao.save(cr);
    }
    return saved;
  }
}
