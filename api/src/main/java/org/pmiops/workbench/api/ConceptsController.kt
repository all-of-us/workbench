package org.pmiops.workbench.api

import com.google.cloud.bigquery.FieldList
import com.google.common.collect.ImmutableList
import com.google.common.collect.Maps
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cdr.dao.DomainInfoDao
import org.pmiops.workbench.cdr.dao.DomainVocabularyInfoDao
import org.pmiops.workbench.cdr.dao.SurveyModuleDao
import org.pmiops.workbench.cdr.model.DomainInfo
import org.pmiops.workbench.cdr.model.DomainVocabularyInfo
import org.pmiops.workbench.db.model.CommonStorageEnums
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptListResponse
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DomainCount
import org.pmiops.workbench.model.DomainInfoResponse
import org.pmiops.workbench.model.DomainValue
import org.pmiops.workbench.model.DomainValuesResponse
import org.pmiops.workbench.model.SearchConceptsRequest
import org.pmiops.workbench.model.StandardConceptFilter
import org.pmiops.workbench.model.SurveyAnswerResponse
import org.pmiops.workbench.model.SurveyQuestionsResponse
import org.pmiops.workbench.model.SurveysResponse
import org.pmiops.workbench.model.VocabularyCount
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ConceptsController @Autowired
constructor(
        private val bigQueryService: BigQueryService,
        private val conceptService: ConceptService,
        private val conceptBigQueryService: ConceptBigQueryService,
        private val workspaceService: WorkspaceService,
        private val domainInfoDao: DomainInfoDao,
        private val domainVocabularyInfoDao: DomainVocabularyInfoDao,
        private val conceptDao: ConceptDao,
        private val surveyModuleDao: SurveyModuleDao) : ConceptsApiDelegate {

    fun getDomainInfo(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<DomainInfoResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val domains = ImmutableList.copyOf(domainInfoDao.findByOrderByDomainId())
        val response = DomainInfoResponse()
                .items(
                        domains.stream()
                                .map(org.pmiops.workbench.cdr.model.DomainInfo.TO_CLIENT_DOMAIN_INFO)
                                .collect<R, A>(Collectors.toList<T>()))
        return ResponseEntity.ok<DomainInfoResponse>(response)
    }

    fun getSurveyAnswers(
            workspaceNamespace: String, workspaceId: String, questionConceptId: Long?): ResponseEntity<List<SurveyAnswerResponse>> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val answer = conceptBigQueryService.getSurveyAnswer(questionConceptId)
        return ResponseEntity.ok<List<SurveyAnswerResponse>>(answer)
    }

    fun getSurveyQuestions(
            workspaceNamespace: String, workspaceId: String, surveyName: String): ResponseEntity<List<SurveyQuestionsResponse>> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val surveyQuestionAnswerList = conceptBigQueryService.getSurveyQuestions(surveyName)
        return ResponseEntity.ok<List<SurveyQuestionsResponse>>(surveyQuestionAnswerList)
    }

    fun getSurveyInfo(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<SurveysResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val surveyModules = surveyModuleDao.findByOrderByOrderNumberAsc()

        val response = SurveysResponse()
                .items(
                        surveyModules.stream()
                                .map(org.pmiops.workbench.cdr.model.SurveyModule.TO_CLIENT_SURVEY_MODULE)
                                .collect<R, A>(Collectors.toList<T>()))
        return ResponseEntity.ok<SurveysResponse>(response)
    }

    private fun addDomainCounts(
            request: SearchConceptsRequest,
            response: ConceptListResponse,
            matchExp: String?,
            standardConceptFilter: StandardConceptFilter?) {
        if (request.getIncludeDomainCounts() == null || !request.getIncludeDomainCounts()) {
            return
        }
        val allDomainInfos = domainInfoDao.findByOrderByDomainId()
        var domainInfos: List<DomainInfo>? = null
        if (matchExp == null) {
            domainInfos = allDomainInfos
        } else {
            if (standardConceptFilter === StandardConceptFilter.ALL_CONCEPTS) {
                domainInfos = domainInfoDao.findAllMatchConceptCounts(matchExp)
            } else if (standardConceptFilter === StandardConceptFilter.STANDARD_CONCEPTS) {
                domainInfos = domainInfoDao.findStandardConceptCounts(matchExp)
            } else {
                return
            }
        }
        val domainCountMap = Maps.uniqueIndex(domainInfos, ???({ it.getDomainEnum() }))
        // Loop through all domains to populate the results (so we get zeros for domains with no
        // matches.)
        for (domainInfo in allDomainInfos) {
            val domain = domainInfo.domainEnum
            val resultInfo = domainCountMap.get(domain)
            response.addDomainCountsItem(
                    DomainCount()
                            .domain(domain)
                            .conceptCount(
                                    if (resultInfo == null)
                                        0L
                                    else
                                        if (standardConceptFilter === StandardConceptFilter.ALL_CONCEPTS)
                                            resultInfo!!.getAllConceptCount()
                                        else
                                            resultInfo!!.getStandardConceptCount())
                            .name(domainInfo.name))
        }
    }

    private fun addVocabularyCounts(
            request: SearchConceptsRequest,
            response: ConceptListResponse,
            matchExp: String?,
            standardConceptFilter: StandardConceptFilter?) {
        if (request.getDomain() == null
                || request.getIncludeVocabularyCounts() == null
                || !request.getIncludeVocabularyCounts()) {
            return
        }
        val domainId = CommonStorageEnums.domainToDomainId(request.getDomain())
        val vocabularyCounts: List<VocabularyCount>
        if (standardConceptFilter === StandardConceptFilter.ALL_CONCEPTS) {
            if (matchExp == null) {
                vocabularyCounts = domainVocabularyInfoDao.findById_DomainIdOrderById_VocabularyId(domainId).stream()
                        .map(TO_VOCABULARY_ALL_CONCEPT_COUNT)
                        .filter(NOT_ZERO)
                        .collect(Collectors.toList<Any>())
            } else {
                vocabularyCounts = conceptDao.findVocabularyAllConceptCounts(matchExp, domainId).stream()
                        .map(TO_CLIENT_VOCAB_COUNT)
                        .collect(Collectors.toList<Any>())
            }
        } else if (standardConceptFilter === StandardConceptFilter.STANDARD_CONCEPTS) {
            if (matchExp == null) {
                vocabularyCounts = domainVocabularyInfoDao.findById_DomainIdOrderById_VocabularyId(domainId).stream()
                        .map(TO_VOCABULARY_STANDARD_CONCEPT_COUNT)
                        .filter(NOT_ZERO)
                        .collect(Collectors.toList<Any>())
            } else {
                vocabularyCounts = conceptDao.findVocabularyStandardConceptCounts(matchExp, domainId).stream()
                        .map(TO_CLIENT_VOCAB_COUNT)
                        .collect(Collectors.toList<Any>())
            }
        } else {
            return
        }
        response.setVocabularyCounts(vocabularyCounts)
    }

    fun searchConcepts(
            workspaceNamespace: String, workspaceId: String, request: SearchConceptsRequest): ResponseEntity<ConceptListResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        var maxResults = request.getMaxResults()
        var minCount = request.getMinCount()
        if (maxResults == null) {
            maxResults = DEFAULT_MAX_RESULTS
        } else if (maxResults < 1) {
            throw BadRequestException("Invalid value for maxResults: " + maxResults!!)
        } else if (maxResults > MAX_MAX_RESULTS) {
            maxResults = MAX_MAX_RESULTS
        }
        if (minCount == null) {
            minCount = 1
        }
        if (request.getVocabularyIds() != null && request.getVocabularyIds().size() === 0) {
            throw BadRequestException("No vocabulary options selected")
        }
        var standardConceptFilter = request.getStandardConceptFilter()
        if (standardConceptFilter == null) {
            standardConceptFilter = StandardConceptFilter.ALL_CONCEPTS
        }

        val matchExp = ConceptService.modifyMultipleMatchKeyword(
                request.getQuery(), ConceptService.SearchType.CONCEPT_SEARCH)
        // TODO: consider doing these queries in parallel
        val response = ConceptListResponse()
        addDomainCounts(request, response, matchExp, standardConceptFilter)
        addVocabularyCounts(request, response, matchExp, standardConceptFilter)

        var domainIds: List<String>? = null
        if (request.getDomain() != null) {
            domainIds = ImmutableList.of(CommonStorageEnums.domainToDomainId(request.getDomain()))
        }
        val convertedConceptFilter = ConceptService.StandardConceptFilter.valueOf(standardConceptFilter!!.name())

        val concepts = conceptService.searchConcepts(
                request.getQuery(),
                convertedConceptFilter,
                request.getVocabularyIds(),
                domainIds,
                maxResults!!,
                minCount!!,
                if (request.getPageNumber() == null) 0 else request.getPageNumber())

        if (concepts != null) {
            response.setItems(
                    concepts.content.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList<T>()))
        }
        return ResponseEntity.ok<ConceptListResponse>(response)
    }

    fun getValuesFromDomain(
            workspaceNamespace: String, workspaceId: String, domainValue: String): ResponseEntity<DomainValuesResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val response = DomainValuesResponse()

        val domain = Domain.valueOf(domainValue)
        val fieldList = bigQueryService.getTableFieldsFromDomain(domain)
        response.setItems(
                fieldList.stream()
                        .map<Any> { field -> DomainValue().value(field.name) }
                        .collect<R, A>(Collectors.toList<T>()))

        return ResponseEntity.ok<DomainValuesResponse>(response)
    }

    companion object {

        private val DEFAULT_MAX_RESULTS = 20
        private val MAX_MAX_RESULTS = 1000

        internal val TO_CLIENT_CONCEPT = { concept ->
            Concept()
                    .conceptClassId(concept.getConceptClassId())
                    .conceptCode(concept.getConceptCode())
                    .conceptName(concept.getConceptName())
                    .conceptId(concept.getConceptId())
                    .countValue(concept.getCountValue())
                    .domainId(concept.getDomainId())
                    .prevalence(concept.getPrevalence())
                    .standardConcept(
                            ConceptService.STANDARD_CONCEPT_CODE == concept.getStandardConcept())
                    .vocabularyId(concept.getVocabularyId())
                    .conceptSynonyms(concept.getSynonyms())
        }

        internal val TO_VOCABULARY_STANDARD_CONCEPT_COUNT = { domainVocabularyInfo ->
            VocabularyCount()
                    .conceptCount(domainVocabularyInfo.getStandardConceptCount())
                    .vocabularyId(domainVocabularyInfo.getId().getVocabularyId())
        }

        internal val TO_VOCABULARY_ALL_CONCEPT_COUNT = { domainVocabularyInfo ->
            VocabularyCount()
                    .conceptCount(domainVocabularyInfo.getAllConceptCount())
                    .vocabularyId(domainVocabularyInfo.getId().getVocabularyId())
        }

        private val TO_CLIENT_VOCAB_COUNT = { vocabCount ->
            VocabularyCount()
                    .conceptCount(vocabCount.getConceptCount())
                    .vocabularyId(vocabCount.getVocabularyId())
        }

        private val NOT_ZERO = { vocabCount -> vocabCount.getConceptCount() > 0 }
    }
}
