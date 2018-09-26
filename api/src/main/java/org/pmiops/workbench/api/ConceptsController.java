package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Map;

import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.ConceptSynonymDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.model.ConceptSynonym;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.VocabularyCount;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class ConceptsController implements ConceptsApiDelegate {

  private static final Integer DEFAULT_MAX_RESULTS = 20;
  private static final int MAX_MAX_RESULTS = 1000;

  private final ConceptService conceptService;
  private final WorkspaceService workspaceService;
  private final DomainInfoDao domainInfoDao;
  private final ConceptDao conceptDao;

  static final Function<org.pmiops.workbench.cdr.model.Concept, Concept> TO_CLIENT_CONCEPT =
      (concept) ->  new Concept()
            .conceptClassId(concept.getConceptClassId())
            .conceptCode(concept.getConceptCode())
            .conceptName(concept.getConceptName())
            .conceptId(concept.getConceptId())
            .countValue(concept.getCountValue())
            .domainId(concept.getDomainId())
            .prevalence(concept.getPrevalence())
            .standardConcept(ConceptService.STANDARD_CONCEPT_CODE.equals(concept.getStandardConcept()))
            .vocabularyId(concept.getVocabularyId())
            .conceptSynonyms(concept.getSynonyms().stream().map(ConceptSynonym::getConceptSynonymName).collect(Collectors.toList()));

  private static final Function<org.pmiops.workbench.cdr.model.VocabularyCount, VocabularyCount>
      TO_CLIENT_VOCAB_COUNT =
      (vocabCount) -> new VocabularyCount()
          .conceptCount(vocabCount.getConceptCount())
          .vocabularyId(vocabCount.getVocabularyId());

  @Autowired
  ConceptSynonymDao conceptSynonymDao;

  @Autowired
  public ConceptsController(ConceptService conceptService, WorkspaceService workspaceService,
                            ConceptSynonymDao conceptSynonymDao, DomainInfoDao domainInfoDao,
                            ConceptDao conceptDao) {
    this.conceptService = conceptService;
    this.workspaceService = workspaceService;
    this.conceptSynonymDao = conceptSynonymDao;
    this.domainInfoDao = domainInfoDao;
    this.conceptDao = conceptDao;
  }

  @Override
  public ResponseEntity<DomainInfoResponse> getDomainInfo(String workspaceNamespace,
      String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<org.pmiops.workbench.cdr.model.DomainInfo> domains =
        ImmutableList.copyOf(domainInfoDao.findByOrderByDomainId());
    DomainInfoResponse response = new DomainInfoResponse().items(
        domains.stream().map(org.pmiops.workbench.cdr.model.DomainInfo.TO_CLIENT_DOMAIN_INFO)
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  private void addDomainCounts(SearchConceptsRequest request, ConceptListResponse response,
                               String matchExp, StandardConceptFilter standardConceptFilter) {
    if (request.getIncludeDomainCounts() != null && request.getIncludeDomainCounts()) {
      List<DomainInfo> allDomainInfos = domainInfoDao.findByOrderByDomainId();
      List<DomainInfo> domainInfos = null;
      if (matchExp == null) {
        domainInfos = allDomainInfos;
      } else {
        if (standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS) {
          domainInfos = domainInfoDao.findAllMatchConceptCounts(matchExp, request.getQuery());
        } else if (standardConceptFilter == StandardConceptFilter.STANDARD_CONCEPTS) {
          domainInfos = domainInfoDao.findStandardConceptCounts(matchExp, request.getQuery());
        } else {
          return;
        }
      }
      Map<Domain, DomainInfo> domainCountMap = Maps.uniqueIndex(domainInfos, DomainInfo::getDomainEnum);
      // Loop through all domains to populate the results (so we get zeros for domains with no
      // matches.)
      for (DomainInfo domainInfo : allDomainInfos) {
        Domain domain = domainInfo.getDomainEnum();
        DomainInfo resultInfo = domainCountMap.get(domain);
        response.addDomainCountsItem(new DomainCount().domain(domain).conceptCount(
            resultInfo == null ? 0L : resultInfo.getAllConceptCount()));
      }
    }
  }

  private void addVocabularyCounts(SearchConceptsRequest request, ConceptListResponse response,
                                   String matchExp, StandardConceptFilter standardConceptFilter) {
    if (request.getDomain() == null) {
      return;
    }
    String domainId = CommonStorageEnums.domainToDomainId(request.getDomain());
    List<org.pmiops.workbench.cdr.model.VocabularyCount> vocabularyCounts;
    Long queryId = null;
    try {
      queryId = Long.parseLong(request.getQuery());
    } catch (NumberFormatException e) {
    }
    if (standardConceptFilter == StandardConceptFilter.ALL_CONCEPTS) {
      vocabularyCounts = conceptDao.findVocabularyAllConceptCounts(matchExp, request.getQuery(),
          queryId, domainId);
    } else if (standardConceptFilter == StandardConceptFilter.STANDARD_CONCEPTS) {
      vocabularyCounts = conceptDao.findVocabularyStandardConceptCounts(matchExp, request.getQuery(),
          queryId, domainId);
    } else {
      return;
    }
    response.setVocabularyCounts(vocabularyCounts.stream().map(TO_CLIENT_VOCAB_COUNT)
        .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<ConceptListResponse> searchConcepts(String workspaceNamespace,
      String workspaceId, SearchConceptsRequest request) {
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
    if(minCount == null){
      minCount = 1;
    }
    StandardConceptFilter standardConceptFilter = request.getStandardConceptFilter();
    if (standardConceptFilter == null) {
      standardConceptFilter = StandardConceptFilter.ALL_CONCEPTS;
    }
    String matchExp = ConceptService.modifyMultipleMatchKeyword(request.getQuery());
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

    Slice<org.pmiops.workbench.cdr.model.Concept> concepts = conceptService.searchConcepts(
        request.getQuery(), convertedConceptFilter,
        request.getVocabularyIds(), domainIds, maxResults, minCount);

    if(concepts != null){
      response.setItems(concepts.getContent().stream().map(TO_CLIENT_CONCEPT)
              .collect(Collectors.toList()));
    }
    return ResponseEntity.ok(response);
  }
}
