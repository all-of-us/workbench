package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantIdAndCohortStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ParticipantCohortStatusDao
    extends CrudRepository<DbParticipantCohortStatus, Long>, ParticipantCohortStatusDaoCustom {

  // Important: Keep in sync with all DB rows that should be copied.
  static final String ALL_COLUMNS_EXCEPT_REVIEW_ID =
      "participant_id, status, gender_concept_id, birth_date, "
          + "race_concept_id, ethnicity_concept_id, sex_at_birth_concept_id, deceased";

  // We use native SQL here as there may be a large number of rows within a
  // given cohort review; this avoids loading them into memory.
  @Modifying
  @Query(
      value =
          "INSERT INTO participant_cohort_status"
              + " (cohort_review_id, "
              + ALL_COLUMNS_EXCEPT_REVIEW_ID
              + ")"
              + " SELECT (:toCrId), "
              + ALL_COLUMNS_EXCEPT_REVIEW_ID
              + " FROM participant_cohort_status pcs"
              + " WHERE pcs.cohort_review_id = (:fromCrId)",
      nativeQuery = true)
  void bulkCopyByCohortReview(
      @Param("fromCrId") long fromCohortReviewId, @Param("toCrId") long toCohortReviewId);

  @Modifying
  @Query(
      value =
          "UPDATE participant_cohort_status"
              + " SET sex_at_birth_concept_id = :conceptId"
              + " WHERE participant_id in (:personIds)"
              + " AND cohort_review_id = :cohortReviewId",
      nativeQuery = true)
  @Transactional
  int bulkUpdateSexAtBirthByParticipantAndCohortReviewId(
      @Param("conceptId") long conceptId,
      @Param("personIds") List<Long> personIds,
      @Param("cohortReviewId") Long cohortReviewId);

  @Query(
      value =
          "SELECT participant_id"
              + " FROM participant_cohort_status pcs"
              + " WHERE cohort_review_id = :cohortReviewId",
      nativeQuery = true)
  Set<Long> findParticipantIdsByCohortReviewId(@Param("cohortReviewId") Long cohortReviewId);

  DbParticipantCohortStatus findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
      @Param("cohortReviewId") long cohortReviewId, @Param("participantId") long participantId);

  List<DbParticipantIdAndCohortStatus> findByParticipantKey_CohortReviewIdAndStatusIn(
      Long cohortReviewId, List<Short> cohortStatuses);

  List<DbParticipantCohortStatus> findByParticipantKey_CohortReviewId(Long cohortReviewId);
}
