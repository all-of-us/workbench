package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ParticipantCohortStatusDao extends CrudRepository<ParticipantCohortStatus, Long> {

    Slice<ParticipantCohortStatus> findByParticipantKey_CohortReviewId(@Param("cohortReviewId") long cohortReviewId,
                                                                               Pageable pageRequest);

    Long countByParticipantKey_CohortReviewId(@Param("cohortReviewId") long cohortReviewId);
}
