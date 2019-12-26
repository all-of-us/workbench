package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.EmptyResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link CohortAnnotationDefinitionApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link CohortAnnotationDefinitionApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public interface CohortAnnotationDefinitionApiDelegate {

    /**
     * @see CohortAnnotationDefinitionApi#createCohortAnnotationDefinition
     */
    ResponseEntity<CohortAnnotationDefinition> createCohortAnnotationDefinition(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        CohortAnnotationDefinition request);

    /**
     * @see CohortAnnotationDefinitionApi#deleteCohortAnnotationDefinition
     */
    ResponseEntity<EmptyResponse> deleteCohortAnnotationDefinition(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        Long annotationDefinitionId);

    /**
     * @see CohortAnnotationDefinitionApi#getCohortAnnotationDefinition
     */
    ResponseEntity<CohortAnnotationDefinition> getCohortAnnotationDefinition(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        Long annotationDefinitionId);

    /**
     * @see CohortAnnotationDefinitionApi#getCohortAnnotationDefinitions
     */
    ResponseEntity<CohortAnnotationDefinitionListResponse> getCohortAnnotationDefinitions(String workspaceNamespace,
        String workspaceId,
        Long cohortId);

    /**
     * @see CohortAnnotationDefinitionApi#updateCohortAnnotationDefinition
     */
    ResponseEntity<CohortAnnotationDefinition> updateCohortAnnotationDefinition(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        Long annotationDefinitionId,
        CohortAnnotationDefinition cohortAnnotationDefinition);

}
