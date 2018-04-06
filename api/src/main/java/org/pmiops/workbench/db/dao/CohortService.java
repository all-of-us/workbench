package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CohortService {
  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  @Autowired private CohortDao cohortDao;
  @Autowired private CohortReviewDao cohortReviewDao;
  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

  @Transactional
  public Cohort saveAndCloneReviews(Cohort from, Cohort to) {
    Cohort saved = cohortDao.save(to);
    cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationDefinitionByCohort(
            from.getCohortId(), to.getCohortId());
    //Important: this must follow the above method
    //{@link CohortAnnotationDefinitionDao#bulkCopyCohortAnnotationDefinitionByCohort(long, long)}
    cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationEnumsByCohort(
            from.getCohortId(), to.getCohortId());
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
      cr = cohortReviewDao.save(cr);
      participantCohortStatusDao.bulkCopyByCohortReview(
        fromReview.getCohortReviewId(), cr.getCohortReviewId());
      participantCohortAnnotationDao.bulkCopyEnumAnnotationsByCohortReviewAndCohort(
              from.getCohortId(),
              to.getCohortId(),
              cr.getCohortReviewId());
      participantCohortAnnotationDao.bulkCopyNonEnumAnnotationsByCohortReviewAndCohort(
              from.getCohortId(),
              to.getCohortId(),
              cr.getCohortReviewId());

    }
    return saved;
  }
}
