package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ParticipantCohortStatusDao extends CrudRepository<ParticipantCohortStatus, Long>, ParticipantCohortStatusDaoCustom {

  // We use native SQL here as there may be a large number of rows within a
  // given cohort review; this avoids loading them into memory.
  @Modifying
  @Query(
      value="INSERT INTO participant_cohort_status" +
      " (cohort_review_id, participant_id, status)" +
      " SELECT :toCrId, participant_id, pcs.status" +
      " FROM participant_cohort_status pcs" +
      " WHERE pcs.cohort_review_id = (:fromCrId)",
      nativeQuery=true)
  void bulkCopyByCohortReview(
      @Param("fromCrId") long fromCohortReviewId, @Param("toCrId") long toCohortReviewId);

    Slice<ParticipantCohortStatus> findByParticipantKey_CohortReviewId(@Param("cohortReviewId") long cohortReviewId,
                                                                               Pageable pageRequest);

    ParticipantCohortStatus findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
            @Param("cohortReviewId") long cohortReviewId,
            @Param("participantId") long participantId);

}
