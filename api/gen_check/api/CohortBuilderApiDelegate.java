package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaMenuOptionsListResponse;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link CohortBuilderApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link CohortBuilderApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public interface CohortBuilderApiDelegate {

    /**
     * @see CohortBuilderApi#countParticipants
     */
    ResponseEntity<Long> countParticipants(Long cdrVersionId,
        SearchRequest request);

    /**
     * @see CohortBuilderApi#findCriteriaByDomainAndSearchTerm
     */
    ResponseEntity<CriteriaListResponse> findCriteriaByDomainAndSearchTerm(Long cdrVersionId,
        String domain,
        String term,
        Integer limit);

    /**
     * @see CohortBuilderApi#findCriteriaMenuOptions
     */
    ResponseEntity<CriteriaMenuOptionsListResponse> findCriteriaMenuOptions(Long cdrVersionId);

    /**
     * @see CohortBuilderApi#getCriteriaAttributeByConceptId
     */
    ResponseEntity<CriteriaAttributeListResponse> getCriteriaAttributeByConceptId(Long cdrVersionId,
        Long conceptId);

    /**
     * @see CohortBuilderApi#getCriteriaAutoComplete
     */
    ResponseEntity<CriteriaListResponse> getCriteriaAutoComplete(Long cdrVersionId,
        String domain,
        String term,
        String type,
        Boolean standard,
        Integer limit);

    /**
     * @see CohortBuilderApi#getCriteriaBy
     */
    ResponseEntity<CriteriaListResponse> getCriteriaBy(Long cdrVersionId,
        String domain,
        String type,
        Boolean standard,
        Long parentId);

    /**
     * @see CohortBuilderApi#getDemoChartInfo
     */
    ResponseEntity<DemoChartInfoListResponse> getDemoChartInfo(Long cdrVersionId,
        SearchRequest request);

    /**
     * @see CohortBuilderApi#getDrugBrandOrIngredientByValue
     */
    ResponseEntity<CriteriaListResponse> getDrugBrandOrIngredientByValue(Long cdrVersionId,
        String value,
        Integer limit);

    /**
     * @see CohortBuilderApi#getDrugIngredientByConceptId
     */
    ResponseEntity<CriteriaListResponse> getDrugIngredientByConceptId(Long cdrVersionId,
        Long conceptId);

    /**
     * @see CohortBuilderApi#getParticipantDemographics
     */
    ResponseEntity<ParticipantDemographics> getParticipantDemographics(Long cdrVersionId);

    /**
     * @see CohortBuilderApi#getStandardCriteriaByDomainAndConceptId
     */
    ResponseEntity<CriteriaListResponse> getStandardCriteriaByDomainAndConceptId(Long cdrVersionId,
        String domain,
        Long conceptId);

}
