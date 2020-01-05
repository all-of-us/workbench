package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.SurveyAnswerResponse;
import org.pmiops.workbench.model.SurveyQuestionsResponse;
import org.pmiops.workbench.model.SurveysResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link ConceptsApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link ConceptsApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public interface ConceptsApiDelegate {

    /**
     * @see ConceptsApi#getDomainInfo
     */
    ResponseEntity<DomainInfoResponse> getDomainInfo(String workspaceNamespace,
        String workspaceId);

    /**
     * @see ConceptsApi#getSurveyAnswers
     */
    ResponseEntity<List<SurveyAnswerResponse>> getSurveyAnswers(String workspaceNamespace,
        String workspaceId,
        Long questionConceptId);

    /**
     * @see ConceptsApi#getSurveyInfo
     */
    ResponseEntity<SurveysResponse> getSurveyInfo(String workspaceNamespace,
        String workspaceId);

    /**
     * @see ConceptsApi#getSurveyQuestions
     */
    ResponseEntity<List<SurveyQuestionsResponse>> getSurveyQuestions(String workspaceNamespace,
        String workspaceId,
        String surveyName);

    /**
     * @see ConceptsApi#searchConcepts
     */
    ResponseEntity<ConceptListResponse> searchConcepts(String workspaceNamespace,
        String workspaceId,
        SearchConceptsRequest request);

}
