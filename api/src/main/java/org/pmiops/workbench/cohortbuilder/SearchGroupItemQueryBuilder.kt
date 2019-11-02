package org.pmiops.workbench.cohortbuilder

import org.pmiops.workbench.cohortbuilder.util.Validation.from
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.betweenOperator
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.notBetweenAndNotInOperator
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.notZeroAndNotOne
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsEmpty
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotDates
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotNumbers
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotOne
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotTwo
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operatorNull
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.temporalGroupNull

import com.google.api.client.util.Sets
import com.google.cloud.bigquery.QueryParameterValue
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.ListMultimap
import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Collectors
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.AttrName
import org.pmiops.workbench.model.Attribute
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.Modifier
import org.pmiops.workbench.model.ModifierType
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.TemporalMention
import org.pmiops.workbench.model.TemporalTime
import org.pmiops.workbench.utils.OperatorUtils

/** SearchGroupItemQueryBuilder builds BigQuery queries for search group items.  */
object SearchGroupItemQueryBuilder {

    private val STANDARD = 1
    private val SOURCE = 0

    // sql parts to help construct BigQuery sql statements
    private val OR = " or\n"
    private val AND = " and "
    private val UNION_TEMPLATE = "union all\n"
    private val DESC = " desc"
    private val BASE_SQL = (
            "select distinct person_id, entry_date, concept_id\n"
                    + "from `\${projectId}.\${dataSetId}.cb_search_all_events`\n"
                    + "where ")
    private val STANDARD_SQL = "(is_standard = %s and concept_id in unnest(%s))\n"
    private val SOURCE_SQL = STANDARD_SQL
    private val VALUE_AS_NUMBER = "(is_standard = %s and concept_id = %s and value_as_number %s %s)\n"
    private val VALUE_AS_CONCEPT_ID = "(is_standard = %s and concept_id = %s and value_as_concept_id %s unnest(%s))\n"
    private val VALUE_SOURCE_CONCEPT_ID = "(is_standard = %s and concept_id = %s and value_source_concept_id %s unnest(%s))\n"
    private val BP_SQL = "(is_standard = %s and concept_id in unnest(%s)"
    private val SYSTOLIC_SQL = " and systolic %s %s"
    private val DIASTOLIC_SQL = " and diastolic %s %s"

    // sql parts to help construct Temporal BigQuery sql
    private val SAME_ENC = "temp1.person_id = temp2.person_id and temp1.visit_occurrence_id = temp2.visit_occurrence_id\n"
    private val X_DAYS_BEFORE = "temp1.person_id = temp2.person_id and temp1.entry_date <= DATE_SUB(temp2.entry_date, INTERVAL %s DAY)\n"
    private val X_DAYS_AFTER = "temp1.person_id = temp2.person_id and temp1." + "entry_date >= DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n"
    private val WITHIN_X_DAYS_OF = "temp1.person_id = temp2.person_id and temp1.entry_date between " + "DATE_SUB(temp2.entry_date, INTERVAL %s DAY) and DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n"
    private val TEMPORAL_EXIST = (
            "select temp1.person_id\n"
                    + "from (%s) temp1\n"
                    + "where exists (select 1\n"
                    + "from (%s) temp2\n"
                    + "where (%s))\n")
    private val TEMPORAL_JOIN = (
            "select temp1.person_id\n"
                    + "from (%s) temp1\n"
                    + "join (select person_id, visit_occurrence_id, entry_date\n"
                    + "from (%s)\n"
                    + ") temp2 on (%s)\n")
    private val TEMPORAL_SQL = (
            "select person_id, visit_occurrence_id, entry_date%s\n"
                    + "from `\${projectId}.\${dataSetId}.cb_search_all_events`\n"
                    + "where %s\n"
                    + "and person_id in (%s)\n")
    private val RANK_1_SQL = ", rank() over (partition by person_id order by entry_date%s) rn"
    private val TEMPORAL_RANK_1_SQL =
            "select person_id, visit_occurrence_id, entry_date\n" + "from (%s) a\n" + "where rn = 1\n"

