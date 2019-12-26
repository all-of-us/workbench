package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CdrQuery;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortAnnotationsRequest;
import org.pmiops.workbench.model.CohortAnnotationsResponse;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.DataTableSpecification;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;

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
public class CohortsApiController implements CohortsApi {
    private final CohortsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public CohortsApiController(CohortsApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<Cohort> createCohort(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "cohort definition"  )  @Valid @RequestBody Cohort cohort) {
        // do some magic!
        return delegate.createCohort(workspaceNamespace, workspaceId, cohort);
    }

    public ResponseEntity<EmptyResponse> deleteCohort(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId) {
        // do some magic!
        return delegate.deleteCohort(workspaceNamespace, workspaceId, cohortId);
    }

    public ResponseEntity<Cohort> duplicateCohort(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Duplicate Cohort Request"  )  @Valid @RequestBody DuplicateCohortRequest duplicateCohortRequest) {
        // do some magic!
        return delegate.duplicateCohort(workspaceNamespace, workspaceId, duplicateCohortRequest);
    }

    public ResponseEntity<Cohort> getCohort(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId) {
        // do some magic!
        return delegate.getCohort(workspaceNamespace, workspaceId, cohortId);
    }

    public ResponseEntity<CohortAnnotationsResponse> getCohortAnnotations(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "a request indicating what annotations to retrieve"  )  @Valid @RequestBody CohortAnnotationsRequest request) {
        // do some magic!
        return delegate.getCohortAnnotations(workspaceNamespace, workspaceId, request);
    }

    public ResponseEntity<CohortListResponse> getCohortsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getCohortsInWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<CdrQuery> getDataTableQuery(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "a query specification for a data table"  )  @Valid @RequestBody DataTableSpecification request) {
        // do some magic!
        return delegate.getDataTableQuery(workspaceNamespace, workspaceId, request);
    }

    public ResponseEntity<MaterializeCohortResponse> materializeCohort(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "cohort materialization request"  )  @Valid @RequestBody MaterializeCohortRequest request) {
        // do some magic!
        return delegate.materializeCohort(workspaceNamespace, workspaceId, request);
    }

    public ResponseEntity<Cohort> updateCohort(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Cohort ID",required=true ) @PathVariable("cohortId") Long cohortId,
        @ApiParam(value = "cohort definition"  )  @Valid @RequestBody Cohort cohort) {
        // do some magic!
        return delegate.updateCohort(workspaceNamespace, workspaceId, cohortId, cohort);
    }

}
