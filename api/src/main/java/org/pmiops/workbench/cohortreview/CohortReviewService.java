package org.pmiops.workbench.cohortreview;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.Vocabulary;

public interface CohortReviewService {

  /** Find the {@link DbCohort} for the specified cohortId. */
  DbCohort findCohort(long workspaceId, long cohortId);

  /** Find the {@link DbCohortReview} for the specified cohortId and cdrVersionId. */
  CohortReview findCohortReview(Long cohortId, Long cdrVersionId);

  /** Find the {@link DbCohortReview} for the specified cohortReviewId. */
  CohortReview findCohortReview(Long cohortReviewId);

  /** Find the {@link CohortReview} for the specified workspaceId and cohortReviewId. */
  CohortReview findCohortReviewForWorkspace(Long workspaceId, Long cohortReviewId);

  /** Delete the specified cohort review. */
  void deleteCohortReview(Long cohortReviewId);

  /** Find the {@link DbCohortReview} for the specified ns and firecloudName. */
  List<CohortReview> getRequiredWithCohortReviews(String ns, String firecloudName);

  /** Save the specified {@link CohortReview}. */
  CohortReview saveCohortReview(CohortReview cohortReview, DbUser creator);

  /**
   * Save the {@link DbCohortReview} as well as the collection of {@link DbParticipantCohortStatus}.
   */
  void saveFullCohortReview(
      CohortReview cohortReview, List<DbParticipantCohortStatus> participantCohortStatuses);

  CohortReview updateCohortReview(
      CohortReview cohortReview, Long cohortReviewId, Timestamp lastModified);

  /** Update the specified {@link ParticipantCohortStatus}. */
  ParticipantCohortStatus updateParticipantCohortStatus(
      Long cohortReviewId, Long participantId, CohortStatus status, Timestamp lastModified);

  /**
   * Find the {@link ParticipantCohortStatus} for the specified cohortReviewId and participantId.
   */
  ParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId);

  /**
   * Find a list of {@link ParticipantCohortStatus} for the specified cohortReviewId, filtering and
   * paging.
   */
  List<ParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest);

  /** Find count for the specified cohortReviewId, filtering and paging. */
  Long findCount(Long cohortReviewId, PageRequest pageRequest);

  /** Save the specified {@link ParticipantCohortAnnotation}. */
  ParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, ParticipantCohortAnnotation participantCohortAnnotation);

  /**
   * Save the {@link ParticipantCohortAnnotation} for the specified participantId, annotationId and
   * {@link ModifyParticipantCohortAnnotationRequest}.
   */
  ParticipantCohortAnnotation updateParticipantCohortAnnotation(
      Long annotationId,
      Long cohortReviewId,
      Long participantId,
      ModifyParticipantCohortAnnotationRequest modifyParticipantCohortAnnotationRequest);

  /**
   * Delete the {@link DbParticipantCohortAnnotation} for the specified annotationId, cohortReviewId
   * and participantId.
   */
  void deleteParticipantCohortAnnotation(
      Long annotationId, Long cohortReviewId, Long participantId);

  /**
   * Find a list of {@link ParticipantCohortAnnotation} for the specified cohortReviewId and
   * participantId.
   */
  List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(
      Long cohortReviewId, Long participantId);

  /** Initialize a {@link CohortReview} */
  CohortReview initializeCohortReview(Long cdrVersionId, DbCohort dbCohort);

  /**
   * Create a list of {@link DbParticipantCohortStatus} for the specified cohort, requestSize and
   * review id.
   */
  List<DbParticipantCohortStatus> createDbParticipantCohortStatusesList(
      DbCohort dbCohort, Integer requestSize, Long cohortReviewId);

  /** Find a list of {@link CohortChartData} for the specified cohort and domain. */
  List<CohortChartData> findCohortChartData(DbCohort dbCohort, Domain domain, int limit);

  /** Find a list of {@link ParticipantChartData} for the specified participant id and domain. */
  List<ParticipantChartData> findParticipantChartData(Long participantId, Domain domain, int limit);

  /** Find participant count. */
  Long findParticipantCount(Long participantId, Domain domain, PageRequest pageRequest);

  /** Find list of {@link ParticipantData} for specifice participant id and domain. */
  List<ParticipantData> findParticipantData(
      Long participantId, Domain domain, PageRequest pageRequest);

  /** Find list of {@link Vocabulary}.* */
  List<Vocabulary> findVocabularies();

  Long participationCount(DbCohort dbCohort);
}
