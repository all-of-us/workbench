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
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link CohortsApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link CohortsApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public interface CohortsApiDelegate {

    /**
     * @see CohortsApi#createCohort
     */
    ResponseEntity<Cohort> createCohort(String workspaceNamespace,
        String workspaceId,
        Cohort cohort);

    /**
     * @see CohortsApi#deleteCohort
     */
    ResponseEntity<EmptyResponse> deleteCohort(String workspaceNamespace,
        String workspaceId,
        Long cohortId);

    /**
     * @see CohortsApi#duplicateCohort
     */
    ResponseEntity<Cohort> duplicateCohort(String workspaceNamespace,
        String workspaceId,
        DuplicateCohortRequest duplicateCohortRequest);

    /**
     * @see CohortsApi#getCohort
     */
    ResponseEntity<Cohort> getCohort(String workspaceNamespace,
        String workspaceId,
        Long cohortId);

    /**
     * @see CohortsApi#getCohortAnnotations
     */
    ResponseEntity<CohortAnnotationsResponse> getCohortAnnotations(String workspaceNamespace,
        String workspaceId,
        CohortAnnotationsRequest request);

    /**
     * @see CohortsApi#getCohortsInWorkspace
     */
    ResponseEntity<CohortListResponse> getCohortsInWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see CohortsApi#getDataTableQuery
     */
    ResponseEntity<CdrQuery> getDataTableQuery(String workspaceNamespace,
        String workspaceId,
        DataTableSpecification request);

    /**
     * @see CohortsApi#materializeCohort
     */
    ResponseEntity<MaterializeCohortResponse> materializeCohort(String workspaceNamespace,
        String workspaceId,
        MaterializeCohortRequest request);

    /**
     * @see CohortsApi#updateCohort
     */
    ResponseEntity<Cohort> updateCohort(String workspaceNamespace,
        String workspaceId,
        Long cohortId,
        Cohort cohort);

}
