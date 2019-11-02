package org.pmiops.workbench.api

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Optional
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.inject.Provider
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.cdr.model.CBCriteriaAttribute
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.elasticsearch.ElasticSearchService
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.ConceptIdName
import org.pmiops.workbench.model.CriteriaAttributeListResponse
import org.pmiops.workbench.model.CriteriaListResponse
import org.pmiops.workbench.model.CriteriaSubType
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DemoChartInfo
import org.pmiops.workbench.model.DemoChartInfoListResponse
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.FilterColumns
import org.pmiops.workbench.model.ParticipantDemographics
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortBuilderController @Autowired
internal constructor(
        private val bigQueryService: BigQueryService,
        private val cohortQueryBuilder: CohortQueryBuilder,
        private val cbCriteriaDao: CBCriteriaDao,
        private val cbCriteriaAttributeDao: CBCriteriaAttributeDao,
        private val cdrVersionDao: CdrVersionDao,
        private val cdrVersionService: CdrVersionService,
        private val elasticSearchService: ElasticSearchService,
        private val configProvider: Provider<WorkbenchConfig>) : CohortBuilderApiDelegate {

    fun getCriteriaAutoComplete(
            cdrVersionId: Long?, domain: String, term: String, type: String, standard: Boolean?, limit: Long?): ResponseEntity<CriteriaListResponse> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        val resultLimit = Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT)
        val criteriaResponse = CriteriaListResponse()
        validateDomainAndType(domain, type)
        val matchExp = modifyTermMatch(term)
        var criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
                domain, type, standard, matchExp, PageRequest(0, resultLimit!!.toInt()))
        if (criteriaList.isEmpty()) {
            criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
                    domain, type, standard, term, PageRequest(0, resultLimit.toInt()))
        }
        criteriaResponse.setItems(
                criteriaList.stream().map(TO_CLIENT_CBCRITERIA).collect(Collectors.toList<T>()))

        return ResponseEntity.ok<CriteriaListResponse>(criteriaResponse)
    }

    fun getDrugBrandOrIngredientByValue(
            cdrVersionId: Long?, value: String, limit: Long?): ResponseEntity<CriteriaListResponse> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        val resultLimit = Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT)
        val criteriaResponse = CriteriaListResponse()
        val criteriaList = cbCriteriaDao.findDrugBrandOrIngredientByValue(value, resultLimit)
        criteriaResponse.setItems(
                criteriaList.stream().map(TO_CLIENT_CBCRITERIA).collect(Collectors.toList<T>()))
        return ResponseEntity.ok<CriteriaListResponse>(criteriaResponse)
    }

    fun getDrugIngredientByConceptId(
            cdrVersionId: Long?, conceptId: Long?): ResponseEntity<CriteriaListResponse> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        val criteriaResponse = CriteriaListResponse()
        val criteriaList = cbCriteriaDao.findDrugIngredientByConceptId(conceptId.toString())
        criteriaResponse.setItems(
                criteriaList.stream().map(TO_CLIENT_CBCRITERIA).collect(Collectors.toList<T>()))
        return ResponseEntity.ok<CriteriaListResponse>(criteriaResponse)
    }

    /**
     * This method will return a count of unique subjects defined by the provided [ ].
     */
    fun countParticipants(cdrVersionId: Long?, request: SearchRequest): ResponseEntity<Long> {
        val cdrVersion = cdrVersionDao.findOne(cdrVersionId)
        cdrVersionService.setCdrVersion(cdrVersion)
        if (configProvider.get().elasticsearch.enableElasticsearchBackend
                && !Strings.isNullOrEmpty(cdrVersion.elasticIndexBaseName)
                && !isApproximate(request)) {
            try {
                return ResponseEntity.ok(elasticSearchService.count(request))
            } catch (e: IOException) {
                log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e)
            }

        }
        val qjc = bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildParticipantCounterQuery(ParticipantCriteria(request)))
        val result = bigQueryService.executeQuery(qjc)
        val rm = bigQueryService.getResultMapper(result)
        val row = result.iterateAll().iterator().next()
        val count = bigQueryService.getLong(row, rm["count"])
        return ResponseEntity.ok(count)
    }

    fun findCriteriaByDomainAndSearchTerm(
            cdrVersionId: Long?, domain: String, term: String, limit: Long?): ResponseEntity<CriteriaListResponse> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        var criteriaList: MutableList<CBCriteria>
        val resultLimit = Optional.ofNullable(limit).orElse(DEFAULT_CRITERIA_SEARCH_LIMIT).toInt()
        val exactMatchByCode = cbCriteriaDao.findExactMatchByCode(domain, term)
        val isStandard = exactMatchByCode.isEmpty() || exactMatchByCode[0].standard

        if (!isStandard) {
            criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeAndCode(
                    domain,
                    exactMatchByCode[0].type,
                    isStandard,
                    term,
                    PageRequest(0, resultLimit))

            val groups = criteriaList.stream().collect<Map<Boolean, List<CBCriteria>>, Any>(Collectors.partitioningBy { c -> c.code == term })
            criteriaList = groups[true]
            criteriaList.addAll(groups[false])

        } else {
            criteriaList = cbCriteriaDao.findCriteriaByDomainAndCode(
                    domain, isStandard, term, PageRequest(0, resultLimit))
            if (criteriaList.isEmpty() && !term.contains(".")) {
                val modTerm = modifyTermMatch(term)
                criteriaList = cbCriteriaDao.findCriteriaByDomainAndSynonyms(
                        domain, isStandard, modTerm, PageRequest(0, resultLimit))
            }
            if (criteriaList.isEmpty() && !term.contains(".")) {
                val modTerm = modifyTermMatch(term)
                criteriaList = cbCriteriaDao.findCriteriaByDomainAndSynonyms(
                        domain, !isStandard, modTerm, PageRequest(0, resultLimit))
            }
        }
        val criteriaResponse = CriteriaListResponse()
                .items(criteriaList.stream().map(TO_CLIENT_CBCRITERIA).collect(Collectors.toList<T>()))

        return ResponseEntity.ok<CriteriaListResponse>(criteriaResponse)
    }

    fun getStandardCriteriaByDomainAndConceptId(
            cdrVersionId: Long?, domain: String, conceptId: Long?): ResponseEntity<CriteriaListResponse> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        // These look ups can be done as one dao call but to make this code testable with the mysql
        // fulltext search match function and H2 in memory database, it's split into 2 separate calls
        // Each call is sub second, so having 2 calls and being testable is better than having one call
        // and it being non-testable.
        val conceptIds = cbCriteriaDao.findConceptId2ByConceptId1(conceptId).stream()
                .map { c -> c.toString() }
                .collect<List<String>, Any>(Collectors.toList())
        var criteriaList: List<CBCriteria> = ArrayList()
        if (!conceptIds.isEmpty()) {
            criteriaList = cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(domain, true, conceptIds)
        }
        val criteriaResponse = CriteriaListResponse()
                .items(criteriaList.stream().map(TO_CLIENT_CBCRITERIA).collect(Collectors.toList<T>()))
        return ResponseEntity.ok<CriteriaListResponse>(criteriaResponse)
    }

    fun getDemoChartInfo(
            cdrVersionId: Long?, request: SearchRequest): ResponseEntity<DemoChartInfoListResponse> {
        val response = DemoChartInfoListResponse()
        if (request.getIncludes().isEmpty()) {
            return ResponseEntity.ok<DemoChartInfoListResponse>(response)
        }
        val cdrVersion = cdrVersionDao.findOne(cdrVersionId)
        cdrVersionService.setCdrVersion(cdrVersion)
        if (configProvider.get().elasticsearch.enableElasticsearchBackend
                && !Strings.isNullOrEmpty(cdrVersion.elasticIndexBaseName)
                && !isApproximate(request)) {
            try {
                return ResponseEntity.ok(response.items(elasticSearchService.demoChartInfo(request)))
            } catch (e: IOException) {
                log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e)
            }

        }
        val qjc = bigQueryService.filterBigQueryConfig(
                cohortQueryBuilder.buildDemoChartInfoCounterQuery(ParticipantCriteria(request)))
        val result = bigQueryService.executeQuery(qjc)
        val rm = bigQueryService.getResultMapper(result)

        for (row in result.iterateAll()) {
            response.addItemsItem(
                    DemoChartInfo()
                            .gender(bigQueryService.getString(row, rm["gender"]))
                            .race(bigQueryService.getString(row, rm["race"]))
                            .ageRange(bigQueryService.getString(row, rm["ageRange"]))
                            .count(bigQueryService.getLong(row, rm["count"])))
        }
        return ResponseEntity.ok<DemoChartInfoListResponse>(response)
    }

    fun getCriteriaAttributeByConceptId(
            cdrVersionId: Long?, conceptId: Long?): ResponseEntity<CriteriaAttributeListResponse> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        val criteriaAttributeResponse = CriteriaAttributeListResponse()
        val criteriaAttributeList = cbCriteriaAttributeDao.findCriteriaAttributeByConceptId(conceptId)
        criteriaAttributeResponse.setItems(
                criteriaAttributeList.stream()
                        .map(TO_CLIENT_CBCRITERIA_ATTRIBUTE)
                        .collect(Collectors.toList<T>()))
        return ResponseEntity.ok<CriteriaAttributeListResponse>(criteriaAttributeResponse)
    }

    fun getCriteriaBy(
            cdrVersionId: Long?, domain: String, type: String, standard: Boolean?, parentId: Long?): ResponseEntity<CriteriaListResponse> {
        val criteriaResponse = CriteriaListResponse()
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))

        validateDomainAndType(domain, type)

        val criteriaList: List<CBCriteria>
        if (parentId != null) {
            criteriaList = cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
                    domain, type, standard, parentId)
        } else {
            criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(domain, type)
        }
        criteriaResponse.setItems(
                criteriaList.stream().map(TO_CLIENT_CBCRITERIA).collect(Collectors.toList<T>()))

        return ResponseEntity.ok<CriteriaListResponse>(criteriaResponse)
    }

    fun getParticipantDemographics(cdrVersionId: Long?): ResponseEntity<ParticipantDemographics> {
        cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId))
        val criteriaList = cbCriteriaDao.findGenderRaceEthnicity()
        val genderList = criteriaList.stream()
                .filter { c -> c.type == FilterColumns.GENDER.toString() }
                .map<Any> { c ->
                    ConceptIdName()
                            .conceptId(Long(c.conceptId))
                            .conceptName(c.name)
                }
                .collect<List<ConceptIdName>, Any>(Collectors.toList())
        val raceList = criteriaList.stream()
                .filter { c -> c.type == FilterColumns.RACE.toString() }
                .map<Any> { c ->
                    ConceptIdName()
                            .conceptId(Long(c.conceptId))
                            .conceptName(c.name)
                }
                .collect<List<ConceptIdName>, Any>(Collectors.toList())
        val ethnicityList = criteriaList.stream()
                .filter { c -> c.type == FilterColumns.ETHNICITY.toString() }
                .map<Any> { c ->
                    ConceptIdName()
                            .conceptId(Long(c.conceptId))
                            .conceptName(c.name)
                }
                .collect<List<ConceptIdName>, Any>(Collectors.toList())

        val participantDemographics = ParticipantDemographics()
                .genderList(genderList)
                .raceList(raceList)
                .ethnicityList(ethnicityList)
        return ResponseEntity.ok<ParticipantDemographics>(participantDemographics)
    }

    /**
     * This method helps determine what request can only be approximated by elasticsearch and must
     * fallback to the BQ implementation.
     *
     * @param request
     * @return
     */
    fun isApproximate(request: SearchRequest): Boolean {
        val allGroups = ImmutableList.copyOf(Iterables.concat(request.getIncludes(), request.getExcludes()))
        val allParams = allGroups.stream()
                .flatMap({ sg -> sg.getItems().stream() })
                .flatMap({ sgi -> sgi.getSearchParameters().stream() })
                .collect(Collectors.toList<Any>())
        return allGroups.stream().anyMatch({ sg -> sg.getTemporal() }) || allParams.stream().anyMatch({ sp -> CriteriaSubType.BP.toString().equals(sp.getSubtype()) })
    }

    private fun modifyTermMatch(term: String?): String {
        if (term == null || term.trim { it <= ' ' }.isEmpty()) {
            throw BadRequestException(
                    String.format(
                            "Bad Request: Please provide a valid search term: \"%s\" is not valid.", term))
        }
        val keywords = term.split("\\W+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (keywords.size == 1 && keywords[0].length <= 3) {
            "+\"" + keywords[0]
        } else IntStream.range(0, keywords.size)
                .filter { i -> keywords[i].length > 2 }
                .mapToObj { i ->
                    if (i + 1 != keywords.size) {
                        return@IntStream.range(0, keywords.length)
                                .filter(i -> keywords[i].length() > 2)
                        .mapToObj "+\""+keywords[i]+"\""
                    }
                    "+" + keywords[i] + "*"
                }
                .collect<String, *>(Collectors.joining())

    }

    private fun validateDomainAndType(domain: String, type: String) {
        Optional.ofNullable(domain)
                .orElseThrow { BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)) }
        Optional.ofNullable(type)
                .orElseThrow { BadRequestException(String.format(BAD_REQUEST_MESSAGE, "type", type)) }
        Arrays.stream(DomainType.values())
                .filter({ domainType -> domainType.toString().equalsIgnoreCase(domain) })
                .findFirst()
                .orElseThrow(
                        { BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)) })
        Optional.ofNullable(type)
                .ifPresent { t ->
                    Arrays.stream(CriteriaType.values())
                            .filter({ critType -> critType.toString().equalsIgnoreCase(t) })
                            .findFirst()
                            .orElseThrow(
                                    {
                                        BadRequestException(
                                                String.format(BAD_REQUEST_MESSAGE, "type", t))
                                    })
                }
    }

    companion object {

        private val log = Logger.getLogger(CohortBuilderController::class.java.name)
        private val DEFAULT_TREE_SEARCH_LIMIT = 100L
        private val DEFAULT_CRITERIA_SEARCH_LIMIT = 250L
        private val BAD_REQUEST_MESSAGE = "Bad Request: Please provide a valid %s. %s is not valid."

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        private val TO_CLIENT_CBCRITERIA = { cbCriteria ->
            org.pmiops.workbench.model.Criteria()
                    .id(cbCriteria.getId())
                    .parentId(cbCriteria.getParentId())
                    .type(cbCriteria.getType())
                    .subtype(cbCriteria.getSubtype())
                    .code(cbCriteria.getCode())
                    .name(cbCriteria.getName())
                    .count(
                            if (StringUtils.isEmpty(cbCriteria.getCount()))
                                null
                            else
                                Long(cbCriteria.getCount()))
                    .group(cbCriteria.getGroup())
                    .selectable(cbCriteria.getSelectable())
                    .conceptId(
                            if (StringUtils.isEmpty(cbCriteria.getConceptId()))
                                null
                            else
                                Long(cbCriteria.getConceptId()))
                    .domainId(cbCriteria.getDomainId())
                    .hasAttributes(cbCriteria.getAttribute())
                    .path(cbCriteria.getPath())
                    .hasAncestorData(cbCriteria.getAncestorData())
                    .hasHierarchy(cbCriteria.getHierarchy())
                    .isStandard(cbCriteria.getStandard())
                    .value(cbCriteria.getValue())
        }

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        private val TO_CLIENT_CBCRITERIA_ATTRIBUTE = { cbCriteria ->
            org.pmiops.workbench.model.CriteriaAttribute()
                    .id(cbCriteria.id)
                    .valueAsConceptId(cbCriteria.valueAsConceptId)
                    .conceptName(cbCriteria.conceptName)
                    .type(cbCriteria.type)
                    .estCount(cbCriteria.estCount)
        }
    }
}