    // sql parts to help construct Modifiers BigQuery sql
    private val MODIFIER_SQL_TEMPLATE = "select criteria.person_id from (%s) criteria\n"
    private val OCCURRENCES_SQL_TEMPLATE = "group by criteria.person_id, criteria.concept_id\n" + "having count(criteria.person_id) "
    private val AGE_AT_EVENT_SQL_TEMPLATE = "and age_at_event "
    private val EVENT_DATE_SQL_TEMPLATE = "and entry_date "
    private val ENCOUNTERS_SQL_TEMPLATE = "and visit_concept_id "

    // sql parts to help construct demographic BigQuery sql
    private val DEMO_BASE = "select person_id\n" + "from `\${projectId}.\${dataSetId}.person` p\nwhere\n"
    private val AGE_SQL = (
            "CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) %s %s\n"
                    + "and not exists (\n"
                    + "SELECT 'x' FROM `\${projectId}.\${dataSetId}.death` d\n"
                    + "where d.person_id = p.person_id)\n")
    private val RACE_SQL = "p.race_concept_id in unnest(%s)\n"
    private val GEN_SQL = "p.gender_concept_id in unnest(%s)\n"
    private val ETH_SQL = "p.ethnicity_concept_id in unnest(%s)\n"
    private val SEX_SQL = "p.sex_at_birth_concept_id in unnest(%s)\n"
    private val DEC_SQL = (
            "exists (\n"
                    + "SELECT 'x' FROM `\${projectId}.\${dataSetId}.death` d\n"
                    + "where d.person_id = p.person_id)\n")

    /** Build the inner most sql using search parameters, modifiers and attributes.  */
    fun buildQuery(
            criteriaLookup: Map<SearchParameter, Set<Long>>,
            queryParams: MutableMap<String, QueryParameterValue>,
            queryParts: MutableList<String>,
            searchGroup: SearchGroup) {
        if (searchGroup.getTemporal()) {
            // build the outer temporal sql statement
            val query = buildOuterTemporalQuery(criteriaLookup, queryParams, searchGroup)
            queryParts.add(query)
        } else {
            for (searchGroupItem in searchGroup.getItems()) {
                // build regular sql statement
                val query = buildBaseQuery(criteriaLookup, queryParams, searchGroupItem, searchGroup.getMention())
                queryParts.add(query)
            }
        }
    }

