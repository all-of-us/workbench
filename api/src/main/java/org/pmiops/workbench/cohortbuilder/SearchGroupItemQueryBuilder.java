package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.notOneModifier;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ONE_MODIFIER_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SEARCH_GROUP_ITEM;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TEMPORAL_GROUP;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TEMPORAL_GROUP_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.modifierText;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.notZeroAndNotOne;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.SearchGroupPredicates.temporalGroupNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

import com.google.api.client.util.Sets;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.utils.OperatorUtils;

/** SearchGroupItemQueryBuilder builds BigQuery queries for search group items. */
public final class SearchGroupItemQueryBuilder {

  // sql parts to help construct BigQuery sql statements
  private static final String OR = " or\n";
  private static final String AND = " and ";
  private static final String UNION_TEMPLATE = "union all\n";
  private static final String DESC = " desc";
  private static final String BASE_SQL =
      "select distinct person_id, entry_date, concept_id\n"
          + "from `${projectId}.${dataSetId}.search_all_domains`\n"
          + "where is_standard = %s\n"
          + "and ";
  private static final String CONCEPT_ID_IN = "(concept_id in unnest(%s))\n";
  private static final String VALUE_AS_NUMBER = "(concept_id = %s and value_as_number %s %s)\n";
  private static final String VALUE_AS_CONCEPT_ID =
      "(concept_id = %s and value_as_concept_id %s unnest(%s))\n";
  private static final String VALUE_SOURCE_CONCEPT_ID =
      "(concept_id = %s and value_source_concept_id %s unnest(%s))\n";
  private static final String BP_SQL = "(concept_id in unnest(%s)";
  private static final String SYSTOLIC_SQL = " and systolic %s %s";
  private static final String DIASTOLIC_SQL = " and diastolic %s %s";

  // sql parts to help construct Temporal BigQuery sql
  private static final String SAME_ENC =
      "temp1.person_id = temp2.person_id and temp1.visit_occurrence_id = temp2.visit_occurrence_id\n";
  private static final String X_DAYS_BEFORE =
      "temp1.person_id = temp2.person_id and temp1.entry_date <= DATE_SUB(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String X_DAYS_AFTER =
      "temp1.person_id = temp2.person_id and temp1."
          + "entry_date >= DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String WITHIN_X_DAYS_OF =
      "temp1.person_id = temp2.person_id and temp1.entry_date between "
          + "DATE_SUB(temp2.entry_date, INTERVAL %s DAY) and DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String TEMPORAL_EXIST =
      "select temp1.person_id\n"
          + "from (%s) temp1\n"
          + "where exists (select 1\n"
          + "from (%s) temp2\n"
          + "where (%s))\n";
  private static final String TEMPORAL_JOIN =
      "select temp1.person_id\n"
          + "from (%s) temp1\n"
          + "join (select person_id, visit_occurrence_id, entry_date\n"
          + "from (%s)\n"
          + ") temp2 on (%s)\n";
  private static final String TEMPORAL_SQL =
      "select person_id, visit_occurrence_id, entry_date%s\n"
          + "from `${projectId}.${dataSetId}.search_all_domains`\n"
          + "where %s\n"
          + "and person_id in (%s)\n";
  private static final String RANK_1_SQL =
      ", rank() over (partition by person_id order by entry_date%s) rn";
  private static final String TEMPORAL_RANK_1_SQL =
      "select person_id, visit_occurrence_id, entry_date\n" + "from (%s) a\n" + "where rn = 1\n";

  // sql parts to help construct Modifiers BigQuery sql
  private static final String MODIFIER_SQL_TEMPLATE =
      "select criteria.person_id from (%s) criteria\n";
  private static final String OCCURRENCES_SQL_TEMPLATE =
      "group by criteria.person_id, criteria.concept_id\n" + "having count(criteria.person_id) ";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE = "and age_at_event ";
  private static final String EVENT_DATE_SQL_TEMPLATE = "and entry_date ";
  private static final String ENCOUNTERS_SQL_TEMPLATE = "and visit_concept_id ";

