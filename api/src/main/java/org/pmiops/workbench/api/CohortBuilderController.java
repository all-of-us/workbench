package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCountListResponse;
import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaMenuOptionsListResponse;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFiltersResponse;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

  private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());
  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  private CdrVersionService cdrVersionService;
  private ElasticSearchService elasticSearchService;
  private Provider<WorkbenchConfig> configProvider;
  private CohortBuilderService cohortBuilderService;

  @Autowired
  CohortBuilderController(
      CdrVersionService cdrVersionService,
      ElasticSearchService elasticSearchService,
      Provider<WorkbenchConfig> configProvider,
      CohortBuilderService cohortBuilderService) {
    this.cdrVersionService = cdrVersionService;
    this.elasticSearchService = elasticSearchService;
    this.configProvider = configProvider;
    this.cohortBuilderService = cohortBuilderService;
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaAutoComplete(
      Long cdrVersionId, String domain, String term, String type, Boolean standard, Integer limit) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    validateDomain(domain);
    validateType(type);
    validateTerm(term);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findCriteriaAutoComplete(
                    domain, term, type, standard, limit)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findDrugBrandOrIngredientByValue(
      Long cdrVersionId, String value, Integer limit) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findDrugBrandOrIngredientByValue(value, limit)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findDrugIngredientByConceptId(
      Long cdrVersionId, Long conceptId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findDrugIngredientByConceptId(conceptId)));
  }

  @Override
  public ResponseEntity<AgeTypeCountListResponse> findAgeTypeCounts(Long cdrVersionId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    return ResponseEntity.ok(
        new AgeTypeCountListResponse().items(cohortBuilderService.findAgeTypeCounts()));
  }

  /**
   * This method will return a count of unique subjects defined by the provided {@link
   * SearchRequest}.
   */
  @Override
  public ResponseEntity<Long> countParticipants(Long cdrVersionId, SearchRequest request) {
    DbCdrVersion cdrVersion = cdrVersionService.findAndSetCdrVersion(cdrVersionId);
    if (configProvider.get().elasticsearch.enableElasticsearchBackend
        && !Strings.isNullOrEmpty(cdrVersion.getElasticIndexBaseName())
        && !isApproximate(request)) {
      try {
        return ResponseEntity.ok(elasticSearchService.count(request));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e);
      }
    }
    return ResponseEntity.ok(cohortBuilderService.countParticipants(request));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaByDomainAndSearchTerm(
      Long cdrVersionId, String domain, String term, Integer limit) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    validateDomain(domain);
    validateTerm(term);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findCriteriaByDomainAndSearchTerm(domain, term, limit)));
  }

  @Override
  public ResponseEntity<CriteriaMenuOptionsListResponse> findCriteriaMenuOptions(
      Long cdrVersionId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    CriteriaMenuOptionsListResponse response =
        new CriteriaMenuOptionsListResponse().items(cohortBuilderService.findCriteriaMenuOptions());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<DataFiltersResponse> findDataFilters(Long cdrVersionId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    return ResponseEntity.ok(
        new DataFiltersResponse().items(cohortBuilderService.findDataFilters()));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findStandardCriteriaByDomainAndConceptId(
      Long cdrVersionId, String domain, Long conceptId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    validateDomain(domain);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findStandardCriteriaByDomainAndConceptId(domain, conceptId)));
  }

  @Override
  public ResponseEntity<DemoChartInfoListResponse> findDemoChartInfo(
      Long cdrVersionId, String genderOrSex, String age, SearchRequest request) {
    DbCdrVersion cdrVersion = cdrVersionService.findAndSetCdrVersion(cdrVersionId);
    GenderOrSexType genderOrSexType = validateGenderOrSexType(genderOrSex);
    AgeType ageType = validateAgeType(age);
    DemoChartInfoListResponse response = new DemoChartInfoListResponse();
    if (request.getIncludes().isEmpty()) {
      return ResponseEntity.ok(response);
    }
    if (configProvider.get().elasticsearch.enableElasticsearchBackend
        && !Strings.isNullOrEmpty(cdrVersion.getElasticIndexBaseName())
        && !isApproximate(request)) {
      try {
        return ResponseEntity.ok(
            response.items(
                elasticSearchService.demoChartInfo(
                    new ParticipantCriteria(request, genderOrSexType, ageType))));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e);
      }
    }
    return ResponseEntity.ok(
        response.items(cohortBuilderService.findDemoChartInfo(genderOrSexType, ageType, request)));
  }

  @Override
  public ResponseEntity<CriteriaAttributeListResponse> findCriteriaAttributeByConceptId(
      Long cdrVersionId, Long conceptId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    return ResponseEntity.ok(
        new CriteriaAttributeListResponse()
            .items(cohortBuilderService.findCriteriaAttributeByConceptId(conceptId)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaBy(
      Long cdrVersionId, String domain, String type, Boolean standard, Long parentId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    validateDomain(domain);
    validateType(type);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findCriteriaBy(domain, type, standard, parentId)));
  }

  @Override
  public ResponseEntity<ParticipantDemographics> findParticipantDemographics(Long cdrVersionId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    return ResponseEntity.ok(cohortBuilderService.findParticipantDemographics());
  }

  /**
   * This method helps determine what request can only be approximated by elasticsearch and must
   * fallback to the BQ implementation.
   */
  protected boolean isApproximate(SearchRequest request) {
    List<SearchGroup> allGroups =
        ImmutableList.copyOf(Iterables.concat(request.getIncludes(), request.getExcludes()));
    List<SearchParameter> allParams =
        allGroups.stream()
            .flatMap(sg -> sg.getItems().stream())
            .flatMap(sgi -> sgi.getSearchParameters().stream())
            .collect(Collectors.toList());
    return allGroups.stream().anyMatch(sg -> sg.getTemporal())
        || allParams.stream().anyMatch(sp -> CriteriaSubType.BP.toString().equals(sp.getSubtype()));
  }

  private void validateDomain(String domain) {
    Arrays.stream(DomainType.values())
        .filter(domainType -> domainType.toString().equalsIgnoreCase(domain))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)));
  }

  private void validateType(String type) {
    Arrays.stream(CriteriaType.values())
        .filter(critType -> critType.toString().equalsIgnoreCase(type))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "type", type)));
  }

  private void validateTerm(String term) {
    if (term == null || term.trim().isEmpty()) {
      throw new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "search term", term));
    }
  }

  private AgeType validateAgeType(String age) {
    return Optional.ofNullable(age)
        .map(AgeType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(BAD_REQUEST_MESSAGE, "age type parameter", age)));
  }

  private GenderOrSexType validateGenderOrSexType(String genderOrSex) {
    return Optional.ofNullable(genderOrSex)
        .map(GenderOrSexType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(
                        BAD_REQUEST_MESSAGE, "gender or sex at birth parameter", genderOrSex)));
  }
}
