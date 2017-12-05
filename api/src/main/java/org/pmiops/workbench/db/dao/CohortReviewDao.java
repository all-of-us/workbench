package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.CohortReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface CohortReviewDao extends JpaRepository<CohortReview, Long> {

    CohortReview findCohortReviewByCohortIdAndCdrVersionId(@Param("cohortId") long cohortId,
                                                               @Param("cdrVersionId") long cdrVersionId);
}
