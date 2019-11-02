package org.pmiops.workbench.cohortreview;

import java.util.List;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public interface CohortReviewService {

  /**
   * Find the {@link DbCohort} for the specified cohortId.
   *
   * @param cohortId
   * @return
   */
  DbCohort findCohort(long cohortId);

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
   * Find the {@link DbCohortReview} for the specified cohortId and cdrVersionId.
   *
   * @param cohortId
   * @param cdrVersionId
   * @return
   */
  DbCohortReview findCohortReview(Long cohortId, Long cdrVersionId);

  /**
   * Find the {@link DbCohortReview} for the specified cohortReviewId.
   *
   * @param cohortReviewId
   * @return
   */
  DbCohortReview findCohortReview(Long cohortReviewId);

  /**
   * Find the {@link DbCohortReview} for the specified ns, firecloudName and cohortReviewId
   *
   * @param ns
   * @param firecloudName
   * @param cohortReviewId
   * @return
   */
  DbCohortReview findCohortReview(String ns, String firecloudName, Long cohortReviewId);

  /**
   * Delete the specified cohort review.
   *
   * @param cohortReview
   */
  void deleteCohortReview(DbCohortReview cohortReview);

  /**
   * Find the {@link DbCohortReview} for the specified ns and firecloudName.
   *
   * @param ns
   * @param firecloudName
   * @return
   */
  List<DbCohortReview> getRequiredWithCohortReviews(String ns, String firecloudName);

  /**
   * Save the specified {@link DbCohortReview}.
   *
   * @param cohortReview
   * @return
   */
  DbCohortReview saveCohortReview(DbCohortReview cohortReview);

  /**
   * Save the {@link DbCohortReview} as well as the collection of {@link DbParticipantCohortStatus}.
   *
   * @param cohortReview
   * @param participantCohortStatuses
   */
  void saveFullCohortReview(
      DbCohortReview cohortReview, List<DbParticipantCohortStatus> participantCohortStatuses);

  /**
   * Save the specified {@link DbParticipantCohortStatus}.
   *
   * @param participantCohortStatus
   * @return
   */
  DbParticipantCohortStatus saveParticipantCohortStatus(
      DbParticipantCohortStatus participantCohortStatus);

  /**
   * Find the {@link DbParticipantCohortStatus} for the specified cohortReviewId and participantId.
   *
   * @param cohortReviewId
   * @param participantId
   * @return
   */
  DbParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId);

  /**
   * Find a list of {@link DbParticipantCohortStatus} for the specified cohortReviewId, filtering
   * and paging.
   *
   * @param cohortReviewId
   * @param pageRequest
   * @return
   */
  List<DbParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest);

  /**
   * Find count for the specified cohortReviewId, filtering and paging.
   *
   * @param cohortReviewId
   * @param pageRequest
   * @return
   */
  Long findCount(Long cohortReviewId, PageRequest pageRequest);

  /**
   * Save the specified {@link DbParticipantCohortAnnotation}.
   *
   * @param cohortReviewId
   * @param participantCohortAnnotation
   * @return
   */
  DbParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, DbParticipantCohortAnnotation participantCohortAnnotation);

  /**
   * Save the {@link DbParticipantCohortAnnotation} for the specified participantId, annotationId
   * and {@link ModifyParticipantCohortAnnotationRequest}.
   *
   * @param annotationId
   * @param participantId
   * @param modifyParticipantCohortAnnotationRequest
   * @return
   */
  DbParticipantCohortAnnotation updateParticipantCohortAnnotation(
      Long annotationId,
      Long cohortReviewId,
      Long participantId,
      ModifyParticipantCohortAnnotationRequest modifyParticipantCohortAnnotationRequest);

  /**
   * Find the {@link DbCohortAnnotationDefinition} for the specified cohortAnnotationDefinitionId.
   *
   * @param cohortAnnotationDefinitionId
   * @return
   */
  DbCohortAnnotationDefinition findCohortAnnotationDefinition(Long cohortAnnotationDefinitionId);

  /**
   * Delete the {@link DbParticipantCohortAnnotation} for the specified annotationId, cohortReviewId
   * and participantId.
   *
   * @param annotationId
   * @param cohortReviewId
   * @param participantId
   */
  void deleteParticipantCohortAnnotation(
      Long annotationId, Long cohortReviewId, Long participantId);

  /**
   * Find the {@link DbParticipantCohortAnnotation} for the specified cohortReviewId,
   * cohortAnnotationDefinitonId and participantId.
   *
   * @param cohortReviewId
   * @param cohortAnnotationDefinitionId
   * @param participantId
   * @return
   */
  DbParticipantCohortAnnotation findParticipantCohortAnnotation(
      Long cohortReviewId, Long cohortAnnotationDefinitionId, Long participantId);

  /**
   * Find a list of {@link DbParticipantCohortAnnotation} for the specified cohortReviewId and
   * participantId.
   *
   * @param cohortReviewId
   * @param participantId
   * @return
   */
  List<DbParticipantCohortAnnotation> findParticipantCohortAnnotations(
      Long cohortReviewId, Long participantId);
}
