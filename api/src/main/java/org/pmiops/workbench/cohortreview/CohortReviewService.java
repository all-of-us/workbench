package org.pmiops.workbench.cohortreview;

import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

import java.util.List;

public interface CohortReviewService {

    /**
     * Find the {@link Cohort} for the specified cohortId.
     *
     * @param cohortId
     * @return
     */
    Cohort findCohort(long cohortId);

    /**
     * Validate that a workspace exists for the specified workspaceId.
     *
     * @param workspaceNamespace
     * @param workspaceName
     * @param workspaceId
     */
    Workspace validateMatchingWorkspace(String workspaceNamespace, String workspaceName,
        long workspaceId, WorkspaceAccessLevel requiredAccess);

    /**
     * Find the {@link CohortReview} for the specified cohortId and cdrVersionId.
     *
     * @param cohortId
     * @param cdrVersionId
     * @return
     */
    CohortReview findCohortReview(Long cohortId, Long cdrVersionId);

    /**
     * Save the specified {@link CohortReview}.
     *
     * @param cohortReview
     * @return
     */
    CohortReview saveCohortReview(CohortReview cohortReview);

    /**
     * Save the {@link CohortReview} as well as the collection of {@link ParticipantCohortStatus}.
     *
     * @param cohortReview
     * @param participantCohortStatuses
     */
    void saveFullCohortReview(CohortReview cohortReview, List<ParticipantCohortStatus> participantCohortStatuses);

    /**
     * Save the specified {@link ParticipantCohortStatus}.
     *
     * @param participantCohortStatus
     * @return
     */
    ParticipantCohortStatus saveParticipantCohortStatus(ParticipantCohortStatus participantCohortStatus);

    /**
     * Find the {@link ParticipantCohortStatus} for the specified cohortReviewId and participantId.
     *
     * @param cohortReviewId
     * @param participantId
     * @return
     */
    ParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId);

    List<ParticipantCohortStatus> findAll(Long cohortReviewId, List<Filter> filtersList, PageRequest pageRequest);

    ParticipantCohortAnnotation saveParticipantCohortAnnotation(ParticipantCohortAnnotation participantCohortAnnotation);

    CohortAnnotationDefinition findCohortAnnotationDefinition(Long cohortAnnotationDefinitionId);

    void deleteParticipantCohortAnnotation(Long annotationId, Long cohortReviewId, Long participantId);

    ParticipantCohortAnnotation findParticipantCohortAnnotation(Long annotationId);

    ParticipantCohortAnnotation findParticipantCohortAnnotation(Long cohortReviewId, Long cohortAnnotationDefinitionId, Long participantId);

    List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(Long participantId);
}
