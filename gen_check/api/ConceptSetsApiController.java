package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateConceptSetRequest;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

@Controller
public class ConceptSetsApiController implements ConceptSetsApi {
    private final ConceptSetsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public ConceptSetsApiController(ConceptSetsApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<ConceptSet> copyConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("conceptSetId") String conceptSetId,
        @ApiParam(value = "" ,required=true )  @Valid @RequestBody CopyRequest copyConceptSetRequest) {
        // do some magic!
        return delegate.copyConceptSet(workspaceNamespace, workspaceId, conceptSetId, copyConceptSetRequest);
    }

    public ResponseEntity<ConceptSet> createConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "create concept set request"  )  @Valid @RequestBody CreateConceptSetRequest request) {
        // do some magic!
        return delegate.createConceptSet(workspaceNamespace, workspaceId, request);
    }

    public ResponseEntity<EmptyResponse> deleteConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId) {
        // do some magic!
        return delegate.deleteConceptSet(workspaceNamespace, workspaceId, conceptSetId);
    }

    public ResponseEntity<ConceptSet> getConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId) {
        // do some magic!
        return delegate.getConceptSet(workspaceNamespace, workspaceId, conceptSetId);
    }

    public ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getConceptSetsInWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<ConceptSetListResponse> getSurveyConceptSetsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("surveyName") String surveyName) {
        // do some magic!
        return delegate.getSurveyConceptSetsInWorkspace(workspaceNamespace, workspaceId, surveyName);
    }

    public ResponseEntity<ConceptSet> updateConceptSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId,
        @ApiParam(value = "concept set definition"  )  @Valid @RequestBody ConceptSet conceptSet) {
        // do some magic!
        return delegate.updateConceptSet(workspaceNamespace, workspaceId, conceptSetId, conceptSet);
    }

    public ResponseEntity<ConceptSet> updateConceptSetConcepts(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Concept set ID",required=true ) @PathVariable("conceptSetId") Long conceptSetId,
        @ApiParam(value = "update concept set request"  )  @Valid @RequestBody UpdateConceptSetRequest request) {
        // do some magic!
        return delegate.updateConceptSetConcepts(workspaceNamespace, workspaceId, conceptSetId, request);
    }

}