  // sql parts to help construct demographic BigQuery sql
  private static final String DEMO_BASE =
      "select person_id\n" + "from `${projectId}.${dataSetId}.person` p\nwhere\n";
  private static final String AGE_SQL =
      "CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) %s %s and %s\n"
          + "and not exists (\n"
          + "SELECT 'x' FROM `${projectId}.${dataSetId}.death` d\n"
          + "where d.person_id = p.person_id)\n";
  private static final String RACE_SQL = "p.race_concept_id in unnest(%s)\n";
  private static final String GEN_SQL = "p.gender_concept_id in unnest(%s)\n";
  private static final String ETH_SQL = "p.ethnicity_concept_id in unnest(%s)\n";
  private static final String DEC_SQL =
      "exists (\n"
          + "SELECT 'x' FROM `${projectId}.${dataSetId}.death` d\n"
          + "where d.person_id = p.person_id)\n";

  /** Build the inner most sql using search parameters, modifiers and attributes. */
  public static void buildQuery(
      Map<SearchParameter, Set<Long>> criteriaLookup,
      Map<String, QueryParameterValue> queryParams,
      List<String> queryParts,
      SearchGroup searchGroup,
      boolean isEnableListSearch) {
    if (searchGroup.getTemporal()) {
      // build the outer temporal sql statement
      String query =
          buildOuterTemporalQuery(criteriaLookup, queryParams, searchGroup, isEnableListSearch);
      queryParts.add(query);
    } else {
      for (SearchGroupItem searchGroupItem : searchGroup.getItems()) {
        // build regular sql statement
        String query;
        if (isEnableListSearch) {
          query =
              buildBaseQuery(
                  criteriaLookup, queryParams, searchGroupItem, searchGroup.getMention());
        } else {
          // TODO:Remove when new search is finished - freemabd
          query =
              QueryBuilderFactory.getQueryBuilder(FactoryKey.getType(searchGroupItem.getType()))
                  .buildQuery(queryParams, searchGroupItem, searchGroup.getMention());
        }
        queryParts.add(query);
      }
    }
  }

  /** Build the inner most sql */
  private static String buildBaseQuery(
      Map<SearchParameter, Set<Long>> criteriaLookup,
      Map<String, QueryParameterValue> queryParams,
      SearchGroupItem searchGroupItem,
      TemporalMention mention) {
    Set<Long> childConceptIds = new HashSet<>();
    boolean isStandard = false;
    List<String> queryParts = new ArrayList<>();

    // When building sql for demographics - we query against the person table
    if (DomainType.PERSON.toString().equals(searchGroupItem.getType())) {
      return buildDemoSql(queryParams, searchGroupItem);
    }
    // Otherwise build sql against flat denormalized search table
    for (SearchParameter param : searchGroupItem.getSearchParameters()) {
      if (param.getAttributes().isEmpty()) {
        childConceptIds.addAll(childConceptIds(criteriaLookup, ImmutableList.of(param)));
        // make sure we only add the concept ids sql template once
        if (!queryParts.contains(CONCEPT_ID_IN)) {
          queryParts.add(CONCEPT_ID_IN);
        }
      } else {
        StringBuilder bpSql = new StringBuilder(BP_SQL);
        List<Long> bpConceptIds = new ArrayList<>();
        for (Attribute attribute : param.getAttributes()) {
          if (attribute.getConceptId() != null) {
            // this indicates we need to build a blood pressure sql statement
            bpConceptIds.add(attribute.getConceptId());
            processBloodPressureSql(queryParams, bpSql, attribute);
          } else if (AttrName.NUM.equals(attribute.getName())) {
            queryParts.add(processNumericalSql(queryParams, param, attribute));
          } else {
            queryParts.add(processCategoricalSql(queryParams, param, attribute));
          }
        }
        if (!bpConceptIds.isEmpty()) {
          // if blood pressure we need to add named parameters for concept ids
          QueryParameterValue cids =
              QueryParameterValue.array(bpConceptIds.stream().toArray(Long[]::new), Long.class);
          String conceptIdsParam = addQueryParameterValue(queryParams, cids);
          queryParts.add(String.format(bpSql.toString(), conceptIdsParam) + ")\n");
        }
      }
      // all search parameters in the search group item have the same source/standard flag
      isStandard = param.getStandard();
    }
    // need to OR all query parts together since they exist in the same search group item
    String queryPartsSql = "(" + String.join(OR, queryParts) + ")\n";
    if (!childConceptIds.isEmpty()) {
      // if we have concept ids that are non bp then add named parameters
      QueryParameterValue cids =
          QueryParameterValue.array(childConceptIds.stream().toArray(Long[]::new), Long.class);
      String conceptIdsParam = addQueryParameterValue(queryParams, cids);
      queryPartsSql = String.format(queryPartsSql, conceptIdsParam);
    }
    // create the source/standard flag named parameter
    String standardParam =
        addQueryParameterValue(queryParams, QueryParameterValue.int64(isStandard ? 1 : 0));
    // format the base sql with all query parts
    String baseSql = String.format(BASE_SQL, standardParam) + queryPartsSql;
    // build modifier sql if modifiers exists
    String modifiedSql = buildModifierSql(baseSql, queryParams, searchGroupItem.getModifiers());
    // split off the sql conditions in case were building a temporal sql statement
    // otherwise this is just ignored
    String conditionsSql = baseSql.split("where")[1] + "%s";
    // build the inner temporal sql if this search group item is temporal
    // otherwise return modifiedSql
    return buildInnerTemporalQuery(
        modifiedSql, conditionsSql, queryParams, searchGroupItem.getModifiers(), mention);
  }

