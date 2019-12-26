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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

@Controller
public class CohortReviewApiController implements CohortReviewApi {
    private final CohortReviewApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public CohortReviewApiController(CohortReviewApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<CohortReview> createCohortReview(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "cohort review creation request body" ,required=true )  @Valid @RequestBody CreateReviewRequest request) {
        // do some magic!
        return delegate.createCohortReview(workspaceNamespace, workspaceId, cohortId, cdrVersionId, request);
    }

    public ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,
        @ApiParam(value = "ParticipantCohortAnnotation creation request body" ,required=true )  @Valid @RequestBody ParticipantCohortAnnotation request) {
        // do some magic!
        return delegate.createParticipantCohortAnnotation(workspaceNamespace, workspaceId, cohortReviewId, participantId, request);
    }

    public ResponseEntity<EmptyResponse> deleteCohortReview(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "specifies which cohort review",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId) {
        // do some magic!
        return delegate.deleteCohortReview(workspaceNamespace, workspaceId, cohortReviewId);
    }

    public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,
        @ApiParam(value = "specifies which annotation",required=true ) @PathVariable("annotationId") Long annotationId) {
        // do some magic!
        return delegate.deleteParticipantCohortAnnotation(workspaceNamespace, workspaceId, cohortReviewId, participantId, annotationId);
    }

    public ResponseEntity<CohortChartDataListResponse> getCohortChartData(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which domain the CohortChartData should belong to.",required=true ) @PathVariable("domain") String domain,
        @ApiParam(value = "the limit search results to") @RequestParam(value = "limit", required = false) Integer limit) {
        // do some magic!
        return delegate.getCohortChartData(workspaceNamespace, workspaceId, cohortReviewId, domain, limit);
    }

    public ResponseEntity<CohortReviewListResponse> getCohortReviewsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getCohortReviewsInWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<ParticipantChartDataListResponse> getParticipantChartData(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,
        @ApiParam(value = "specifies which domain the chart data should belong to.",required=true ) @PathVariable("domain") String domain,
        @ApiParam(value = "the limit search results to") @RequestParam(value = "limit", required = false) Integer limit) {
        // do some magic!
        return delegate.getParticipantChartData(workspaceNamespace, workspaceId, cohortReviewId, participantId, domain, limit);
    }

    public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId) {
        // do some magic!
        return delegate.getParticipantCohortAnnotations(workspaceNamespace, workspaceId, cohortReviewId, participantId);
    }

    public ResponseEntity<ParticipantCohortStatus> getParticipantCohortStatus(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId) {
        // do some magic!
        return delegate.getParticipantCohortStatus(workspaceNamespace, workspaceId, cohortReviewId, participantId);
    }

    public ResponseEntity<CohortReview> getParticipantCohortStatuses(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "request body for getting list of ParticipantCohortStatuses." ,required=true )  @Valid @RequestBody PageFilterRequest request) {
        // do some magic!
        return delegate.getParticipantCohortStatuses(workspaceNamespace, workspaceId, cohortId, cdrVersionId, request);
    }

    public ResponseEntity<ParticipantDataListResponse> getParticipantData(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,
        @ApiParam(value = "request body for getting list of participant data." ,required=true )  @Valid @RequestBody PageFilterRequest request) {
        // do some magic!
        return delegate.getParticipantData(workspaceNamespace, workspaceId, cohortReviewId, participantId, request);
    }

    public ResponseEntity<VocabularyListResponse> getVocabularies(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId) {
        // do some magic!
        return delegate.getVocabularies(workspaceNamespace, workspaceId, cohortReviewId);
    }

    public ResponseEntity<CohortReview> updateCohortReview(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "specifies which cohort review",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "cohort review"  )  @Valid @RequestBody CohortReview cohortReview) {
        // do some magic!
        return delegate.updateCohortReview(workspaceNamespace, workspaceId, cohortReviewId, cohortReview);
    }

    public ResponseEntity<ParticipantCohortAnnotation> updateParticipantCohortAnnotation(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,
        @ApiParam(value = "specifies which annotation",required=true ) @PathVariable("annotationId") Long annotationId,
        @ApiParam(value = "ParticipantCohortAnnotation modification request body" ,required=true )  @Valid @RequestBody ModifyParticipantCohortAnnotationRequest request) {
        // do some magic!
        return delegate.updateParticipantCohortAnnotation(workspaceNamespace, workspaceId, cohortReviewId, participantId, annotationId, request);
    }

    public ResponseEntity<ParticipantCohortStatus> updateParticipantCohortStatus(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort Review ID",required=true ) @PathVariable("cohortReviewId") Long cohortReviewId,
        @ApiParam(value = "specifies which participant",required=true ) @PathVariable("participantId") Long participantId,
        @ApiParam(value = "Contains the new review status"  )  @Valid @RequestBody ModifyCohortStatusRequest cohortStatusRequest) {
        // do some magic!
        return delegate.updateParticipantCohortStatus(workspaceNamespace, workspaceId, cohortReviewId, participantId, cohortStatusRequest);
    }

}
