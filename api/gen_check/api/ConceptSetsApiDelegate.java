package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateConceptSetRequest;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link ConceptSetsApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link ConceptSetsApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public interface ConceptSetsApiDelegate {

    /**
     * @see ConceptSetsApi#copyConceptSet
     */
    ResponseEntity<ConceptSet> copyConceptSet(String workspaceNamespace,
        String workspaceId,
        String conceptSetId,
        CopyRequest copyConceptSetRequest);

    /**
     * @see ConceptSetsApi#createConceptSet
     */
    ResponseEntity<ConceptSet> createConceptSet(String workspaceNamespace,
        String workspaceId,
        CreateConceptSetRequest request);

    /**
     * @see ConceptSetsApi#deleteConceptSet
     */
    ResponseEntity<EmptyResponse> deleteConceptSet(String workspaceNamespace,
        String workspaceId,
        Long conceptSetId);

    /**
     * @see ConceptSetsApi#getConceptSet
     */
    ResponseEntity<ConceptSet> getConceptSet(String workspaceNamespace,
        String workspaceId,
        Long conceptSetId);

    /**
     * @see ConceptSetsApi#getConceptSetsInWorkspace
     */
    ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see ConceptSetsApi#getSurveyConceptSetsInWorkspace
     */
    ResponseEntity<ConceptSetListResponse> getSurveyConceptSetsInWorkspace(String workspaceNamespace,
        String workspaceId,
        String surveyName);

    /**
     * @see ConceptSetsApi#updateConceptSet
     */
    ResponseEntity<ConceptSet> updateConceptSet(String workspaceNamespace,
        String workspaceId,
        Long conceptSetId,
        ConceptSet conceptSet);

    /**
     * @see ConceptSetsApi#updateConceptSetConcepts
     */
    ResponseEntity<ConceptSet> updateConceptSetConcepts(String workspaceNamespace,
        String workspaceId,
        Long conceptSetId,
        UpdateConceptSetRequest request);

}
