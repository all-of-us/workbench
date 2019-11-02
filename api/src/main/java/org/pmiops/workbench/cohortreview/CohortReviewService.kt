package org.pmiops.workbench.cohortreview

import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest
import org.pmiops.workbench.model.WorkspaceAccessLevel

interface CohortReviewService {

    /**
     * Find the [Cohort] for the specified cohortId.
     *
     * @param cohortId
     * @return
     */
    fun findCohort(cohortId: Long): Cohort

    /**
     * Validate that a workspace exists for the specified workspaceId, and set the workspace's. CDR
     * version in [org.pmiops.workbench.cdr.CdrVersionContext].
     *
     * @param workspaceNamespace
     * @param workspaceName
     * @param workspaceId
     */
    fun validateMatchingWorkspaceAndSetCdrVersion(
            workspaceNamespace: String,
            workspaceName: String,
            workspaceId: Long,
            requiredAccess: WorkspaceAccessLevel): Workspace

    fun enforceWorkspaceAccessLevel(
            workspaceNamespace: String, workspaceId: String, requiredAccess: WorkspaceAccessLevel): WorkspaceAccessLevel

    /**
     * Find the [CohortReview] for the specified cohortId and cdrVersionId.
     *
     * @param cohortId
     * @param cdrVersionId
     * @return
     */
    fun findCohortReview(cohortId: Long?, cdrVersionId: Long?): CohortReview

    /**
     * Find the [CohortReview] for the specified cohortReviewId.
     *
     * @param cohortReviewId
     * @return
     */
    fun findCohortReview(cohortReviewId: Long?): CohortReview

    /**
     * Find the [CohortReview] for the specified ns, firecloudName and cohortReviewId
     *
     * @param ns
     * @param firecloudName
     * @param cohortReviewId
     * @return
     */
    fun findCohortReview(ns: String, firecloudName: String, cohortReviewId: Long?): CohortReview

    /**
     * Delete the specified cohort review.
     *
     * @param cohortReview
     */
    fun deleteCohortReview(cohortReview: CohortReview)

    /**
     * Find the [CohortReview] for the specified ns and firecloudName.
     *
     * @param ns
     * @param firecloudName
     * @return
     */
    fun getRequiredWithCohortReviews(ns: String, firecloudName: String): List<CohortReview>

    /**
     * Save the specified [CohortReview].
     *
     * @param cohortReview
     * @return
     */
    fun saveCohortReview(cohortReview: CohortReview): CohortReview

    /**
     * Save the [CohortReview] as well as the collection of [ParticipantCohortStatus].
     *
     * @param cohortReview
     * @param participantCohortStatuses
     */
    fun saveFullCohortReview(
            cohortReview: CohortReview, participantCohortStatuses: List<ParticipantCohortStatus>)

    /**
     * Save the specified [ParticipantCohortStatus].
     *
     * @param participantCohortStatus
     * @return
     */
    fun saveParticipantCohortStatus(
            participantCohortStatus: ParticipantCohortStatus): ParticipantCohortStatus

    /**
     * Find the [ParticipantCohortStatus] for the specified cohortReviewId and participantId.
     *
     * @param cohortReviewId
     * @param participantId
     * @return
     */
    fun findParticipantCohortStatus(cohortReviewId: Long?, participantId: Long?): ParticipantCohortStatus

    /**
     * Find a list of [ParticipantCohortStatus] for the specified cohortReviewId, filtering and
     * paging.
     *
     * @param cohortReviewId
     * @param pageRequest
     * @return
     */
    fun findAll(cohortReviewId: Long?, pageRequest: PageRequest): List<ParticipantCohortStatus>

    /**
     * Find count for the specified cohortReviewId, filtering and paging.
     *
     * @param cohortReviewId
     * @param pageRequest
     * @return
     */
    fun findCount(cohortReviewId: Long?, pageRequest: PageRequest): Long?

    /**
     * Save the specified [ParticipantCohortAnnotation].
     *
     * @param cohortReviewId
     * @param participantCohortAnnotation
     * @return
     */
    fun saveParticipantCohortAnnotation(
            cohortReviewId: Long?, participantCohortAnnotation: ParticipantCohortAnnotation): ParticipantCohortAnnotation

    /**
     * Save the [ParticipantCohortAnnotation] for the specified participantId, annotationId and
     * [ModifyParticipantCohortAnnotationRequest].
     *
     * @param annotationId
     * @param participantId
     * @param modifyParticipantCohortAnnotationRequest
     * @return
     */
    fun updateParticipantCohortAnnotation(
            annotationId: Long?,
            cohortReviewId: Long?,
            participantId: Long?,
            modifyParticipantCohortAnnotationRequest: ModifyParticipantCohortAnnotationRequest): ParticipantCohortAnnotation

    /**
     * Find the [CohortAnnotationDefinition] for the specified cohortAnnotationDefinitionId.
     *
     * @param cohortAnnotationDefinitionId
     * @return
     */
    fun findCohortAnnotationDefinition(cohortAnnotationDefinitionId: Long?): CohortAnnotationDefinition

    /**
     * Delete the [ParticipantCohortAnnotation] for the specified annotationId, cohortReviewId
     * and participantId.
     *
     * @param annotationId
     * @param cohortReviewId
     * @param participantId
     */
    fun deleteParticipantCohortAnnotation(
            annotationId: Long?, cohortReviewId: Long?, participantId: Long?)

    /**
     * Find the [ParticipantCohortAnnotation] for the specified cohortReviewId,
     * cohortAnnotationDefinitonId and participantId.
     *
     * @param cohortReviewId
     * @param cohortAnnotationDefinitionId
     * @param participantId
     * @return
     */
    fun findParticipantCohortAnnotation(
            cohortReviewId: Long?, cohortAnnotationDefinitionId: Long?, participantId: Long?): ParticipantCohortAnnotation

    /**
     * Find a list of [ParticipantCohortAnnotation] for the specified cohortReviewId and
     * participantId.
     *
     * @param cohortReviewId
     * @param participantId
     * @return
     */
    fun findParticipantCohortAnnotations(
            cohortReviewId: Long?, participantId: Long?): List<ParticipantCohortAnnotation>
}
