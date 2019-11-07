package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CohortCloningService {

  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  @Autowired private CohortDao cohortDao;
  @Autowired private CohortFactory cohortFactory;
  @Autowired private CohortReviewDao cohortReviewDao;
  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

  @Transactional
  public DbCohort cloneCohortAndReviews(DbCohort fromCohort, DbWorkspace targetWorkspace) {
    final DbCohort duplicatedCohort =
        cohortFactory.duplicateCohort(
            fromCohort.getName(), targetWorkspace.getCreator(), targetWorkspace, fromCohort);
    final DbCohort toCohort = cohortDao.save(duplicatedCohort);
    copyCohortAnnotations(fromCohort, toCohort);

    for (DbCohortReview fromReview : fromCohort.getCohortReviews()) {
      final DbCohortReview duplicatedReview =
          cohortFactory.duplicateCohortReview(fromReview, toCohort);
      final DbCohortReview toReview = cohortReviewDao.save(duplicatedReview);
      copyCohortReviewAnnotations(fromCohort, fromReview, toCohort, toReview);
    }

    return toCohort;
  }

  private void copyCohortAnnotations(DbCohort from, DbCohort to) {
    cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationDefinitionByCohort(
        from.getCohortId(), to.getCohortId());
    // Important: this must follow the above method
    // {@link CohortAnnotationDefinitionDao#bulkCopyCohortAnnotationDefinitionByCohort(long, long)}
    cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationEnumsByCohort(
        from.getCohortId(), to.getCohortId());
  }

  private void copyCohortReviewAnnotations(
      DbCohort fromCohort, DbCohortReview fromReview, DbCohort toCohort, DbCohortReview toReview) {
    participantCohortStatusDao.bulkCopyByCohortReview(
        fromReview.getCohortReviewId(), toReview.getCohortReviewId());
    participantCohortAnnotationDao.bulkCopyEnumAnnotationsByCohortReviewAndCohort(
        fromCohort.getCohortId(), toCohort.getCohortId(), toReview.getCohortReviewId());
    participantCohortAnnotationDao.bulkCopyNonEnumAnnotationsByCohortReviewAndCohort(
        fromCohort.getCohortId(), toCohort.getCohortId(), toReview.getCohortReviewId());
  }
}
