package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.EmptyResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

@Controller
public class CohortAnnotationDefinitionApiController implements CohortAnnotationDefinitionApi {
    private final CohortAnnotationDefinitionApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public CohortAnnotationDefinitionApiController(CohortAnnotationDefinitionApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<CohortAnnotationDefinition> createCohortAnnotationDefinition(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "CohortAnnotationDefinition creation request body" ,required=true )  @Valid @RequestBody CohortAnnotationDefinition request) {
        // do some magic!
        return delegate.createCohortAnnotationDefinition(workspaceNamespace, workspaceId, cohortId, request);
    }

    public ResponseEntity<EmptyResponse> deleteCohortAnnotationDefinition(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "specifies which CohortAnnotationDefinition.",required=true ) @PathVariable("annotationDefinitionId") Long annotationDefinitionId) {
        // do some magic!
        return delegate.deleteCohortAnnotationDefinition(workspaceNamespace, workspaceId, cohortId, annotationDefinitionId);
    }

    public ResponseEntity<CohortAnnotationDefinition> getCohortAnnotationDefinition(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "specifies which CohortAnnotationDefinition.",required=true ) @PathVariable("annotationDefinitionId") Long annotationDefinitionId) {
        // do some magic!
        return delegate.getCohortAnnotationDefinition(workspaceNamespace, workspaceId, cohortId, annotationDefinitionId);
    }

    public ResponseEntity<CohortAnnotationDefinitionListResponse> getCohortAnnotationDefinitions(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId) {
        // do some magic!
        return delegate.getCohortAnnotationDefinitions(workspaceNamespace, workspaceId, cohortId);
    }

    public ResponseEntity<CohortAnnotationDefinition> updateCohortAnnotationDefinition(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "specifies which CohortAnnotationDefinition.",required=true ) @PathVariable("annotationDefinitionId") Long annotationDefinitionId,
        @ApiParam(value = "Contains the new CohortAnnotationDefinition"  )  @Valid @RequestBody CohortAnnotationDefinition cohortAnnotationDefinition) {
        // do some magic!
        return delegate.updateCohortAnnotationDefinition(workspaceNamespace, workspaceId, cohortId, annotationDefinitionId, cohortAnnotationDefinition);
    }

}
