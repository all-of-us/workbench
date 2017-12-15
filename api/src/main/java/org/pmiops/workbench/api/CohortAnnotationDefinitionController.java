package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ModifyCohortAnnotationDefinitionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortAnnotationDefinitionController implements CohortAnnotationDefinitionApiDelegate {

    @Override
    public ResponseEntity<CohortAnnotationDefinition> createCohortAnnotationDefinition(String workspaceNamespace, String workspaceId, Long cohortId, CohortAnnotationDefinition request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortAnnotationDefinition());
    }

    @Override
    public ResponseEntity<EmptyResponse> deleteCohortAnnotationDefinition(String workspaceNamespace, String workspaceId, Long cohortId, Long annotationDefinitionId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new EmptyResponse());
    }

    @Override
    public ResponseEntity<CohortAnnotationDefinitionListResponse> getCohortAnnotationDefinitions(String workspaceNamespace, String workspaceId, Long cohortId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortAnnotationDefinitionListResponse());
    }

    @Override
    public ResponseEntity<CohortAnnotationDefinition> updateCohortAnnotationDefinition(String workspaceNamespace, String workspaceId, Long cohortId, Long annotationDefinitionId, ModifyCohortAnnotationDefinitionRequest modifyCohortAnnotationDefinitionRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortAnnotationDefinition());
    }
}