    /** Build the inner most sql  */
    private fun buildBaseQuery(
            criteriaLookup: Map<SearchParameter, Set<Long>>,
            queryParams: MutableMap<String, QueryParameterValue>,
            searchGroupItem: SearchGroupItem,
            mention: TemporalMention): String {
        val standardChildConceptIds = HashSet<Long>()
        val sourceChildConceptIds = HashSet<Long>()
        val queryParts = ArrayList<String>()

        // When building sql for demographics - we query against the person table
        if (DomainType.PERSON.toString().equals(searchGroupItem.getType())) {
            return buildDemoSql(queryParams, searchGroupItem)
        }
        var standard = false
        var source = false
        // Otherwise build sql against flat denormalized search table
        for (param in searchGroupItem.getSearchParameters()) {
            if (param.getAttributes().isEmpty()) {
                if (param.getStandard()) {
                    // make sure we only add the standard concept ids sql template once
                    if (!standard) {
                        queryParts.add(STANDARD_SQL)
                        standard = true
                    }
                    standardChildConceptIds.addAll(childConceptIds(criteriaLookup, ImmutableList.of<SearchParameter>(param)))
                } else {
                    if (!source) {
                        queryParts.add(SOURCE_SQL)
                        source = true
                    }
                    sourceChildConceptIds.addAll(childConceptIds(criteriaLookup, ImmutableList.of<SearchParameter>(param)))
                }
            } else {
                val bpSql = StringBuilder(BP_SQL)
                val bpConceptIds = ArrayList<Long>()
                for (attribute in param.getAttributes()) {
                    validateAttribute(attribute)
                    if (attribute.getConceptId() != null) {
                        // attribute.conceptId is unique to blood pressure attributes
                        // this indicates we need to build a blood pressure sql statement
                        bpConceptIds.add(attribute.getConceptId())
                        processBloodPressureSql(queryParams, bpSql, attribute)
                    } else if (AttrName.NUM.equals(attribute.getName())) {
                        queryParts.add(processNumericalSql(queryParams, param, attribute))
                    } else {
                        queryParts.add(processCategoricalSql(queryParams, param, attribute))
                    }
                }
                if (!bpConceptIds.isEmpty()) {
                    val standardParam = addQueryParameterValue(
                            queryParams, QueryParameterValue.int64(if (param.getStandard()) 1 else 0))
                    // if blood pressure we need to add named parameters for concept ids
                    val cids = QueryParameterValue.array(bpConceptIds.stream().toArray(Long[]::new  /* Currently unsupported in Kotlin */), Long::class.java)
                    val conceptIdsParam = addQueryParameterValue(queryParams, cids)
                    queryParts.add(String.format(bpSql.toString(), standardParam, conceptIdsParam) + ")\n")
                }
            }
        }
        addParamValueAndFormat(queryParams, standardChildConceptIds, queryParts, STANDARD)
        addParamValueAndFormat(queryParams, sourceChildConceptIds, queryParts, SOURCE)
        // need to OR all query parts together since they exist in the same search group item
        val queryPartsSql = "(" + queryParts.joinToString(OR) + ")\n"
        // format the base sql with all query parts
        val baseSql = BASE_SQL + queryPartsSql
        // build modifier sql if modifiers exists
        val modifiedSql = buildModifierSql(baseSql, queryParams, searchGroupItem.getModifiers())
        // build the inner temporal sql if this search group item is temporal
        // otherwise return modifiedSql
        return buildInnerTemporalQuery(
                modifiedSql, queryPartsSql, queryParams, searchGroupItem.getModifiers(), mention)
    }

