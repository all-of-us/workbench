package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuesResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.SurveyAnswerResponse;
import org.pmiops.workbench.model.SurveyQuestionsResponse;
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

  private final BigQueryService bigQueryService;
  private final ConceptService conceptService;
  private final ConceptBigQueryService conceptBigQueryService;
  private final WorkspaceService workspaceService;
  private final DomainInfoDao domainInfoDao;
  private final ConceptDao conceptDao;
  private final SurveyModuleDao surveyModuleDao;

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
      BigQueryService bigQueryService,
      ConceptService conceptService,
      ConceptBigQueryService conceptBigQueryService,
      WorkspaceService workspaceService,
      DomainInfoDao domainInfoDao,
      ConceptDao conceptDao,
      SurveyModuleDao surveyModuleDao) {
    this.bigQueryService = bigQueryService;
    this.conceptService = conceptService;
    this.conceptBigQueryService = conceptBigQueryService;
    this.workspaceService = workspaceService;
    this.domainInfoDao = domainInfoDao;
    this.conceptDao = conceptDao;
    this.surveyModuleDao = surveyModuleDao;
  }

  @Override
  public ResponseEntity<DomainInfoResponse> getDomainInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<DbDomainInfo> domains = ImmutableList.copyOf(domainInfoDao.findByOrderByDomainId());
    DomainInfoResponse response =
        new DomainInfoResponse()
            .items(
                domains.stream()
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

  public ResponseEntity<List<SurveyQuestionsResponse>> getSurveyQuestions(
      String workspaceNamespace, String workspaceId, String surveyName) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<SurveyQuestionsResponse> surveyQuestionAnswerList =
        conceptBigQueryService.getSurveyQuestions(surveyName);
    return ResponseEntity.ok(surveyQuestionAnswerList);
  }

  @Override
  public ResponseEntity<SurveysResponse> getSurveyInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<DbSurveyModule> surveyModules =
        surveyModuleDao.findByParticipantCountNotOrderByOrderNumberAsc(0L);

    SurveysResponse response =
        new SurveysResponse()
            .items(
                surveyModules.stream()
                    .map(DbSurveyModule.TO_CLIENT_SURVEY_MODULE)
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  private void addDomainCounts(SearchConceptsRequest request, ConceptListResponse response) {
    if (request.getIncludeDomainCounts()) {
      StandardConceptFilter standardConceptFilter = request.getStandardConceptFilter();
      String matchExp =
          ConceptService.modifyMultipleMatchKeyword(
              request.getQuery(), ConceptService.SearchType.CONCEPT_SEARCH);
      List<DbDomainInfo> allDomainInfos = domainInfoDao.findByOrderByDomainId();
      List<DbDomainInfo> domainInfos = matchExp == null ? allDomainInfos : new ArrayList<>();
      if (matchExp != null) {
        domainInfos =
            standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS
                ? domainInfoDao.findAllMatchConceptCounts(matchExp)
                : domainInfoDao.findStandardConceptCounts(matchExp);
      }
      Map<Domain, DbDomainInfo> domainCountMap =
          Maps.uniqueIndex(domainInfos, DbDomainInfo::getDomainEnum);
      // Loop through all domains to populate the results (so we get zeros for domains with no
      // matches.)
      for (DbDomainInfo domainInfo : allDomainInfos) {
        Domain domain = domainInfo.getDomainEnum();
        DbDomainInfo resultInfo = domainCountMap.get(domain);
        response.addDomainCountsItem(
            new DomainCount()
                .domain(domain)
                .conceptCount(
                    resultInfo == null
                        ? 0L
                        : (standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS
                            ? resultInfo.getAllConceptCount()
                            : resultInfo.getStandardConceptCount()))
                .name(domainInfo.getName()));
      }
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

    Slice<DbConcept> concepts =
        conceptService.searchConcepts(
            request.getQuery(),
            request.getStandardConceptFilter().name(),
            ImmutableList.of(CommonStorageEnums.domainToDomainId(request.getDomain())),
            maxResults,
            (request.getPageNumber() == null) ? 0 : request.getPageNumber());

    // TODO: consider doing these queries in parallel
    ConceptListResponse response = new ConceptListResponse();
    addDomainCounts(request, response);
    if (concepts != null) {
      response.setItems(
          concepts.getContent().stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<DomainValuesResponse> getValuesFromDomain(
      String workspaceNamespace, String workspaceId, String domainValue) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DomainValuesResponse response = new DomainValuesResponse();

    Domain domain = Domain.valueOf(domainValue);
    FieldList fieldList = bigQueryService.getTableFieldsFromDomain(domain);
    response.setItems(
        fieldList.stream()
            .map(field -> new DomainValue().value(field.getName()))
            .collect(Collectors.toList()));

    return ResponseEntity.ok(response);
  }
}
