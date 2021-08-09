package org.pmiops.workbench.api;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCountListResponse;
import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenuListResponse;
import org.pmiops.workbench.model.CriteriaRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFiltersResponse;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCardResponse;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SurveyCount;
import org.pmiops.workbench.model.SurveyVersionListResponse;
import org.pmiops.workbench.model.SurveysResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

  private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());
  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  private final Provider<WorkbenchConfig> configProvider;
  private final CohortBuilderService cohortBuilderService;
  private final WorkspaceAuthService workspaceAuthService;

  @Autowired
  CohortBuilderController(
      Provider<WorkbenchConfig> configProvider,
      CohortBuilderService cohortBuilderService,
      WorkspaceAuthService workspaceAuthService) {
    this.configProvider = configProvider;
    this.cohortBuilderService = cohortBuilderService;
    this.workspaceAuthService = workspaceAuthService;
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaAutoComplete(
      String workspaceNamespace,
      String workspaceId,
      String domain,
      String term,
      String type,
      Boolean standard,
      Integer limit) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
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
      String workspaceNamespace, String workspaceId, String value, Integer limit) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findDrugBrandOrIngredientByValue(value, limit)));
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
   * SearchRequest}.
   */
  @Override
  public ResponseEntity<Long> countParticipants(
      String workspaceNamespace, String workspaceId, SearchRequest request) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(cohortBuilderService.countParticipants(request));
  }

  @Override
  public ResponseEntity<CriteriaListWithCountResponse> findCriteriaByDomain(
      String workspaceNamespace,
      String workspaceId,
      String domain,
      Boolean standard,
      String term,
      String surveyName,
      Integer limit) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(domain, surveyName);
    return ResponseEntity.ok(
        cohortBuilderService.findCriteriaByDomain(domain, term, surveyName, standard, limit));
  }

  @Override
  public ResponseEntity<CriteriaListWithCountResponse> findCriteriaByDomainAndSearchTerm(
      String workspaceNamespace,
      String workspaceId,
      String domain,
      String term,
      String surveyName,
      Integer limit) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(domain, surveyName);
    return ResponseEntity.ok(
        cohortBuilderService.findCriteriaByDomainAndSearchTerm(domain, term, surveyName, limit));
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
  public ResponseEntity<CriteriaMenuListResponse> findCriteriaMenuOld(
      String workspaceNamespace, String workspaceId, Long parentId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CriteriaMenuListResponse response =
        new CriteriaMenuListResponse()
            .items(cohortBuilderService.findCriteriaMenuByParentId_old(parentId));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CriteriaMenuListResponse> findCriteriaMenu(
      String workspaceNamespace, String workspaceId, Long parentId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CriteriaMenuListResponse response =
        new CriteriaMenuListResponse()
            .items(cohortBuilderService.findCriteriaMenuByParentId(parentId));
    return ResponseEntity.ok(response);
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
      String genderOrSex,
      String age,
      SearchRequest request) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    GenderOrSexType genderOrSexType = validateGenderOrSexType(genderOrSex);
    AgeType ageType = validateAgeType(age);
    DemoChartInfoListResponse response = new DemoChartInfoListResponse();
    if (request.getIncludes().isEmpty()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.ok(
        response.items(cohortBuilderService.findDemoChartInfo(genderOrSexType, ageType, request)));
  }

  @Override
  public ResponseEntity<DomainCount> findDomainCount(
      String workspaceNamespace, String workspaceId, String domain, String term) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    validateDomain(domain);
    validateTerm(term);
    Long count = cohortBuilderService.findDomainCount(domain, term);
    return ResponseEntity.ok(
        new DomainCount().conceptCount(count).domain(Domain.valueOf(domain)).name(domain));
  }

  @Override
  public ResponseEntity<DomainInfoResponse> findDomainInfos(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new DomainInfoResponse().items(cohortBuilderService.findDomainInfos()));
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
  public ResponseEntity<SurveyCount> findSurveyCount(
      String workspaceNamespace, String workspaceId, String name, String term) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    Long surveyCount = cohortBuilderService.findSurveyCount(name, term);
    return ResponseEntity.ok(
        new SurveyCount().conceptCount(surveyCount == null ? 0 : surveyCount).name(name));
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
      String workspaceNamespace, String workspaceId, Long surveyConceptId, Long questionConceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new SurveyVersionListResponse()
            .items(
                cohortBuilderService.findSurveyVersionByQuestionConceptId(
                    surveyConceptId, questionConceptId)));
  }

  @Override
  public ResponseEntity<SurveyVersionListResponse>
      findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
          String workspaceNamespace,
          String workspaceId,
          Long surveyConceptId,
          Long questionConceptId,
          Long answerConceptId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new SurveyVersionListResponse()
            .items(
                cohortBuilderService.findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
                    surveyConceptId, questionConceptId, answerConceptId)));
  }

  private void validateDomain(String domain) {
    Arrays.stream(Domain.values())
        .filter(domainType -> domainType.toString().equalsIgnoreCase(domain))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)));
  }

  private void validateDomain(String domain, String surveyName) {
    Arrays.stream(Domain.values())
        .filter(domainType -> domainType.toString().equalsIgnoreCase(domain))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)));
    if (Domain.SURVEY.equals(Domain.fromValue(domain))) {
      Optional.ofNullable(surveyName)
          .orElseThrow(
              () ->
                  new BadRequestException(
                      String.format(BAD_REQUEST_MESSAGE, "surveyName", surveyName)));
    }
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