    /** Build sql statement for demographics  */
    private fun buildDemoSql(
            queryParams: MutableMap<String, QueryParameterValue>, searchGroupItem: SearchGroupItem): String {
        val parameters = searchGroupItem.getSearchParameters()
        val param = parameters.get(0)
        when (CriteriaType.valueOf(param.getType())) {
            AGE -> {
                val attribute = param.getAttributes().get(0)
                val ageNamedParameter1 = addQueryParameterValue(
                        queryParams, QueryParameterValue.int64(Long(attribute.getOperands().get(0))))
                var finaParam = ageNamedParameter1
                if (attribute.getOperands().size() > 1) {
                    val ageNamedParameter2 = addQueryParameterValue(
                            queryParams, QueryParameterValue.int64(Long(attribute.getOperands().get(1))))
                    finaParam = finaParam + AND + ageNamedParameter2
                }
                return DEMO_BASE + String.format(
                        AGE_SQL, OperatorUtils.getSqlOperator(attribute.getOperator()), finaParam)
            }
            GENDER, SEX, ETHNICITY, RACE -> {
                // Gender, Sex, Ethnicity and Race all share the same implementation
                val conceptIds = searchGroupItem.getSearchParameters().stream()
                        .map(???({ SearchParameter.getConceptId() }))
                .toArray(Long[]::new  /* Currently unsupported in Kotlin */)
                val namedParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(conceptIds, Long::class.java))

                val demoSql = if (CriteriaType.RACE.toString().equals(param.getType()))
                    RACE_SQL
                else if (CriteriaType.GENDER.toString().equals(param.getType()))
                    GEN_SQL
                else if (CriteriaType.ETHNICITY.toString().equals(param.getType())) ETH_SQL else SEX_SQL
                return DEMO_BASE + String.format(demoSql, namedParameter)
            }
            DECEASED -> return DEMO_BASE + DEC_SQL
            else -> throw BadRequestException(
                    "Search unsupported for demographics type " + param.getType())
        }
    }

    /**
     * Implementation of temporal CB queries. Please reference the following google doc for details:
     * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
     */
    private fun buildInnerTemporalQuery(
            modifiedSql: String,
            conditionsSql: String,
            queryParams: MutableMap<String, QueryParameterValue>,
            modifiers: List<Modifier>,
            mention: TemporalMention?): String {
        var conditionsSql = conditionsSql
        if (mention == null) {
            return modifiedSql
        }
        // if modifiers exists we need to add them again to the inner temporal sql
        conditionsSql = conditionsSql + getAgeDateAndEncounterSql(queryParams, modifiers)
        if (TemporalMention.ANY_MENTION.equals(mention)) {
            return String.format(TEMPORAL_SQL, "", conditionsSql, modifiedSql)
        } else if (TemporalMention.FIRST_MENTION.equals(mention)) {
            val rank1Sql = String.format(RANK_1_SQL, "")
            val temporalSql = String.format(TEMPORAL_SQL, rank1Sql, conditionsSql, modifiedSql)
            return String.format(TEMPORAL_RANK_1_SQL, temporalSql)
        }
        val rank1Sql = String.format(RANK_1_SQL, DESC)
        val temporalSql = String.format(TEMPORAL_SQL, rank1Sql, conditionsSql, modifiedSql)
        return String.format(TEMPORAL_RANK_1_SQL, temporalSql)
    }

    /**
     * The temporal group functionality description is here:
     * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
     */
    private fun buildOuterTemporalQuery(
            criteriaLookup: Map<SearchParameter, Set<Long>>,
            params: MutableMap<String, QueryParameterValue>,
            searchGroup: SearchGroup): String {
        val temporalQueryParts1 = ArrayList<String>()
        val temporalQueryParts2 = ArrayList<String>()
        val temporalGroups = getTemporalGroups(searchGroup)
        for (key in temporalGroups.keySet()) {
            val tempGroups = temporalGroups.get(key)
            // key of zero indicates belonging to the first temporal group
            // key of one indicates belonging to the second temporal group
            val isFirstGroup = key == 0
            for (tempGroup in tempGroups) {
                val query = buildBaseQuery(criteriaLookup, params, tempGroup, searchGroup.getMention())
                if (isFirstGroup) {
                    temporalQueryParts1.add(query)
                } else {
                    temporalQueryParts2.add(query)
                }
            }
        }
        var conditions = SAME_ENC
        if (TemporalTime.WITHIN_X_DAYS_OF.equals(searchGroup.getTime())) {
            val parameterName = addQueryParameterValue(params, QueryParameterValue.int64(searchGroup.getTimeValue()))
            conditions = String.format(WITHIN_X_DAYS_OF, parameterName, parameterName)
        } else if (TemporalTime.X_DAYS_BEFORE.equals(searchGroup.getTime())) {
            val parameterName = addQueryParameterValue(params, QueryParameterValue.int64(searchGroup.getTimeValue()))
            conditions = String.format(X_DAYS_BEFORE, parameterName)
        } else if (TemporalTime.X_DAYS_AFTER.equals(searchGroup.getTime())) {
            val parameterName = addQueryParameterValue(params, QueryParameterValue.int64(searchGroup.getTimeValue()))
            conditions = String.format(X_DAYS_AFTER, parameterName)
        }
        return String.format(
                if (temporalQueryParts2.size == 1) TEMPORAL_EXIST else TEMPORAL_JOIN,
                temporalQueryParts1.joinToString(UNION_TEMPLATE),
                temporalQueryParts2.joinToString(UNION_TEMPLATE),
                conditions)
    }

    /**
     * Helper method to collect search groups into 2 temporal groups. Key of zero indicates belonging
     * to the first temporal group. Key of one indicates belonging to the second temporal group.
     */
    private fun getTemporalGroups(searchGroup: SearchGroup): ListMultimap<Int, SearchGroupItem> {
        val itemMap = ArrayListMultimap.create<Int, SearchGroupItem>()
        searchGroup
                .getItems()
                .forEach { item ->
                    from(temporalGroupNull())
                            .test(item)
                            .throwException(
                                    "Bad Request: search group item temporal group {0} is not valid.",
                                    item.getTemporalGroup())
                    itemMap.put(item.getTemporalGroup(), item)
                }
        from<ListMultimap<Int, SearchGroupItem>>(notZeroAndNotOne())
                .test(itemMap)
                .throwException(
                        "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).")
        return itemMap
    }

    /** Helper method to build blood pressure sql.  */
    private fun processBloodPressureSql(
            queryParams: MutableMap<String, QueryParameterValue>, sqlBuilder: StringBuilder, attribute: Attribute) {
        if (!AttrName.ANY.equals(attribute.getName())) {
            // this makes an assumption that the UI adds systolic attribute first. Otherwise we will have
            // to hard code the conceptId which is not optimal.
            val sqlTemplate = if (sqlBuilder.toString().contains("systolic")) DIASTOLIC_SQL else SYSTOLIC_SQL
            sqlBuilder.append(
                    String.format(
                            sqlTemplate,
                            OperatorUtils.getSqlOperator(attribute.getOperator()),
                            getOperandsExpression(queryParams, attribute)))
        }
    }

    /** Helper method to create sql statement for attributes of numerical type.  */
    private fun processNumericalSql(
            queryParams: MutableMap<String, QueryParameterValue>,
            parameter: SearchParameter,
            attribute: Attribute): String {
        val standardParam = addQueryParameterValue(
                queryParams, QueryParameterValue.int64(if (parameter.getStandard()) 1 else 0))
        val conceptIdParam = addQueryParameterValue(queryParams, QueryParameterValue.int64(parameter.getConceptId()))
        return String.format(
                VALUE_AS_NUMBER,
                standardParam,
                conceptIdParam,
                OperatorUtils.getSqlOperator(attribute.getOperator()),
                getOperandsExpression(queryParams, attribute))
    }

    /** Helper method to create sql statement for attributes of categorical type.  */
    private fun processCategoricalSql(
            queryParams: MutableMap<String, QueryParameterValue>,
            parameter: SearchParameter,
            attribute: Attribute): String {
        val standardParam = addQueryParameterValue(
                queryParams, QueryParameterValue.int64(if (parameter.getStandard()) 1 else 0))
        val conceptIdParam = addQueryParameterValue(queryParams, QueryParameterValue.int64(parameter.getConceptId()))
        val operandsParam = addQueryParameterValue(
                queryParams,
                QueryParameterValue.array(
                        attribute.getOperands().stream().map({ s -> java.lang.Long.parseLong(s) }).toArray(Long[]::new  /* Currently unsupported in Kotlin */),
                        Long::class.java))
        // if the search parameter is ppi/survey then we need to use different column.
        return String.format(
                if (DomainType.SURVEY.toString().equals(parameter.getDomain()))
                    VALUE_SOURCE_CONCEPT_ID
                else
                    VALUE_AS_CONCEPT_ID,
                standardParam,
                conceptIdParam,
                OperatorUtils.getSqlOperator(attribute.getOperator()),
                operandsParam)
    }

    /** Helper method to build the operand sql expression.  */
    private fun getOperandsExpression(
            queryParams: MutableMap<String, QueryParameterValue>, attribute: Attribute): String {
        val operandsParam1 = addQueryParameterValue(
                queryParams, QueryParameterValue.float64(Double(attribute.getOperands().get(0))))
        val valueExpression: String
        if (attribute.getOperator().equals(Operator.BETWEEN)) {
            val operandsParam2 = addQueryParameterValue(
                    queryParams, QueryParameterValue.float64(Double(attribute.getOperands().get(1))))
            valueExpression = operandsParam1 + AND + operandsParam2
        } else {
            valueExpression = operandsParam1
        }
        return valueExpression
    }

    /** Collect all child nodes per specified search parameters.  */
    private fun childConceptIds(
            criteriaLookup: Map<SearchParameter, Set<Long>>, params: List<SearchParameter>): Set<Long> {
        val out = Sets.newHashSet<Long>()
        for (param in params) {
            if (param.getGroup() || param.getAncestorData()) {
                out.addAll(criteriaLookup[param])
            }
            if (param.getConceptId() != null) {
                // not all SearchParameter have a concept id, so attributes/modifiers
                // are used to find matches in those scenarios.
                out.add(param.getConceptId())
            }
        }
        return out
    }

    /** Helper method to build modifier sql if needed.  */
    private fun buildModifierSql(
            baseSql: String, queryParams: MutableMap<String, QueryParameterValue>, modifiers: List<Modifier>): String {
        validateModifiers(modifiers)
        val ageDateAndEncounterSql = getAgeDateAndEncounterSql(queryParams, modifiers)
        // Number of Occurrences has to be last because of the group by
        val occurrenceSql = buildOccurrencesSql(queryParams, getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES))
        return String.format(MODIFIER_SQL_TEMPLATE, baseSql + ageDateAndEncounterSql) + occurrenceSql
    }

    /**
     * Helper method to build all modifiers together except occurrences since it has to be last
     * because of the group by.
     */
    private fun getAgeDateAndEncounterSql(
            queryParams: MutableMap<String, QueryParameterValue>, modifiers: List<Modifier>): String {
        val ageDateAndEncounterModifiers = ArrayList<Modifier>()
        ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.AGE_AT_EVENT))
        ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.EVENT_DATE))
        ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.ENCOUNTERS))
        val modifierSql = StringBuilder()
        for (modifier in ageDateAndEncounterModifiers) {
            if (modifier == null) {
                continue
            }
            val modifierParamList = ArrayList<String>()
            for (operand in modifier!!.getOperands()) {
                val modifierParameter = addQueryParameterValue(
                        queryParams,
                        if (isAgeAtEvent(modifier!!) || isEncounters(modifier!!))
                            QueryParameterValue.int64(Long(operand))
                        else
                            QueryParameterValue.date(operand))
                modifierParamList.add(modifierParameter)
            }
            if (isAgeAtEvent(modifier!!)) {
                modifierSql.append(AGE_AT_EVENT_SQL_TEMPLATE)
                modifierSql.append(
                        OperatorUtils.getSqlOperator(modifier!!.getOperator())
                                + " "
                                + modifierParamList.joinToString(AND)
                                + "\n")
            } else if (isEncounters(modifier!!)) {
                modifierSql.append(ENCOUNTERS_SQL_TEMPLATE)
                modifierSql.append(
                        OperatorUtils.getSqlOperator(modifier!!.getOperator())
                                + " ("
                                + modifierParamList[0]
                                + ")\n")
            } else {
                modifierSql.append(EVENT_DATE_SQL_TEMPLATE)
                modifierSql.append(
                        OperatorUtils.getSqlOperator(modifier!!.getOperator())
                                + " "
                                + modifierParamList.joinToString(AND)
                                + "\n")
            }
        }
        return modifierSql.toString()
    }

    /** Helper method to build occurrences modifier sql.  */
    private fun buildOccurrencesSql(
            queryParams: MutableMap<String, QueryParameterValue>, occurrences: Modifier?): String {
        val modifierSql = StringBuilder()
        if (occurrences != null) {
            val modifierParamList = ArrayList<String>()
            for (operand in occurrences!!.getOperands()) {
                val modifierParameter = addQueryParameterValue(queryParams, QueryParameterValue.int64(Long(operand)))
                modifierParamList.add(modifierParameter)
            }
            modifierSql.append(
                    OCCURRENCES_SQL_TEMPLATE
                            + OperatorUtils.getSqlOperator(occurrences!!.getOperator())
                            + " "
                            + modifierParamList.joinToString(AND)
                            + "\n")
        }
        return modifierSql.toString()
    }

    /** Add source or standard concept ids and set params *  */
    private fun addParamValueAndFormat(
            queryParams: MutableMap<String, QueryParameterValue>,
            childConceptIds: Set<Long>,
            queryParts: MutableList<String>,
            standardOrSource: Int) {
        if (!childConceptIds.isEmpty()) {
            val standardParam = addQueryParameterValue(queryParams, QueryParameterValue.int64(standardOrSource))
            val cids = QueryParameterValue.array(childConceptIds.stream().toArray(Long[]::new  /* Currently unsupported in Kotlin */), Long::class.java)
            val conceptIdsParam = addQueryParameterValue(queryParams, cids)
            for (i in queryParts.indices) {
                val part = queryParts[i]
                if (part == STANDARD_SQL) {
                    queryParts[i] = String.format(part, standardParam, conceptIdsParam)
                    break
                }
            }
        }
    }

    /** Helper method to return a modifier.  */
    private fun getModifier(modifiers: List<Modifier>, modifierType: ModifierType): Modifier? {
        val modifierList = modifiers.stream()
                .filter { modifier -> modifier.getName().equals(modifierType) }
                .collect(Collectors.toList<Any>())
        return if (modifierList.isEmpty()) {
            null
        } else modifierList[0]
    }

    private fun isAgeAtEvent(modifier: Modifier): Boolean {
        return modifier.getName().equals(ModifierType.AGE_AT_EVENT)
    }

    private fun isEncounters(modifier: Modifier): Boolean {
        return modifier.getName().equals(ModifierType.ENCOUNTERS)
    }

    /** Generate a unique parameter name and add it to the parameter map provided.  */
    private fun addQueryParameterValue(
            queryParameterValueMap: MutableMap<String, QueryParameterValue>,
            queryParameterValue: QueryParameterValue): String {
        val parameterName = "p" + queryParameterValueMap.size
        queryParameterValueMap[parameterName] = queryParameterValue
        return "@$parameterName"
    }

    /** Validate attributes  */
    private fun validateAttribute(attr: Attribute) {
        if (!AttrName.ANY.equals(attr.getName())) {
            from(operatorNull())
                    .test(attr)
                    .throwException("Bad Request: attribute operator {0} is not valid.", attr.getOperator())
            from(operandsEmpty()).test(attr).throwException("Bad Request: attribute operands are empty.")
            from(notBetweenAndNotInOperator().and(operandsNotOne()))
                    .test(attr)
                    .throwException(
                            "Bad Request: attribute {0} must have one operand when using the {1} operator.",
                            attr.getName().toString(), attr.getOperator().toString())
            from(betweenOperator().and(operandsNotTwo()))
                    .test(attr)
                    .throwException(
                            "Bad Request: attribute {0} can only have 2 operands when using the {1} operator",
                            attr.getName().toString(), attr.getOperator().toString())
            from(operandsNotNumbers())
                    .test(attr)
                    .throwException(
                            "Bad Request: attribute {0} operands must be numeric.", attr.getName().toString())
        }
    }

    private fun validateModifiers(modifiers: List<Modifier>) {
        modifiers.forEach { modifier ->
            from(operatorNull())
                    .test(modifier)
                    .throwException(
                            "Bad Request: modifier operator {0} is not valid.", modifier.getOperator())
            from(operandsEmpty())
                    .test(modifier)
                    .throwException("Bad Request: modifier operands are empty.")
            from(notBetweenAndNotInOperator().and(operandsNotOne()))
                    .test(modifier)
                    .throwException(
                            "Bad Request: modifier {0} must have one operand when using the {1} operator.",
                            modifier.getName().toString(), modifier.getOperator().toString())
            from(betweenOperator().and(operandsNotTwo()))
                    .test(modifier)
                    .throwException(
                            "Bad Request: modifier {0} can only have 2 operands when using the {1} operator",
                            modifier.getName().toString(), modifier.getOperator().toString())
            if (ModifierType.EVENT_DATE.equals(modifier.getName())) {
                from(operandsNotDates())
                        .test(modifier)
                        .throwException(
                                "Bad Request: modifier {0} must be a valid date.",
                                modifier.getName().toString())
            } else {
                from(operandsNotNumbers())
                        .test(modifier)
                        .throwException(
                                "Bad Request: modifier {0} operands must be numeric.",
                                modifier.getName().toString())
            }
        }
    }
}
