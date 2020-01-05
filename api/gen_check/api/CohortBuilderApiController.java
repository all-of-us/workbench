package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaMenuOptionsListResponse;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

@Controller
public class CohortBuilderApiController implements CohortBuilderApi {
    private final CohortBuilderApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public CohortBuilderApiController(CohortBuilderApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<Long> countParticipants(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "object of parameters by which to perform the search" ,required=true )  @Valid @RequestBody SearchRequest request) {
        // do some magic!
        return delegate.countParticipants(cdrVersionId, request);
    }

    public ResponseEntity<CriteriaListResponse> findCriteriaByDomainAndSearchTerm(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "the specific type of domain",required=true ) @PathVariable("domain") String domain,
         @NotNull@ApiParam(value = "the term to search for", required = true) @RequestParam(value = "term", required = true) String term,
        @ApiParam(value = "number of criteria matches to return") @RequestParam(value = "limit", required = false) Integer limit) {
        // do some magic!
        return delegate.findCriteriaByDomainAndSearchTerm(cdrVersionId, domain, term, limit);
    }

    public ResponseEntity<CriteriaMenuOptionsListResponse> findCriteriaMenuOptions(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId) {
        // do some magic!
        return delegate.findCriteriaMenuOptions(cdrVersionId);
    }

    public ResponseEntity<CriteriaAttributeListResponse> getCriteriaAttributeByConceptId(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "conceptId of brand",required=true ) @PathVariable("conceptId") Long conceptId) {
        // do some magic!
        return delegate.getCriteriaAttributeByConceptId(cdrVersionId, conceptId);
    }

    public ResponseEntity<CriteriaListResponse> getCriteriaAutoComplete(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "the specific domain of criteria to get",required=true ) @PathVariable("domain") String domain,
         @NotNull@ApiParam(value = "the term to search for", required = true) @RequestParam(value = "term", required = true) String term,
        @ApiParam(value = "the type of the criteria were search for") @RequestParam(value = "type", required = false) String type,
        @ApiParam(value = "the type of the criteria were search for", defaultValue = "false") @RequestParam(value = "standard", required = false, defaultValue="false") Boolean standard,
        @ApiParam(value = "number of criteria matches to return") @RequestParam(value = "limit", required = false) Integer limit) {
        // do some magic!
        return delegate.getCriteriaAutoComplete(cdrVersionId, domain, term, type, standard, limit);
    }

    public ResponseEntity<CriteriaListResponse> getCriteriaBy(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "the specific domain of criteria to get",required=true ) @PathVariable("domain") String domain,
        @ApiParam(value = "the specific type of criteria to get") @RequestParam(value = "type", required = false) String type,
        @ApiParam(value = "reveals if source or standard", defaultValue = "false") @RequestParam(value = "standard", required = false, defaultValue="false") Boolean standard,
        @ApiParam(value = "fetch direct children of parentId") @RequestParam(value = "parentId", required = false) Long parentId) {
        // do some magic!
        return delegate.getCriteriaBy(cdrVersionId, domain, type, standard, parentId);
    }

    public ResponseEntity<DemoChartInfoListResponse> getDemoChartInfo(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "object of parameters by which to perform the search" ,required=true )  @Valid @RequestBody SearchRequest request) {
        // do some magic!
        return delegate.getDemoChartInfo(cdrVersionId, request);
    }

    public ResponseEntity<CriteriaListResponse> getDrugBrandOrIngredientByValue(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
         @NotNull@ApiParam(value = "matches name or code of drug", required = true) @RequestParam(value = "value", required = true) String value,
        @ApiParam(value = "number of criteria matches to return") @RequestParam(value = "limit", required = false) Integer limit) {
        // do some magic!
        return delegate.getDrugBrandOrIngredientByValue(cdrVersionId, value, limit);
    }

    public ResponseEntity<CriteriaListResponse> getDrugIngredientByConceptId(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "conceptId of brand",required=true ) @PathVariable("conceptId") Long conceptId) {
        // do some magic!
        return delegate.getDrugIngredientByConceptId(cdrVersionId, conceptId);
    }

    public ResponseEntity<ParticipantDemographics> getParticipantDemographics(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId) {
        // do some magic!
        return delegate.getParticipantDemographics(cdrVersionId);
    }

    public ResponseEntity<CriteriaListResponse> getStandardCriteriaByDomainAndConceptId(@ApiParam(value = "specifies which cdr version",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "the specific type of domain",required=true ) @PathVariable("domain") String domain,
        @ApiParam(value = "the concept id to search for",required=true ) @PathVariable("conceptId") Long conceptId) {
        // do some magic!
        return delegate.getStandardCriteriaByDomainAndConceptId(cdrVersionId, domain, conceptId);
    }

}
