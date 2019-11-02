package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.CohortReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CohortReviewDao : JpaRepository<CohortReview, Long> {

    fun findAllByCohortId(cohortId: Long): Set<CohortReview>

    fun findCohortReviewByCohortIdAndCdrVersionId(
            @Param("cohortId") cohortId: Long, @Param("cdrVersionId") cdrVersionId: Long): CohortReview

    fun findCohortReviewByCohortReviewId(@Param("cohortReviewId") cohortReviewId: Long): CohortReview

    @Query(value = "select * from cohort_review cr "
            + "join cohort c on (cr.cohort_id = c.cohort_id) "
            + "join workspace ws on (c.workspace_id = ws.workspace_id) "
            + "where ws.workspace_namespace = :ns "
            + "and ws.firecloud_name = :fcName "
            + "and ws.active_status = :status", nativeQuery = true)
    fun findByFirecloudNameAndActiveStatus(
            @Param("ns") workspaceNamespace: String,
            @Param("fcName") fcName: String,
            @Param("status") status: Short): List<CohortReview>

    @Query(value = "select * from cohort_review cr "
            + "join cohort c on (cr.cohort_id = c.cohort_id) "
            + "join workspace ws on (c.workspace_id = ws.workspace_id) "
            + "where ws.workspace_namespace = :ns "
            + "and ws.firecloud_name = :fcName "
            + "and cr.cohort_review_id = :cohortReviewId", nativeQuery = true)
    fun findByNamespaceAndFirecloudNameAndCohortReviewId(
            @Param("ns") workspaceNamespace: String,
            @Param("fcName") fcName: String,
            @Param("cohortReviewId") cohortReviewId: Long): CohortReview
}
