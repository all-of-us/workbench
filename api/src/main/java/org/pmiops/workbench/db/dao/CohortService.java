package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.model.AnnotationType;
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
      cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationDefinitionByCohort(
              from.getCohortId(), to.getCohortId());
      cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationEnumsByCohort(
              from.getCohortId(), to.getCohortId());
//      cohortAnnotationDefinitionDao.findByCohortId(from.getCohortId())
//        .stream()
//        .map(definition -> new CohortAnnotationDefinition(definition).cohortId(saved.getCohortId()))
//        .forEach(cohortAnnotationDefinitionDao::save);
      participantCohortAnnotationDao.bulkCopyEnumAnnotationsByCohortReviewAndCohort(
        fromReview.getCohortReviewId(),
        cr.getCohortReviewId(),
        from.getCohortId(),
        to.getCohortId());
      participantCohortAnnotationDao.bulkCopyNonEnumAnnotationsByCohortReviewAndCohort(
        fromReview.getCohortReviewId(),
        cr.getCohortReviewId(),
        from.getCohortId(),
        to.getCohortId());

    }
    return saved;
  }
}
