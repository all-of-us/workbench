package org.pmiops.workbench.api;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.chart.ChartService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCountListResponse;
import org.pmiops.workbench.model.CardCountResponse;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.ConceptsRequest;
import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenuListResponse;
import org.pmiops.workbench.model.CriteriaRequest;
import org.pmiops.workbench.model.CriteriaSearchRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFiltersResponse;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCardResponse;
import org.pmiops.workbench.model.EthnicityInfoListResponse;
import org.pmiops.workbench.model.GenderSexRaceOrEthType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SurveyVersionListResponse;
import org.pmiops.workbench.model.SurveysResponse;
import org.pmiops.workbench.model.Variant;
import org.pmiops.workbench.model.VariantFilterRequest;
import org.pmiops.workbench.model.VariantFiltersResponse;
import org.pmiops.workbench.model.VariantListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

  private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());
  public static final Integer DEFAULT_COHORT_CHART_DATA_LIMIT = 10;
  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  private final CohortBuilderService cohortBuilderService;
  private final ChartService chartService;
  private final WorkspaceAuthService workspaceAuthService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  CohortBuilderController(
      CohortBuilderService cohortBuilderService,
      ChartService chartService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceAuthService workspaceAuthService) {
    this.cohortBuilderService = cohortBuilderService;
    this.chartService = chartService;
    this.workspaceAuthService = workspaceAuthService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaAutoComplete(
      String workspaceNamespace, String workspaceId, CriteriaSearchRequest criteriaSearchRequest) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(criteriaSearchRequest.getDomain());
    validateTerm(criteriaSearchRequest.getTerm());
    if (Domain.SURVEY.equals(Domain.fromValue(criteriaSearchRequest.getDomain()))) {
      validateSurveyName(criteriaSearchRequest.getSurveyName());
    } else {
      validateType(criteriaSearchRequest.getType());
    }
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findCriteriaAutoComplete(criteriaSearchRequest)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findDrugBrandOrIngredientByValue(
      String workspaceNamespace, String workspaceId, String value) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findDrugBrandOrIngredientByValue(value, null)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findDrugIngredientByConceptId(
      String workspaceNamespace, String workspaceId, Long conceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findDrugIngredientByConceptId(conceptId)));
  }

  @Override
  public ResponseEntity<AgeTypeCountListResponse> findAgeTypeCounts(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new AgeTypeCountListResponse().items(cohortBuilderService.findAgeTypeCounts()));
  }

  /**
   * This method will return a count of unique subjects defined by the provided {@link
   * CohortDefinition}.
   */
  @Override
  public ResponseEntity<Long> countParticipants(
      String workspaceNamespace, String workspaceId, CohortDefinition cohortDefinition) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    try {
      return ResponseEntity.ok(cohortBuilderService.countParticipants(cohortDefinition));
    } catch (Exception exception) {
      log.severe("cohortDefinition:\n" + new Gson().toJson(cohortDefinition));
      throw exception;
    }
  }

  @Override
  public ResponseEntity<CriteriaListWithCountResponse> findCriteriaByDomain(
      String workspaceNamespace, String workspaceId, CriteriaSearchRequest request) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(request.getDomain(), request.getSurveyName());
    return ResponseEntity.ok(cohortBuilderService.findCriteriaByDomain(request));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaForCohortEdit(
      String workspaceNamespace, String workspaceId, String domain, CriteriaRequest request) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(domain);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findCriteriaByDomainIdAndConceptIds(
                    domain, request.getSourceConceptIds(), request.getStandardConceptIds())));
  }

  @Override
  public ResponseEntity<CriteriaMenuListResponse> findCriteriaMenu(
      String workspaceNamespace, String workspaceId, Long parentId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    if (workbenchConfigProvider.get().featureFlags.enableHasEhrData) {
      return ResponseEntity.ok(
          new CriteriaMenuListResponse()
              .items(cohortBuilderService.findCriteriaMenuByParentId(parentId)));
    } else {
      return ResponseEntity.ok(
          new CriteriaMenuListResponse()
              .items(
                  cohortBuilderService.findCriteriaMenuByParentId(parentId).stream()
                      .filter(item -> !CriteriaType.HAS_EHR_DATA.toString().equals(item.getType()))
                      .collect(Collectors.toList())));
    }
  }

  @Override
  public ResponseEntity<DataFiltersResponse> findDataFilters(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new DataFiltersResponse().items(cohortBuilderService.findDataFilters()));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findStandardCriteriaByDomainAndConceptId(
      String workspaceNamespace, String workspaceId, String domain, Long conceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(domain);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findStandardCriteriaByDomainAndConceptId(domain, conceptId)));
  }

  @Override
  public ResponseEntity<DemoChartInfoListResponse> findDemoChartInfo(
      String workspaceNamespace,
      String workspaceId,
      String genderSexRaceOrEth,
      String age,
      CohortDefinition cohortDefinition) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    GenderSexRaceOrEthType genderSexRaceOrEthType =
        validateGenderSexRaceOrEthType(genderSexRaceOrEth);
    AgeType ageType = validateAgeType(age);
    DemoChartInfoListResponse response = new DemoChartInfoListResponse();
    if (cohortDefinition.getIncludes().isEmpty()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.ok(
        response.items(
            chartService.findDemoChartInfo(genderSexRaceOrEthType, ageType, cohortDefinition)));
  }

  @Override
  public ResponseEntity<EthnicityInfoListResponse> findEthnicityInfo(
      String workspaceNamespace, String workspaceId, CohortDefinition cohortDefinition) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    EthnicityInfoListResponse response = new EthnicityInfoListResponse();
    if (cohortDefinition.getIncludes().isEmpty()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.ok(response.items(chartService.findEthnicityInfo(cohortDefinition)));
  }

  @Override
  public ResponseEntity<CardCountResponse> findUniversalDomainCounts(
      String workspaceNamespace, String workspaceId, String term) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateTerm(term);

    return ResponseEntity.ok(
        new CardCountResponse().items(cohortBuilderService.findUniversalDomainCounts(term)));
  }

  @Override
  public ResponseEntity<VariantFiltersResponse> findVariantFilters(
      String workspaceNamespace, String workspaceId, VariantFilterRequest request) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateTerm(request.getSearchTerm());

    return ResponseEntity.ok(cohortBuilderService.findVariantFilters(request));
  }

  @Override
  public ResponseEntity<VariantListResponse> findVariants(
      String workspaceNamespace, String workspaceId, VariantFilterRequest request) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateTerm(request.getSearchTerm());

    // this method returns a paginated list of variants
    // ImmutableTriple contains nextPageToken, total results count
    // and list of variants
    ImmutableTriple<String, Integer, List<Variant>> searchResults =
        cohortBuilderService.findVariants(request);

    return ResponseEntity.ok(
        new VariantListResponse()
            .nextPageToken(searchResults.getLeft())
            .totalSize(searchResults.getMiddle())
            .items(searchResults.getRight()));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findVersionedSurveys(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new CriteriaListResponse().items(cohortBuilderService.findVersionedSurveys()));
  }

  @Override
  public ResponseEntity<CardCountResponse> findConceptCounts(
      String workspaceNamespace, String workspaceId, String term) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateTerm(term);
    return ResponseEntity.ok(
        new CardCountResponse().items(cohortBuilderService.findDomainCounts(term)));
  }

  @Override
  public ResponseEntity<DomainCardResponse> findDomainCards(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new DomainCardResponse().items(cohortBuilderService.findDomainCards()));
  }

  @Override
  public ResponseEntity<CriteriaAttributeListResponse> findCriteriaAttributeByConceptId(
      String workspaceNamespace, String workspaceId, Long conceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new CriteriaAttributeListResponse()
            .items(cohortBuilderService.findCriteriaAttributeByConceptId(conceptId)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaBy(
      String workspaceNamespace,
      String workspaceId,
      String domain,
      String type,
      Boolean standard,
      Long parentId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(domain);
    validateType(type);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findCriteriaBy(domain, type, standard, parentId)));
  }

  @Override
  public ResponseEntity<ParticipantDemographics> findParticipantDemographics(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(cohortBuilderService.findParticipantDemographics());
  }

  @Override
  public ResponseEntity<SurveysResponse> findSurveyModules(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(new SurveysResponse().items(cohortBuilderService.findSurveyModules()));
  }

  @Override
  public ResponseEntity<SurveyVersionListResponse> findSurveyVersionByQuestionConceptId(
      String workspaceNamespace, String workspaceId, Long questionConceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new SurveyVersionListResponse()
            .items(cohortBuilderService.findSurveyVersionByQuestionConceptId(questionConceptId)));
  }

  @Override
  public ResponseEntity<SurveyVersionListResponse>
      findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
          String workspaceNamespace,
          String workspaceId,
          Long questionConceptId,
          Long answerConceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new SurveyVersionListResponse()
            .items(
                cohortBuilderService.findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
                    questionConceptId, answerConceptId)));
  }

  @Override
  public ResponseEntity<CohortChartDataListResponse> getCohortChartData(
      String workspaceNamespace,
      String workspaceId,
      String domain,
      CohortDefinition cohortDefinition) {

    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    long count = cohortBuilderService.countParticipants(cohortDefinition);

    return ResponseEntity.ok(
        new CohortChartDataListResponse()
            .count(count)
            .items(
                chartService.findCohortChartData(
                    cohortDefinition,
                    Objects.requireNonNull(Domain.fromValue(domain)),
                    DEFAULT_COHORT_CHART_DATA_LIMIT)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaByConceptIdsOrConceptCodes(
      String workspaceNamespace, String workspaceId, ConceptsRequest request) {

    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findCriteriaByConceptIdsOrConceptCodes(
                    request.getConceptKeys())));
  }

  protected void validateDomain(String domain) {
    Arrays.stream(Domain.values())
        .filter(domainType -> domainType.toString().equalsIgnoreCase(domain))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)));
  }

  protected void validateDomain(String domain, String surveyName) {
    validateDomain(domain);
    if (Domain.SURVEY.equals(Domain.fromValue(domain))) {
      Optional.ofNullable(surveyName)
          .orElseThrow(
              () ->
                  new BadRequestException(
                      String.format(BAD_REQUEST_MESSAGE, "surveyName", surveyName)));
    }
  }

  protected void validateSurveyName(String surveyName) {
    Optional.ofNullable(surveyName)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(BAD_REQUEST_MESSAGE, "surveyName", surveyName)));
  }

  protected void validateType(String type) {
    Arrays.stream(CriteriaType.values())
        .filter(critType -> critType.toString().equalsIgnoreCase(type))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "type", type)));
  }

  protected void validateTerm(String term) {
    if (term == null || term.trim().isEmpty()) {
      throw new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "search term", term));
    }
    // term has double-quoted phrase ["my word"] and more than 1 [*word]
    // term has one word AND is [-word]
    // term has 2 words AND has [*word and -word]
    // term has 2 or more words AND has [*word and *word2]
  }

  protected AgeType validateAgeType(String age) {
    return Optional.ofNullable(age)
        .map(AgeType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(BAD_REQUEST_MESSAGE, "age type parameter", age)));
  }

  protected GenderSexRaceOrEthType validateGenderSexRaceOrEthType(String genderSexRaceOrEth) {
    return Optional.ofNullable(genderSexRaceOrEth)
        .map(GenderSexRaceOrEthType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(
                        BAD_REQUEST_MESSAGE,
                        "gender, sex at birth, race or ethnicity parameter",
                        genderSexRaceOrEth)));
  }
}
