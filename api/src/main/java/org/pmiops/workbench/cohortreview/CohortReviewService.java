package org.pmiops.workbench.cohortreview;

import java.util.List;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.CohortDataModel;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public interface CohortReviewService {

  /**
   * Find the {@link CohortDataModel} for the specified cohortId.
   *
   * @param cohortId
   * @return
   */
  CohortDataModel findCohort(long cohortId);

  /**
   * Validate that a workspace exists for the specified workspaceId, and set the workspace's. CDR
   * version in {@link org.pmiops.workbench.cdr.CdrVersionContext}.
   *
   * @param workspaceNamespace
   * @param workspaceName
   * @param workspaceId
   */
  DbWorkspace validateMatchingWorkspaceAndSetCdrVersion(
      String workspaceNamespace,
      String workspaceName,
      long workspaceId,
      WorkspaceAccessLevel requiredAccess);

  WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess);

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
   * Find the {@link CohortReview} for the specified ns, firecloudName and cohortReviewId
   *
   * @param ns
   * @param firecloudName
   * @param cohortReviewId
   * @return
   */
  CohortReview findCohortReview(String ns, String firecloudName, Long cohortReviewId);

  /**
   * Delete the specified cohort review.
   *
   * @param cohortReview
   */
  void deleteCohortReview(CohortReview cohortReview);

  /**
   * Find the {@link CohortReview} for the specified ns and firecloudName.
   *
   * @param ns
   * @param firecloudName
   * @return
   */
  List<CohortReview> getRequiredWithCohortReviews(String ns, String firecloudName);

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
  void saveFullCohortReview(
      CohortReview cohortReview, List<ParticipantCohortStatus> participantCohortStatuses);

  /**
   * Save the specified {@link ParticipantCohortStatus}.
   *
   * @param participantCohortStatus
   * @return
   */
  ParticipantCohortStatus saveParticipantCohortStatus(
      ParticipantCohortStatus participantCohortStatus);

  /**
   * Find the {@link ParticipantCohortStatus} for the specified cohortReviewId and participantId.
   *
   * @param cohortReviewId
   * @param participantId
   * @return
   */
  ParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId);

  /**
   * Find a list of {@link ParticipantCohortStatus} for the specified cohortReviewId, filtering and
   * paging.
   *
   * @param cohortReviewId
   * @param pageRequest
   * @return
   */
  List<ParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest);

  /**
   * Find count for the specified cohortReviewId, filtering and paging.
   *
   * @param cohortReviewId
   * @param pageRequest
   * @return
   */
  Long findCount(Long cohortReviewId, PageRequest pageRequest);

  /**
   * Save the specified {@link ParticipantCohortAnnotation}.
   *
   * @param cohortReviewId
   * @param participantCohortAnnotation
   * @return
   */
  ParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, ParticipantCohortAnnotation participantCohortAnnotation);

  /**
   * Save the {@link ParticipantCohortAnnotation} for the specified participantId, annotationId and
   * {@link ModifyParticipantCohortAnnotationRequest}.
   *
   * @param annotationId
   * @param participantId
   * @param modifyParticipantCohortAnnotationRequest
   * @return
   */
  ParticipantCohortAnnotation updateParticipantCohortAnnotation(
      Long annotationId,
      Long cohortReviewId,
      Long participantId,
      ModifyParticipantCohortAnnotationRequest modifyParticipantCohortAnnotationRequest);

  /**
   * Find the {@link CohortAnnotationDefinition} for the specified cohortAnnotationDefinitionId.
   *
   * @param cohortAnnotationDefinitionId
   * @return
   */
  CohortAnnotationDefinition findCohortAnnotationDefinition(Long cohortAnnotationDefinitionId);

  /**
   * Delete the {@link ParticipantCohortAnnotation} for the specified annotationId, cohortReviewId
   * and participantId.
   *
   * @param annotationId
   * @param cohortReviewId
   * @param participantId
   */
  void deleteParticipantCohortAnnotation(
      Long annotationId, Long cohortReviewId, Long participantId);

  /**
   * Find the {@link ParticipantCohortAnnotation} for the specified cohortReviewId,
   * cohortAnnotationDefinitonId and participantId.
   *
   * @param cohortReviewId
   * @param cohortAnnotationDefinitionId
   * @param participantId
   * @return
   */
  ParticipantCohortAnnotation findParticipantCohortAnnotation(
      Long cohortReviewId, Long cohortAnnotationDefinitionId, Long participantId);

  /**
   * Find a list of {@link ParticipantCohortAnnotation} for the specified cohortReviewId and
   * participantId.
   *
   * @param cohortReviewId
   * @param participantId
   * @return
   */
  List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(
      Long cohortReviewId, Long participantId);
}