  /** Build sql statement for demographics */
  private static String buildDemoSql(
      Map<String, QueryParameterValue> queryParams, SearchGroupItem searchGroupItem) {
    // The UI implementation of demographics only allows for 1 search parameter per search group
    // item
    SearchParameter param = searchGroupItem.getSearchParameters().get(0);
    switch (CriteriaType.valueOf(param.getType())) {
      case AGE:
        // age attribute should always have 2 operands since the UI uses a slider
        Attribute attribute = param.getAttributes().get(0);
        String ageNamedParameter1 =
            addQueryParameterValue(
                queryParams, QueryParameterValue.int64(new Long(attribute.getOperands().get(0))));
        String ageNamedParameter2 =
            addQueryParameterValue(
                queryParams, QueryParameterValue.int64(new Long(attribute.getOperands().get(1))));
        return DEMO_BASE
            + String.format(
                AGE_SQL,
                OperatorUtils.getSqlOperator(attribute.getOperator()),
                ageNamedParameter1,
                ageNamedParameter2);
      case GENDER:
      case ETHNICITY:
      case RACE:
        // Gender, Ethnicity and Race all share the same implementation
        Long[] conceptIds =
            searchGroupItem.getSearchParameters().stream()
                .map(SearchParameter::getConceptId)
                .toArray(Long[]::new);
        String namedParameter =
            addQueryParameterValue(queryParams, QueryParameterValue.array(conceptIds, Long.class));
        String demoSql =
            CriteriaType.RACE.toString().equals(param.getType())
                ? RACE_SQL
                : CriteriaType.GENDER.toString().equals(param.getType()) ? GEN_SQL : ETH_SQL;
        return DEMO_BASE + String.format(demoSql, namedParameter);
      case DECEASED:
        return DEMO_BASE + DEC_SQL;
      default:
        throw new BadRequestException(
            "Search unsupported for demographics type " + param.getType());
    }
  }

