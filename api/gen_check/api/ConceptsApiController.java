package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.SurveyAnswerResponse;
import org.pmiops.workbench.model.SurveyQuestionsResponse;
import org.pmiops.workbench.model.SurveysResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

@Controller
public class ConceptsApiController implements ConceptsApi {
    private final ConceptsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public ConceptsApiController(ConceptsApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<DomainInfoResponse> getDomainInfo(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getDomainInfo(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<List<SurveyAnswerResponse>> getSurveyAnswers(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
         @NotNull@ApiParam(value = "", required = true) @RequestParam(value = "questionConceptId", required = true) Long questionConceptId) {
        // do some magic!
        return delegate.getSurveyAnswers(workspaceNamespace, workspaceId, questionConceptId);
    }

    public ResponseEntity<SurveysResponse> getSurveyInfo(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getSurveyInfo(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<List<SurveyQuestionsResponse>> getSurveyQuestions(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("surveyName") String surveyName) {
        // do some magic!
        return delegate.getSurveyQuestions(workspaceNamespace, workspaceId, surveyName);
    }

    public ResponseEntity<ConceptListResponse> searchConcepts(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "concept search request"  )  @Valid @RequestBody SearchConceptsRequest request) {
        // do some magic!
        return delegate.searchConcepts(workspaceNamespace, workspaceId, request);
    }

}
