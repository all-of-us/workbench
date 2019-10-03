package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.DomainVocabularyInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.pmiops.workbench.cdr.model.DomainVocabularyInfo;
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
import org.pmiops.workbench.model.VocabularyCount;
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
  private final DomainVocabularyInfoDao domainVocabularyInfoDao;
  private final ConceptDao conceptDao;
  private final SurveyModuleDao surveyModuleDao;

  public static final Function<org.pmiops.workbench.cdr.model.Concept, Concept> TO_CLIENT_CONCEPT =
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

  static final Function<DomainVocabularyInfo, VocabularyCount>
      TO_VOCABULARY_STANDARD_CONCEPT_COUNT =
          (domainVocabularyInfo) ->
              new VocabularyCount()
                  .conceptCount(domainVocabularyInfo.getStandardConceptCount())
                  .vocabularyId(domainVocabularyInfo.getId().getVocabularyId());

  static final Function<DomainVocabularyInfo, VocabularyCount> TO_VOCABULARY_ALL_CONCEPT_COUNT =
      (domainVocabularyInfo) ->
          new VocabularyCount()
              .conceptCount(domainVocabularyInfo.getAllConceptCount())
              .vocabularyId(domainVocabularyInfo.getId().getVocabularyId());

  private static final Function<org.pmiops.workbench.cdr.model.VocabularyCount, VocabularyCount>
      TO_CLIENT_VOCAB_COUNT =
          (vocabCount) ->
              new VocabularyCount()
                  .conceptCount(vocabCount.getConceptCount())
                  .vocabularyId(vocabCount.getVocabularyId());

  private static final Predicate<VocabularyCount> NOT_ZERO =
      (vocabCount) -> vocabCount.getConceptCount() > 0;

  @Autowired
  public ConceptsController(
      BigQueryService bigQueryService,
      ConceptService conceptService,
      ConceptBigQueryService conceptBigQueryService,
      WorkspaceService workspaceService,
      DomainInfoDao domainInfoDao,
      DomainVocabularyInfoDao domainVocabularyInfoDao,
      ConceptDao conceptDao,
      SurveyModuleDao surveyModuleDao) {
    this.bigQueryService = bigQueryService;
    this.conceptService = conceptService;
    this.conceptBigQueryService = conceptBigQueryService;
    this.workspaceService = workspaceService;
    this.domainInfoDao = domainInfoDao;
    this.domainVocabularyInfoDao = domainVocabularyInfoDao;
    this.conceptDao = conceptDao;
    this.surveyModuleDao = surveyModuleDao;
  }

  @Override
  public ResponseEntity<DomainInfoResponse> getDomainInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<org.pmiops.workbench.cdr.model.DomainInfo> domains =
        ImmutableList.copyOf(domainInfoDao.findByOrderByDomainId());
    DomainInfoResponse response =
        new DomainInfoResponse()
            .items(
                domains.stream()
                    .map(org.pmiops.workbench.cdr.model.DomainInfo.TO_CLIENT_DOMAIN_INFO)
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
    List<org.pmiops.workbench.cdr.model.SurveyModule> surveyModules =
        surveyModuleDao.findByOrderByOrderNumberAsc();

    SurveysResponse response =
        new SurveysResponse()
            .items(
                surveyModules.stream()
                    .map(org.pmiops.workbench.cdr.model.SurveyModule.TO_CLIENT_SURVEY_MODULE)
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  private void addDomainCounts(
      SearchConceptsRequest request,
      ConceptListResponse response,
      String matchExp,
      StandardConceptFilter standardConceptFilter) {
    if (request.getIncludeDomainCounts() == null || !request.getIncludeDomainCounts()) {
      return;
    }
    List<DomainInfo> allDomainInfos = domainInfoDao.findByOrderByDomainId();
    List<DomainInfo> domainInfos = null;
    if (matchExp == null) {
      domainInfos = allDomainInfos;
    } else {
      if (standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS) {
        domainInfos = domainInfoDao.findAllMatchConceptCounts(matchExp);
      } else if (standardConceptFilter == StandardConceptFilter.STANDARD_CONCEPTS) {
        domainInfos = domainInfoDao.findStandardConceptCounts(matchExp);
      } else {
        return;
      }
    }
    Map<Domain, DomainInfo> domainCountMap =
        Maps.uniqueIndex(domainInfos, DomainInfo::getDomainEnum);
    // Loop through all domains to populate the results (so we get zeros for domains with no
    // matches.)
    for (DomainInfo domainInfo : allDomainInfos) {
      Domain domain = domainInfo.getDomainEnum();
      DomainInfo resultInfo = domainCountMap.get(domain);
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

  private void addVocabularyCounts(
      SearchConceptsRequest request,
      ConceptListResponse response,
      String matchExp,
      StandardConceptFilter standardConceptFilter) {
    if (request.getDomain() == null
        || request.getIncludeVocabularyCounts() == null
        || !request.getIncludeVocabularyCounts()) {
      return;
    }
    String domainId = CommonStorageEnums.domainToDomainId(request.getDomain());
    List<VocabularyCount> vocabularyCounts;
    if (standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS) {
      if (matchExp == null) {
        vocabularyCounts =
            domainVocabularyInfoDao.findById_DomainIdOrderById_VocabularyId(domainId).stream()
                .map(TO_VOCABULARY_ALL_CONCEPT_COUNT)
                .filter(NOT_ZERO)
                .collect(Collectors.toList());
      } else {
        vocabularyCounts =
            conceptDao.findVocabularyAllConceptCounts(matchExp, domainId).stream()
                .map(TO_CLIENT_VOCAB_COUNT)
                .collect(Collectors.toList());
      }
    } else if (standardConceptFilter == StandardConceptFilter.STANDARD_CONCEPTS) {
      if (matchExp == null) {
        vocabularyCounts =
            domainVocabularyInfoDao.findById_DomainIdOrderById_VocabularyId(domainId).stream()
                .map(TO_VOCABULARY_STANDARD_CONCEPT_COUNT)
                .filter(NOT_ZERO)
                .collect(Collectors.toList());
      } else {
        vocabularyCounts =
            conceptDao.findVocabularyStandardConceptCounts(matchExp, domainId).stream()
                .map(TO_CLIENT_VOCAB_COUNT)
                .collect(Collectors.toList());
      }
    } else {
      return;
    }
    response.setVocabularyCounts(vocabularyCounts);
  }

  @Override
  public ResponseEntity<ConceptListResponse> searchConcepts(
      String workspaceNamespace, String workspaceId, SearchConceptsRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    Integer maxResults = request.getMaxResults();
    Integer minCount = request.getMinCount();
    if (maxResults == null) {
      maxResults = DEFAULT_MAX_RESULTS;
    } else if (maxResults < 1) {
      throw new BadRequestException("Invalid value for maxResults: " + maxResults);
    } else if (maxResults > MAX_MAX_RESULTS) {
      maxResults = MAX_MAX_RESULTS;
    }
    if (minCount == null) {
      minCount = 1;
    }
    if (request.getVocabularyIds() != null && request.getVocabularyIds().size() == 0) {
      throw new BadRequestException("No vocabulary options selected");
    }
    StandardConceptFilter standardConceptFilter = request.getStandardConceptFilter();
    if (standardConceptFilter == null) {
      standardConceptFilter = StandardConceptFilter.ALL_CONCEPTS;
    }

    String matchExp =
        ConceptService.modifyMultipleMatchKeyword(
            request.getQuery(), ConceptService.SearchType.CONCEPT_SEARCH);
    // TODO: consider doing these queries in parallel
    ConceptListResponse response = new ConceptListResponse();
    addDomainCounts(request, response, matchExp, standardConceptFilter);
    addVocabularyCounts(request, response, matchExp, standardConceptFilter);

    List<String> domainIds = null;
    if (request.getDomain() != null) {
      domainIds = ImmutableList.of(CommonStorageEnums.domainToDomainId(request.getDomain()));
    }
    ConceptService.StandardConceptFilter convertedConceptFilter =
        ConceptService.StandardConceptFilter.valueOf(standardConceptFilter.name());

    Slice<org.pmiops.workbench.cdr.model.Concept> concepts =
        conceptService.searchConcepts(
            request.getQuery(),
            convertedConceptFilter,
            request.getVocabularyIds(),
            domainIds,
            maxResults,
            minCount,
            (request.getPageNumber() == null) ? 0 : request.getPageNumber());

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
