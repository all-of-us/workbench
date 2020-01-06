package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.SurveyAnswerResponse;
import org.pmiops.workbench.model.SurveyQuestions;
import org.pmiops.workbench.model.SurveysResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptsController implements ConceptsApiDelegate {

  private static final Integer DEFAULT_MAX_RESULTS = 20;
  private static final int MAX_MAX_RESULTS = 1000;

  private final ConceptService conceptService;
  private final ConceptBigQueryService conceptBigQueryService;
  private final WorkspaceService workspaceService;

  static final Function<DbConcept, Concept> TO_CLIENT_CONCEPT =
      (concept) ->
          new Concept()
              .conceptClassId(concept.getConceptClassId())
              .conceptCode(concept.getConceptCode())
              .conceptName(concept.getConceptName())
              .conceptId(concept.getConceptId())
              .countValue(concept.getCountValue())
              .domainId(concept.getDomainId())
              .prevalence(concept.getPrevalence())
              .standardConcept(
                  ConceptService.STANDARD_CONCEPT_CODE.equals(concept.getStandardConcept()))
              .vocabularyId(concept.getVocabularyId())
              .conceptSynonyms(concept.getSynonyms());

  @Autowired
  public ConceptsController(
      ConceptService conceptService,
      ConceptBigQueryService conceptBigQueryService,
      WorkspaceService workspaceService) {
    this.conceptService = conceptService;
    this.conceptBigQueryService = conceptBigQueryService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<DomainInfoResponse> getDomainInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DomainInfoResponse response =
        new DomainInfoResponse()
            .items(
                conceptService.getDomainInfo().stream()
                    .map(DbDomainInfo.TO_CLIENT_DOMAIN_INFO)
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<SurveyAnswerResponse>> getSurveyAnswers(
      String workspaceNamespace, String workspaceId, Long questionConceptId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<SurveyAnswerResponse> answer = conceptBigQueryService.getSurveyAnswer(questionConceptId);
    return ResponseEntity.ok(answer);
  }

  public ResponseEntity<List<SurveyQuestions>> getSurveyQuestions(
      String workspaceNamespace, String workspaceId, String surveyName) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<SurveyQuestions> surveyQuestionAnswerList =
        conceptBigQueryService.getSurveyQuestions(surveyName);
    return ResponseEntity.ok(surveyQuestionAnswerList);
  }

  @Override
  public ResponseEntity<SurveysResponse> getSurveyInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    SurveysResponse response =
        new SurveysResponse()
            .items(
                conceptService.getSurveyInfo().stream()
                    .map(DbSurveyModule.TO_CLIENT_SURVEY_MODULE)
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  private void addDomainCounts(SearchConceptsRequest request, ConceptListResponse response) {
    if (request.getIncludeDomainCounts()) {
      StandardConceptFilter standardConceptFilter = request.getStandardConceptFilter();
      boolean allConcepts = standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS;
      DbDomainInfo pmDomainInfo = null;
      String matchExp = ConceptService.modifyMultipleMatchKeyword(request.getQuery());
      List<DbDomainInfo> allDbDomainInfos = conceptService.getAllDomainsOrderByDomainId();
      List<DbDomainInfo> matchingDbDomainInfos =
          matchExp == null ? allDbDomainInfos : new ArrayList<>();
      if (matchingDbDomainInfos.isEmpty()) {
        matchingDbDomainInfos =
            allConcepts
                ? conceptService.getAllConceptCounts(matchExp)
                : conceptService.getStandardConceptCounts(matchExp);
        if (allConcepts) {
          pmDomainInfo = conceptService.findPhysicalMeasurementConceptCounts(matchExp);
        }
      }
      Map<Domain, DbDomainInfo> domainCountMap =
          Maps.uniqueIndex(matchingDbDomainInfos, DbDomainInfo::getDomainEnum);
      // Loop through all domains to populate the results (so we get zeros for domains with no
      // matches.)
      for (DbDomainInfo allDbDomainInfo : allDbDomainInfos) {
        Domain domain = allDbDomainInfo.getDomainEnum();
        DbDomainInfo matchingDbDomainInfo = domainCountMap.get(domain);
        if (domain.equals(Domain.PHYSICALMEASUREMENT) && pmDomainInfo != null) {
          matchingDbDomainInfo = pmDomainInfo;
        }
        response.addDomainCountsItem(
            new DomainCount()
                .domain(domain)
                .conceptCount(
                    matchingDbDomainInfo == null
                        ? 0L
                        : (allConcepts
                            ? matchingDbDomainInfo.getAllConceptCount()
                            : matchingDbDomainInfo.getStandardConceptCount()))
                .name(allDbDomainInfo.getName()));
      }
      long conceptCount = 0;
      if (allConcepts) {
        conceptCount =
            matchExp == null
                ? conceptService.findSurveyCountBySurveyName(request.getSurveyName())
                : conceptService.findSurveyCountByTerm(matchExp);
      }
      response.addDomainCountsItem(
          new DomainCount()
              .domain(Domain.SURVEY)
              .conceptCount(conceptCount)
              .name(StringUtils.capitalize(Domain.SURVEY.toString().toLowerCase()).concat("s")));
    }
  }

  @Override
  public ResponseEntity<ConceptListResponse> searchConcepts(
      String workspaceNamespace, String workspaceId, SearchConceptsRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    Integer maxResults = Optional.ofNullable(request.getMaxResults()).orElse(DEFAULT_MAX_RESULTS);
    maxResults = maxResults > MAX_MAX_RESULTS ? MAX_MAX_RESULTS : maxResults;
    if (maxResults < 1) {
      throw new BadRequestException("Invalid value for maxResults: " + maxResults);
    }

    ConceptListResponse response = new ConceptListResponse();
    if (request.getDomain().equals(Domain.SURVEY)) {
      List<SurveyQuestions> surveyQuestionList =
          conceptBigQueryService.getSurveyQuestions(request.getSurveyName());
      response.setQuestions(surveyQuestionList);
    } else {
      Slice<DbConcept> concepts =
          conceptService.searchConcepts(
              request.getQuery(),
              request.getStandardConceptFilter().name(),
              ImmutableList.of(CommonStorageEnums.domainToDomainId(request.getDomain())),
              maxResults,
              (request.getPageNumber() == null) ? 0 : request.getPageNumber());
      if (concepts != null) {
        response.setItems(
            concepts.getContent().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
      }
    }

    // TODO: consider doing these queries in parallel
    addDomainCounts(request, response);
    return ResponseEntity.ok(response);
  }
}
