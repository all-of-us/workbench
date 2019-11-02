package org.pmiops.workbench.elasticsearch

import com.google.api.client.util.Sets
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import java.util.logging.Logger
import java.util.stream.Collectors
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.RangeQueryBuilder
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cohortbuilder.util.CriteriaLookupUtil
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.AttrName
import org.pmiops.workbench.model.Attribute
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.Modifier
import org.pmiops.workbench.model.ModifierType
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest

/**
 * Utility for conversion of Cohort Builder request into Elasticsearch filters. Instances of this
 * class are used internally to track metadata during request processing.
 */
class ElasticFilters private constructor(cbCriteriaDao: CBCriteriaDao) {

    private val criteriaLookupUtil: CriteriaLookupUtil

    private var processed = false
    private var childrenByCriteriaGroup: Map<SearchParameter, Set<Long>>? = null

    init {
        this.criteriaLookupUtil = CriteriaLookupUtil(cbCriteriaDao)
    }

    private fun process(req: SearchRequest): QueryBuilder {
        Preconditions.checkArgument(!processed)
        childrenByCriteriaGroup = criteriaLookupUtil.buildCriteriaLookupMap(req)

        val filter = QueryBuilders.boolQuery()
        for (sg in req.getIncludes()) {
            filter.filter(searchGroupToFilter(sg))
        }
        for (sg in req.getExcludes()) {
            // Only case to use mustNot is when both includes and excludes exist together
            if (req.getIncludes().isEmpty()) {
                filter.filter(searchGroupToFilter(sg))
            } else {
                filter.mustNot(searchGroupToFilter(sg))
            }
        }
        processed = true
        return filter
    }

