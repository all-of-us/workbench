package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ParticipantCohortStatusDao extends CrudRepository<ParticipantCohortStatus, Long>, ParticipantCohortStatusDaoCustom {

  // Important: Keep in sync with all DB rows that should be copied.
  static final String ALL_COLUMNS_EXCEPT_REVIEW_ID =
      "participant_id, status, gender_concept_id, birth_date, " +
      "race_concept_id, ethnicity_concept_id";

  // We use native SQL here as there may be a large number of rows within a
  // given cohort review; this avoids loading them into memory.
  @Modifying
  @Query(
      value="INSERT INTO participant_cohort_status" +
      " (cohort_review_id, " + ALL_COLUMNS_EXCEPT_REVIEW_ID + ")" +
      " SELECT (:toCrId), " + ALL_COLUMNS_EXCEPT_REVIEW_ID +
      " FROM participant_cohort_status pcs" +
      " WHERE pcs.cohort_review_id = (:fromCrId)",
      nativeQuery=true)
  void bulkCopyByCohortReview(
      @Param("fromCrId") long fromCohortReviewId, @Param("toCrId") long toCohortReviewId);

    ParticipantCohortStatus findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
            @Param("cohortReviewId") long cohortReviewId,
            @Param("participantId") long participantId);

  List<ParticipantIdAndCohortStatus> findByParticipantKey_CohortReviewIdAndStatusIn(
      Long cohortReviewId, List<CohortStatus> cohortStatuses);
}