  /**
   * Implementation of temporal CB queries. Please reference the following google doc for details:
   * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
   */
  private static String buildInnerTemporalQuery(
      String modifiedSql,
      String conditionsSql,
      Map<String, QueryParameterValue> queryParams,
      List<Modifier> modifiers,
      TemporalMention mention) {
    if (mention == null) {
      return modifiedSql;
    }
    // if modifiers exists we need to add them again to the inner temporal sql
    conditionsSql = String.format(conditionsSql, getAgeDateAndEncounterSql(queryParams, modifiers));
    if (TemporalMention.ANY_MENTION.equals(mention)) {
      return String.format(TEMPORAL_SQL, "", conditionsSql, modifiedSql);
    } else if (TemporalMention.FIRST_MENTION.equals(mention)) {
      String rank1Sql = String.format(RANK_1_SQL, "");
      String temporalSql = String.format(TEMPORAL_SQL, rank1Sql, conditionsSql, modifiedSql);
      return String.format(TEMPORAL_RANK_1_SQL, temporalSql);
    }
    String rank1Sql = String.format(RANK_1_SQL, DESC);
    String temporalSql = String.format(TEMPORAL_SQL, rank1Sql, conditionsSql, modifiedSql);
    return String.format(TEMPORAL_RANK_1_SQL, temporalSql);
  }