    /**
     * Every criteria ID (one or more concept IDs) in a SearchGroup is effectively OR'd together
     * implemented via a combination of Elastic nested filters and should's. In particular,
     * NUM_OF_OCCURRENCES queries force an independent evaluation of each criteria ID.
     */
    private fun searchGroupToFilter(sg: SearchGroup): QueryBuilder {
        val filter = QueryBuilders.boolQuery()

        for (sgi in sg.getItems()) {
            // Modifiers apply to all criteria in this SearchGroupItem, but will be reapplied to each
            // subquery generated for each criteria ID.
            val modFilters = Lists.newArrayList<QueryBuilder>()

            // Note: For now we roll all non-trivial modifiers into one standard clause template. It's
            // conceivable applying the "N occurrences" pattern everywhere may cause performance problems;
            // if so, non-occurrence value modifiers can be split out as a separate template.
            var occurredAtLeast = 1
            for (mod in sgi.getModifiers()) {
                when (mod.getName()) {
                    NUM_OF_OCCURRENCES -> occurredAtLeast = Integer.parseInt(Iterables.getOnlyElement(mod.getOperands()))
                    EVENT_DATE, AGE_AT_EVENT -> modFilters.add(dateModifierToQuery(mod))
                    ENCOUNTERS -> modFilters.add(QueryBuilders.termsQuery("events.visit_concept_id", mod.getOperands()))
                    else -> throw BadRequestException("Unknown modifier type: " + mod.getName())
                }
            }

            // TODO(freemabd): Handle Blood Pressure and Deceased
            for (param in sgi.getSearchParameters()) {
                var conceptField = "events." + if (param.getStandard()) "concept_id" else "source_concept_id"
                if (isNonNestedSchema(param)) {
                    conceptField = NON_NESTED_FIELDS.get(param.getType())
                }
                val leafConceptIds = toleafConceptIds(ImmutableList.of<SearchParameter>(param))
                val b = QueryBuilders.boolQuery()
                if (!leafConceptIds.isEmpty()) {
                    b.filter(QueryBuilders.termsQuery(conceptField, leafConceptIds))
                } else {
                    // should represent a deceased query
                    val isDeceased = CriteriaType.valueOf(param.getType()).equals(CriteriaType.DECEASED)
                    b.filter(QueryBuilders.termQuery("is_deceased", isDeceased))
                }
                for (attr in param.getAttributes()) {
                    b.filter(attributeToQuery(attr, DomainType.SURVEY.toString().equals(param.getDomain())))
                }
                for (f in modFilters) {
                    b.filter(f)
                }

                if (isNonNestedSchema(param)) {
                    // setup non nested filter with proper field
                    filter.should(b)
                } else {
                    // "should" gives us "OR" behavior so long as we're in a filter context, which we are.
                    // This translates to N occurrences of criteria 1 OR N occurrences of criteria 2, etc.
                    filter.should(
                            QueryBuilders.functionScoreQuery(
                                    QueryBuilders.nestedQuery(
                                            // We sum a constant score for each matching document, yielding the total
                                            // number of matching nested documents (events).
                                            "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
                                    .setMinScore(occurredAtLeast.toFloat()))
                }
            }
        }

        return filter
    }

    private fun toleafConceptIds(params: List<SearchParameter>): Set<String> {
        val out = Sets.newHashSet<String>()
        for (param in params) {
            if (param.getGroup()) {
                out.addAll(
                        childrenByCriteriaGroup!![param].stream()
                                .map { id -> java.lang.Long.toString(id!!) }
                                .collect<Set<String>, Any>(Collectors.toSet()))
            }
            if (param.getConceptId() != null) {
                // not all SearchParameter have a concept id, so attributes/modifiers
                // are used to find matches in those scenarios.
                out.add(java.lang.Long.toString(param.getConceptId()))
            }
        }
        return out
    }

    companion object {

        private val log = Logger.getLogger(ElasticFilters::class.java.name)

        /** Translates a Cohort Builder search request into an Elasticsearch filter.  */
        fun fromCohortSearch(cbCriteriaDao: CBCriteriaDao, req: SearchRequest): QueryBuilder {
            val f = ElasticFilters(cbCriteriaDao)
            return f.process(req)
        }

        private val NON_NESTED_FIELDS = ImmutableMap.of(
                CriteriaType.GENDER.toString(), "gender_concept_id",
                CriteriaType.RACE.toString(), "race_concept_id",
                CriteriaType.ETHNICITY.toString(), "ethnicity_concept_id")

        private fun attributeToQuery(attr: Attribute, isSourceConceptId: Boolean): QueryBuilder {
            // Attributes with a name of CAT map to the value_as_concept_id column
            if (AttrName.CAT.equals(attr.getName())) {
                // Currently the UI only uses the In operator for CAT which fits the terms query
                val name = if (isSourceConceptId) "events.value_as_source_concept_id" else "events.value_as_concept_id"
                return QueryBuilders.termsQuery(name, attr.getOperands())
            }
            var left: Any? = null
            var right: Any? = null
            val rq: RangeQueryBuilder
            if (AttrName.NUM.equals(attr.getName())) {
                rq = QueryBuilders.rangeQuery("events.value_as_number")
                left = java.lang.Float.parseFloat(attr.getOperands().get(0))
                if (attr.getOperands().size() > 1) {
                    right = java.lang.Float.parseFloat(attr.getOperands().get(1))
                }
                when (attr.getOperator()) {
                    LESS_THAN_OR_EQUAL_TO -> rq.lte(left)
                    GREATER_THAN_OR_EQUAL_TO -> rq.gte(left)
                    BETWEEN -> rq.gte(left).lte(right)
                    EQUAL -> rq.gte(left).lte(left)
                    else -> throw BadRequestException("Bad operator for attribute: " + attr.getOperator())
                }
                return rq
            } else if (AttrName.AGE.equals(attr.getName())) {
                rq = QueryBuilders.rangeQuery("birth_datetime")
                // use the low end of the age range to calculate the high end(right) of the date range
                right = ElasticUtils.todayMinusYears(Integer.parseInt(attr.getOperands().get(0)))
                if (attr.getOperands().size() > 1) {
                    // use high end of the age range to calculate the low end(left) of the date range
                    // need to add 1 year to adjust to the beginning of the date range
                    // Ex: 2019-03-19(current date) - 55year(age) - 1 year = 1963-03-19
                    // Need to use GT to make sure not to include 1963-03-19 which evaluates to 56 years old
                    // which is out the range of 55. 1963-03-20 evaluates to 55 years 11 months 30 days.
                    left = ElasticUtils.todayMinusYears(Integer.parseInt(attr.getOperands().get(1)) + 1)
                }
                when (attr.getOperator()) {
                    BETWEEN -> rq.gt(left).lte(right).format("yyyy-MM-dd")
                    else -> throw BadRequestException("Bad operator for attribute: " + attr.getOperator())
                }
                return rq
            }
            throw BadRequestException("attribute name is not an attr name type: " + attr.getName())
        }

        private fun dateModifierToQuery(mod: Modifier): QueryBuilder {
            val rq: RangeQueryBuilder
            val left: Any
            var right: Any? = null
            if (ModifierType.EVENT_DATE.equals(mod.getName())) {
                rq = QueryBuilders.rangeQuery("events.start_date")
                left = mod.getOperands().get(0)
                if (mod.getOperands().size() > 1) {
                    right = mod.getOperands().get(1)
                }
            } else if (ModifierType.AGE_AT_EVENT.equals(mod.getName())) {
                rq = QueryBuilders.rangeQuery("events.age_at_start")
                left = Integer.parseInt(mod.getOperands().get(0))
                if (mod.getOperands().size() > 1) {
                    right = Integer.parseInt(mod.getOperands().get(1))
                }
            } else {
                throw RuntimeException("modifier is not a date modifier type: " + mod.getName())
            }
            when (mod.getOperator()) {
                LESS_THAN -> rq.lt(left)
                GREATER_THAN -> rq.gt(left)
                LESS_THAN_OR_EQUAL_TO -> rq.lte(left)
                GREATER_THAN_OR_EQUAL_TO -> rq.gte(left)
                BETWEEN -> rq.gte(left).lte(right)
                LIKE, IN, EQUAL, NOT_EQUAL -> throw BadRequestException("Bad operator for date modifier: " + mod.getOperator())
                else -> throw BadRequestException("Bad operator for date modifier: " + mod.getOperator())
            }
            return rq
        }

        private fun isNonNestedSchema(param: SearchParameter): Boolean {
            val domainType = DomainType.valueOf(param.getDomain())
            return DomainType.PERSON.equals(domainType)
        }
    }
}
