package org.pmiops.workbench.cohortreview;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

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
    void validateMatchingWorkspace(String workspaceNamespace, String workspaceName, long workspaceId);

    /**
     * Find the {@link CohortReview} for the specified cohortId and cdrVersionId.
     *
     * @param cohortId
     * @param cdrVersionId
     * @return
     */
    CohortReview findCohortReview(Long cohortId, Long cdrVersionId);

    /**
     * Find the {@link CohortReview} for the specified cohortReviewId.
     *
     * @param cohortReviewId
     * @return
     */
    CohortReview findCohortReview(Long cohortReviewId);

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

    /**
     * Find all {@link ParticipantCohortStatus} for the specified cohortReviewId and pageRequest.
     *
     * @param cohortReviewId
     * @param pageRequest
     * @return
     */
    Slice<ParticipantCohortStatus> findParticipantCohortStatuses(Long cohortReviewId, PageRequest pageRequest);
}
