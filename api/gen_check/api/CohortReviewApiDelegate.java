package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortReviewListResponse;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.VocabularyListResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link CohortReviewApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link CohortReviewApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public interface CohortReviewApiDelegate {

    /**
     * @see CohortReviewApi#createCohortReview
     */
    ResponseEntity<CohortReview> createCohortReview(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        Long cdrVersionId,
        CreateReviewRequest request);

    /**
     * @see CohortReviewApi#createParticipantCohortAnnotation
     */
    ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId,
        ParticipantCohortAnnotation request);

    /**
     * @see CohortReviewApi#deleteCohortReview
     */
    ResponseEntity<EmptyResponse> deleteCohortReview(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId);

    /**
     * @see CohortReviewApi#deleteParticipantCohortAnnotation
     */
    ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId,
        Long annotationId);

    /**
     * @see CohortReviewApi#getCohortChartData
     */
    ResponseEntity<CohortChartDataListResponse> getCohortChartData(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        String domain,
        Integer limit);

    /**
     * @see CohortReviewApi#getCohortReviewsInWorkspace
     */
    ResponseEntity<CohortReviewListResponse> getCohortReviewsInWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see CohortReviewApi#getParticipantChartData
     */
    ResponseEntity<ParticipantChartDataListResponse> getParticipantChartData(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId,
        String domain,
        Integer limit);

    /**
     * @see CohortReviewApi#getParticipantCohortAnnotations
     */
    ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId);

    /**
     * @see CohortReviewApi#getParticipantCohortStatus
     */
    ResponseEntity<ParticipantCohortStatus> getParticipantCohortStatus(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId);

    /**
     * @see CohortReviewApi#getParticipantCohortStatuses
     */
    ResponseEntity<CohortReview> getParticipantCohortStatuses(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        Long cdrVersionId,
        PageFilterRequest request);

    /**
     * @see CohortReviewApi#getParticipantData
     */
    ResponseEntity<ParticipantDataListResponse> getParticipantData(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId,
        PageFilterRequest request);

    /**
     * @see CohortReviewApi#getVocabularies
     */
    ResponseEntity<VocabularyListResponse> getVocabularies(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId);

    /**
     * @see CohortReviewApi#updateCohortReview
     */
    ResponseEntity<CohortReview> updateCohortReview(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        CohortReview cohortReview);

    /**
     * @see CohortReviewApi#updateParticipantCohortAnnotation
     */
    ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId,
        Long annotationId,
        ModifyParticipantCohortAnnotationRequest request);

    /**
     * @see CohortReviewApi#updateParticipantCohortStatus
     */
    ResponseEntity<ParticipantCohortStatus> updateParticipantCohortStatus(String workspaceNamespace,
        String workspaceId,
        Long cohortReviewId,
        Long participantId,
        ModifyCohortStatusRequest cohortStatusRequest);

}