  /**
   * The temporal group functionality description is here:
   * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8
   */
  private static String buildOuterTemporalQuery(
      Map<SearchParameter, Set<Long>> criteriaLookup,
      Map<String, QueryParameterValue> params,
      SearchGroup searchGroup,
      boolean isEnableListSearch) {
    List<String> temporalQueryParts1 = new ArrayList<>();
    List<String> temporalQueryParts2 = new ArrayList<>();
    ListMultimap<Integer, SearchGroupItem> temporalGroups = getTemporalGroups(searchGroup);
    for (Integer key : temporalGroups.keySet()) {
      List<SearchGroupItem> tempGroups = temporalGroups.get(key);
      // key of zero indicates belonging to the first temporal group
      // key of one indicates belonging to the second temporal group
      boolean isFirstGroup = key == 0;
      for (SearchGroupItem tempGroup : tempGroups) {
        String query;
        if (isEnableListSearch) {
          query = buildBaseQuery(criteriaLookup, params, tempGroup, searchGroup.getMention());
        } else {
          // TODO:Remove when new search is finished - freemabd
          query =
              QueryBuilderFactory.getQueryBuilder(FactoryKey.getType(tempGroup.getType()))
                  .buildQuery(
                      params,
                      tempGroup,
                      isFirstGroup ? searchGroup.getMention() : TemporalMention.ANY_MENTION);
        }
        if (isFirstGroup) {
          temporalQueryParts1.add(query);
        } else {
          temporalQueryParts2.add(query);
        }
      }
    }
    String conditions = SAME_ENC;
    if (TemporalTime.WITHIN_X_DAYS_OF.equals(searchGroup.getTime())) {
      String parameterName =
          addQueryParameterValue(params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(WITHIN_X_DAYS_OF, parameterName, parameterName);
    } else if (TemporalTime.X_DAYS_BEFORE.equals(searchGroup.getTime())) {
      String parameterName =
          addQueryParameterValue(params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(X_DAYS_BEFORE, parameterName);
    } else if (TemporalTime.X_DAYS_AFTER.equals(searchGroup.getTime())) {
      String parameterName =
          addQueryParameterValue(params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(X_DAYS_AFTER, parameterName);
    }
    return String.format(
        temporalQueryParts2.size() == 1 ? TEMPORAL_EXIST : TEMPORAL_JOIN,
        String.join(UNION_TEMPLATE, temporalQueryParts1),
        String.join(UNION_TEMPLATE, temporalQueryParts2),
        conditions);
  }

  /**
   * Helper method to collect search groups into 2 temporal groups. Key of zero indicates belonging
   * to the first temporal group. Key of one indicates belonging to the second temporal group.
   */
  private static ListMultimap<Integer, SearchGroupItem> getTemporalGroups(SearchGroup searchGroup) {
    ListMultimap<Integer, SearchGroupItem> itemMap = ArrayListMultimap.create();
    searchGroup
        .getItems()
        .forEach(
            item -> {
              from(temporalGroupNull())
                  .test(item)
                  .throwException(
                      NOT_VALID_MESSAGE,
                      SEARCH_GROUP_ITEM,
                      TEMPORAL_GROUP,
                      item.getTemporalGroup());
              itemMap.put(item.getTemporalGroup(), item);
            });
    from(notZeroAndNotOne()).test(itemMap).throwException(TEMPORAL_GROUP_MESSAGE);
    return itemMap;
  }

  /** Helper method to build blood pressure sql. */
  private static void processBloodPressureSql(
      Map<String, QueryParameterValue> queryParams, StringBuilder sqlBuilder, Attribute attribute) {
    if (!AttrName.ANY.equals(attribute.getName())) {
      // this makes an assumption that the UI adds systolic attribute first. Otherwise we will have
      // to hard code the conceptId which is not optimal.
      String sqlTemplate =
          sqlBuilder.toString().contains("systolic") ? DIASTOLIC_SQL : SYSTOLIC_SQL;
      sqlBuilder.append(
          String.format(
              sqlTemplate,
              OperatorUtils.getSqlOperator(attribute.getOperator()),
              getOperandsExpression(queryParams, attribute)));
    }
  }

  /** Helper method to create sql statement for attributes of numerical type. */
  private static String processNumericalSql(
      Map<String, QueryParameterValue> queryParams,
      SearchParameter parameter,
      Attribute attribute) {
    String conceptIdParam =
        addQueryParameterValue(queryParams, QueryParameterValue.int64(parameter.getConceptId()));
    return String.format(
        VALUE_AS_NUMBER,
        conceptIdParam,
        OperatorUtils.getSqlOperator(attribute.getOperator()),
        getOperandsExpression(queryParams, attribute));
  }

  /** Helper method to create sql statement for attributes of categorical type. */
  private static String processCategoricalSql(
      Map<String, QueryParameterValue> queryParams,
      SearchParameter parameter,
      Attribute attribute) {
    String operandsParam =
        addQueryParameterValue(
            queryParams,
            QueryParameterValue.array(
                attribute.getOperands().stream().map(s -> Long.parseLong(s)).toArray(Long[]::new),
                Long.class));
    String conceptIdParam =
        addQueryParameterValue(queryParams, QueryParameterValue.int64(parameter.getConceptId()));
    // if the search parameter is ppi/survey then we need to use different column.
    return String.format(
        (DomainType.SURVEY.toString().equals(parameter.getDomain())
            ? VALUE_SOURCE_CONCEPT_ID
            : VALUE_AS_CONCEPT_ID),
        conceptIdParam,
        OperatorUtils.getSqlOperator(attribute.getOperator()),
        operandsParam);
  }

  /** Helper method to build the operand sql expression. */
  private static String getOperandsExpression(
      Map<String, QueryParameterValue> queryParams, Attribute attribute) {
    String operandsParam1 =
        addQueryParameterValue(
            queryParams, QueryParameterValue.float64(new Double(attribute.getOperands().get(0))));
    String valueExpression;
    if (attribute.getOperator().equals(Operator.BETWEEN)) {
      String operandsParam2 =
          addQueryParameterValue(
              queryParams, QueryParameterValue.float64(new Double(attribute.getOperands().get(1))));
      valueExpression = operandsParam1 + AND + operandsParam2;
    } else {
      valueExpression = operandsParam1;
    }
    return valueExpression;
  }

  /** Collect all child nodes per specified search parameters. */
  private static Set<Long> childConceptIds(
      Map<SearchParameter, Set<Long>> criteriaLookup, List<SearchParameter> params) {
    Set<Long> out = Sets.newHashSet();
    for (SearchParameter param : params) {
      if (param.getGroup()) {
        out.addAll(criteriaLookup.get(param));
      }
      if (param.getConceptId() != null) {
        // not all SearchParameter have a concept id, so attributes/modifiers
        // are used to find matches in those scenarios.
        out.add(param.getConceptId());
      }
    }
    return out;
  }

  /** Helper method to build modifier sql if needed. */
  private static String buildModifierSql(
      String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    String ageDateAndEncounterSql = getAgeDateAndEncounterSql(queryParams, modifiers);
    // Number of Occurrences has to be last because of the group by
    String occurrenceSql =
        buildOccurrencesSql(queryParams, getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES));
    return String.format(MODIFIER_SQL_TEMPLATE, baseSql + ageDateAndEncounterSql) + occurrenceSql;
  }

  /**
   * Helper method to build all modifiers together except occurrences since it has to be last
   * because of the group by.
   */
  private static String getAgeDateAndEncounterSql(
      Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    List<Modifier> ageDateAndEncounterModifiers = new ArrayList<>();
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.AGE_AT_EVENT));
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.EVENT_DATE));
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.ENCOUNTERS));
    StringBuilder modifierSql = new StringBuilder();
    for (Modifier modifier : ageDateAndEncounterModifiers) {
      if (modifier != null) {
        List<String> modifierParamList = new ArrayList<>();
        for (String operand : modifier.getOperands()) {
          String modifierParameter =
              addQueryParameterValue(
                  queryParams,
                  (isAgeAtEvent(modifier) || isEncounters(modifier))
                      ? QueryParameterValue.int64(new Long(operand))
                      : QueryParameterValue.date(operand));
          modifierParamList.add(modifierParameter);
        }
        if (isAgeAtEvent(modifier)) {
          modifierSql.append(AGE_AT_EVENT_SQL_TEMPLATE);
          modifierSql.append(
              OperatorUtils.getSqlOperator(modifier.getOperator())
                  + " "
                  + String.join(AND, modifierParamList)
                  + "\n");
        } else if (isEncounters(modifier)) {
          modifierSql.append(ENCOUNTERS_SQL_TEMPLATE);
          modifierSql.append(
              OperatorUtils.getSqlOperator(modifier.getOperator())
                  + " ("
                  + modifierParamList.get(0)
                  + ")\n");
        } else {
          modifierSql.append(EVENT_DATE_SQL_TEMPLATE);
          modifierSql.append(
              OperatorUtils.getSqlOperator(modifier.getOperator())
                  + " "
                  + String.join(AND, modifierParamList)
                  + "\n");
        }
      }
    }
    return modifierSql.toString();
  }

  /** Helper method to build occurrences modifier sql. */
  private static String buildOccurrencesSql(
      Map<String, QueryParameterValue> queryParams, Modifier occurrences) {
    StringBuilder modifierSql = new StringBuilder();
    if (occurrences != null) {
      List<String> modifierParamList = new ArrayList<>();
      for (String operand : occurrences.getOperands()) {
        String modifierParameter =
            addQueryParameterValue(queryParams, QueryParameterValue.int64(new Long(operand)));
        modifierParamList.add(modifierParameter);
      }
      modifierSql.append(
          OCCURRENCES_SQL_TEMPLATE
              + OperatorUtils.getSqlOperator(occurrences.getOperator())
              + " "
              + String.join(AND, modifierParamList)
              + "\n");
    }
    return modifierSql.toString();
  }

  /** Helper method to return a modifier. */
  private static Modifier getModifier(List<Modifier> modifiers, ModifierType modifierType) {
    List<Modifier> modifierList =
        modifiers.stream()
            .filter(modifier -> modifier.getName().equals(modifierType))
            .collect(Collectors.toList());
    if (modifierList.isEmpty()) {
      return null;
    }
    from(notOneModifier())
        .test(modifierList)
        .throwException(ONE_MODIFIER_MESSAGE, modifierText.get(modifierType));
    return modifierList.get(0);
  }

  private static boolean isAgeAtEvent(Modifier modifier) {
    return modifier.getName().equals(ModifierType.AGE_AT_EVENT);
  }

  private static boolean isEncounters(Modifier modifier) {
    return modifier.getName().equals(ModifierType.ENCOUNTERS);
  }

  /** Generate a unique parameter name and add it to the parameter map provided. */
  private static String addQueryParameterValue(
      Map<String, QueryParameterValue> queryParameterValueMap,
      QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return "@" + parameterName;
  }
}
