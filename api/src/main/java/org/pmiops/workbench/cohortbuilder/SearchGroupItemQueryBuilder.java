package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.cohortbuilder.util.Validation.from;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.betweenOperator;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.notBetweenAndNotInOperator;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.notZeroAndNotOne;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsEmpty;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotDates;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotOne;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operandsNotTwo;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.operatorNull;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.temporalGroupNull;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
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

  private static final int STANDARD = 1;
  private static final int SOURCE = 0;

  private static final ImmutableMap<AttrName, String> AGE_COLUMN_SQL_MAP =
      ImmutableMap.of(
          AttrName.AGE,
              "DATE_DIFF(CURRENT_DATE,dob, YEAR) - IF(EXTRACT(MONTH FROM dob)*100 + EXTRACT(DAY FROM dob) > EXTRACT(MONTH FROM CURRENT_DATE)*100 + EXTRACT(DAY FROM CURRENT_DATE),1,0)",
          AttrName.AGE_AT_CONSENT, "age_at_consent",
          AttrName.AGE_AT_CDR, "age_at_cdr");

  private static final ImmutableMap<CriteriaType, String> DEMO_COLUMN_SQL_MAP =
      ImmutableMap.of(
          CriteriaType.RACE, "race_concept_id",
          CriteriaType.GENDER, "gender_concept_id",
          CriteriaType.SEX, "sex_at_birth_concept_id",
          CriteriaType.ETHNICITY, "ethnicity_concept_id");
  private static final ImmutableList<Domain> SOURCE_STANDARD_DOMAINS =
      ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE);
  private static final ImmutableMap<Domain, String> HAS_DATA_DOMAINS =
      ImmutableMap.ofEntries(
          new AbstractMap.SimpleEntry<>(Domain.FITBIT, "has_fitbit"),
          new AbstractMap.SimpleEntry<>(
              Domain.FITBIT_HEART_RATE_SUMMARY, "has_fitbit_heart_rate_summary"),
          new AbstractMap.SimpleEntry<>(
              Domain.FITBIT_HEART_RATE_LEVEL, "has_fitbit_heart_rate_level"),
          new AbstractMap.SimpleEntry<>(Domain.FITBIT_ACTIVITY, "has_fitbit_activity_summary"),
          new AbstractMap.SimpleEntry<>(Domain.FITBIT_INTRADAY_STEPS, "has_fitbit_steps_intraday"),
          new AbstractMap.SimpleEntry<>(
              Domain.FITBIT_SLEEP_DAILY_SUMMARY, "has_fitbit_sleep_daily_summary"),
          new AbstractMap.SimpleEntry<>(Domain.FITBIT_SLEEP_LEVEL, "has_fitbit_sleep_level"),
          new AbstractMap.SimpleEntry<>(Domain.WHOLE_GENOME_VARIANT, "has_whole_genome_variant"),
          new AbstractMap.SimpleEntry<>(
              Domain.PHYSICAL_MEASUREMENT, "has_physical_measurement_data"),
          new AbstractMap.SimpleEntry<>(Domain.ARRAY_DATA, "has_array_data"),
          new AbstractMap.SimpleEntry<>(
              Domain.LR_WHOLE_GENOME_VARIANT, "has_lr_whole_genome_variant"),
          new AbstractMap.SimpleEntry<>(
              Domain.STRUCTURAL_VARIANT_DATA, "has_structural_variant_data"));

  // sql parts to help construct BigQuery sql statements
  private static final String OR = " OR ";
  private static final String AND = " AND ";
  private static final String UNION_TEMPLATE = "UNION ALL\n";
  private static final String DESC = " DESC";
  private static final String BASE_SQL =
      "SELECT DISTINCT person_id, entry_date, concept_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_all_events`\n"
          + "WHERE ";
  private static final String STANDARD_SQL = "is_standard = %s";
  private static final String CONCEPT_ID_UNNEST_SQL = "concept_id IN unnest(%s)";
  private static final String CONCEPT_ID_IN_SQL = "concept_id IN";
  private static final String VALUE_SOURCE_CONCEPT_ID_IN_SQL = " value_source_concept_id IN";
  private static final String STANDARD_OR_SOURCE_SQL =
      CONCEPT_ID_UNNEST_SQL + AND + STANDARD_SQL + "\n";
  public static final String CHILD_LOOKUP_SQL =
      " (SELECT DISTINCT c.concept_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "JOIN (select cast(cr.id as string) as id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "WHERE concept_id IN unnest(%s)\n"
          + "AND full_text LIKE '%%_rank1]%%') a\n"
          + "ON (c.path LIKE CONCAT('%%.', a.id, '.%%') OR c.path LIKE CONCAT('%%.', a.id) OR c.path LIKE CONCAT(a.id, '.%%') OR c.path = a.id)\n"
          + "WHERE is_standard = %s\n"
          + "AND is_selectable = 1)";
  public static final String DRUG_CHILD_LOOKUP_SQL =
      " (SELECT DISTINCT ca.descendant_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria_ancestor` ca\n"
          + "JOIN (select distinct c.concept_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "JOIN (select cast(cr.id as string) as id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "WHERE concept_id IN unnest(%s)\n"
          + "AND full_text LIKE '%%_rank1]%%') a\n"
          + "ON (c.path LIKE CONCAT('%%.', a.id, '.%%') OR c.path LIKE CONCAT('%%.', a.id) OR c.path LIKE CONCAT(a.id, '.%%') OR c.path = a.id)\n"
          + "WHERE is_standard = %s\n"
          + "AND is_selectable = 1) b ON (ca.ancestor_id = b.concept_id))";
  public static final String ANSWER_LOOKUP_SQL =
      " (SELECT DISTINCT CAST(c.value AS INT64) as value\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "JOIN (select cast(cr.id as string) as id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "WHERE concept_id IN unnest(%s)\n"
          + "AND domain_id = 'SURVEY') a\n"
          + "ON (c.path LIKE CONCAT('%%.', a.id, '.%%') OR c.path LIKE CONCAT('%%.', a.id) OR c.path LIKE CONCAT(a.id, '.%%') OR c.path = a.id)\n"
          + "WHERE domain_id = 'SURVEY'\n"
          + "AND type = 'PPI'\n"
          + "AND subtype = 'ANSWER')";
  public static final String QUESTION_LOOKUP_SQL =
      "(SELECT DISTINCT concept_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "JOIN (select cast(cr.id as string) as id\n"
          + "FROM `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "WHERE concept_id IN (@surveyConceptIds)\n"
          + "AND domain_id = 'SURVEY') a\n"
          + "ON (c.path like CONCAT('%', a.id, '.%'))\n"
          + "WHERE domain_id = 'SURVEY'\n"
          + "AND type = 'PPI'\n"
          + "AND subtype = 'QUESTION')";
  private static final String PARENT_STANDARD_OR_SOURCE_SQL =
      CONCEPT_ID_IN_SQL + CHILD_LOOKUP_SQL + AND + STANDARD_SQL + "\n";
  private static final String DRUG_SQL =
      CONCEPT_ID_IN_SQL + DRUG_CHILD_LOOKUP_SQL + AND + STANDARD_SQL;
  private static final String PFHH_QUESTION_SQL =
      VALUE_SOURCE_CONCEPT_ID_IN_SQL + ANSWER_LOOKUP_SQL;
  private static final String VALUE_AS_NUMBER = " value_as_number %s %s";
  private static final String VALUE_AS_NUMBER_IS_NOT_NULL = " AND value_as_number IS NOT NULL";
  private static final String VALUE_AS_CONCEPT_ID = " value_as_concept_id %s unnest(%s)";
  private static final String VALUE_SOURCE_CONCEPT_ID = " value_source_concept_id %s unnest(%s)";
  private static final String SOURCE_CONCEPT_SURVEY_ID =
      " AND survey_version_concept_id %s unnest(%s)";
  private static final String SYSTOLIC_SQL = " AND systolic %s %s";
  private static final String DIASTOLIC_SQL = " AND diastolic %s %s";

  // sql parts to help construct Temporal BigQuery sql
  private static final String SAME_ENC =
      "temp1.person_id = temp2.person_id AND temp1.visit_occurrence_id = temp2.visit_occurrence_id\n";
  private static final String X_DAYS_BEFORE =
      "temp1.person_id = temp2.person_id AND temp1.entry_date <= DATE_SUB(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String X_DAYS_AFTER =
      "temp1.person_id = temp2.person_id AND temp1."
          + "entry_date >= DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String WITHIN_X_DAYS_OF =
      "temp1.person_id = temp2.person_id AND temp1.entry_date between "
          + "DATE_SUB(temp2.entry_date, INTERVAL %s DAY) and DATE_ADD(temp2.entry_date, INTERVAL %s DAY)\n";
  private static final String TEMPORAL_EXIST =
      "SELECT temp1.person_id\n"
          + "FROM (%s) temp1\n"
          + "WHERE EXISTS (SELECT 1\n"
          + "FROM (%s) temp2\n"
          + "WHERE (%s))\n";
  private static final String TEMPORAL_JOIN =
      "SELECT temp1.person_id\n"
          + "FROM (%s) temp1\n"
          + "JOIN (SELECT person_id, visit_occurrence_id, entry_date\n"
          + "FROM (%s)\n"
          + ") temp2 on (%s)\n";
  private static final String TEMPORAL_SQL =
      "SELECT person_id, visit_occurrence_id, entry_date%s\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_all_events`\n"
          + "WHERE %s\n"
          + "AND person_id IN (%s)\n";
  private static final String RANK_1_SQL =
      ", RANK() OVER (PARTITION BY person_id ORDER BY entry_date%s) rn";
  private static final String TEMPORAL_RANK_1_SQL =
      "SELECT person_id, visit_occurrence_id, entry_date\n" + "FROM (%s) a\n" + "WHERE rn = 1\n";

  // sql parts to help construct Modifiers BigQuery sql
  private static final String MODIFIER_SQL_TEMPLATE =
      "SELECT criteria.person_id FROM (%s) criteria\n";
  private static final String OCCURRENCES_SQL_TEMPLATE =
      "GROUP BY criteria.person_id, criteria.concept_id\n" + "HAVING COUNT(criteria.person_id) ";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE = " AND age_at_event ";
  private static final String EVENT_DATE_SQL_TEMPLATE = " AND entry_date ";
  private static final String ENCOUNTERS_SQL_TEMPLATE = " AND visit_concept_id ";

  private static final String CATI_FLAG_SQL_TEMPLATE = " AND cati_concept_id ";

  // sql parts to help construct demographic BigQuery sql
  private static final String DEC_SQL =
      "EXISTS (\n"
          + "SELECT 'x' FROM `${projectId}.${dataSetId}.death` d\n"
          + "WHERE d.person_id = p.person_id)\n";
  private static final String DEMO_BASE =
      "SELECT person_id\n" + "FROM `${projectId}.${dataSetId}.person` p\nWHERE\n";
  private static final String AGE_SQL =
      "SELECT person_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_person` p\nWHERE %s %s %s\n";
  private static final String AGE_DEC_SQL = "AND NOT " + DEC_SQL;
  private static final String DEMO_IN_SQL = "%s IN unnest(%s)\n";
  private static final String HAS_DATA_SQL =
      "SELECT person_id\n" + "FROM `${projectId}.${dataSetId}.cb_search_person` p\nWHERE %s = 1\n";
  private static final String CB_SEARCH_ALL_EVENTS_WHERE =
      "SELECT concept_id FROM `${projectId}.${dataSetId}.cb_search_all_events`\nWHERE ";
  private static final String CB_SEARCH_ALL_EVENTS_PERSON_ID_WHERE =
      "SELECT person_id FROM `${projectId}.${dataSetId}.cb_search_all_events`\nWHERE ";
  private static final String PERSON_ID_IN = "person_id IN (";
  private static final String VARIANT_SQL =
      "SELECT person_id\n"
          + "FROM `${projectId}.${dataSetId}.cb_variant_to_person`\n"
          + "CROSS JOIN UNNEST(person_ids) AS person_id\n"
          + "WHERE vid IN UNNEST(%s)";

  /** Build the innermost sql using search parameters, modifiers and attributes. */
  public static void buildQuery(
      Map<String, QueryParameterValue> queryParams,
      List<String> queryParts,
      SearchGroup searchGroup) {
    if (searchGroup.isTemporal()) {
      // build the outer temporal sql statement
      String query = buildOuterTemporalQuery(queryParams, searchGroup);
      queryParts.add(query);
    } else {
      if (CollectionUtils.isEmpty(searchGroup.getItems())) {
        throw new BadRequestException(
            "SearchGroup Id: " + searchGroup.getId() + " has null/empty items list");
      }

      for (SearchGroupItem searchGroupItem : searchGroup.getItems()) {
        // build regular sql statement
        String query = buildBaseQuery(queryParams, searchGroupItem, searchGroup.getMention());
        queryParts.add(query);
      }
    }
  }

  /** Build the innermost sql */
  private static String buildBaseQuery(
      Map<String, QueryParameterValue> queryParams,
      SearchGroupItem searchGroupItem,
      TemporalMention mention) {
    Set<SearchParameter> standardSearchParameters = new HashSet<>();
    Set<SearchParameter> sourceSearchParameters = new HashSet<>();
    Set<SearchParameter> pfhhQuestionSearchParameters = new HashSet<>();
    Set<SearchParameter> pfhhAnswerSearchParameters = new HashSet<>();
    List<String> queryParts = new ArrayList<>();

    if (CollectionUtils.isEmpty(searchGroupItem.getSearchParameters())) {
      throw new BadRequestException(
          "SearchGroupItem Id: "
              + searchGroupItem.getId()
              + " has null/empty search parameter list");
    }

    Domain domain = Domain.fromValue(searchGroupItem.getType());

    // When building sql for SNP Indel Variant
    if (Domain.SNP_INDEL_VARIANT.equals(domain)) {
      return buildVariantSql(queryParams, searchGroupItem);
    }
    // When building sql for demographics - we query against the person table
    if (Domain.PERSON.equals(domain)) {
      return buildDemoSql(queryParams, searchGroupItem);
    }
    if (hasDataDomains(searchGroupItem)) {
      return String.format(HAS_DATA_SQL, HAS_DATA_DOMAINS.get(domain));
    }
    // Otherwise build sql against flat denormalized search table
    for (SearchParameter param : searchGroupItem.getSearchParameters()) {
      if (param.getAttributes().isEmpty()) {
        if (param.isStandard()) {
          standardSearchParameters.add(param);
        } else {
          sourceSearchParameters.add(param);
        }
      } else {
        // All PFHH survey search parameters will have a PFHH attribute
        // PFHH parameters only search with answer concept ids
        // If we get questions or survey we have to generate SQL that will look up all the answers
        if (param.getAttributes().contains(getPFHHAttribute())) {
          if (param.getSubtype().equals(CriteriaSubType.ANSWER.toString())) {
            pfhhAnswerSearchParameters.add(param);
          } else {
            pfhhQuestionSearchParameters.add(param);
          }
        } else {
          // handle non PFHH parameters that have attributes
          queryParts.add(processAttributeSql(queryParams, param));
        }
      }
    }
    assert domain != null;
    addParamValueAndFormat(
        domain.toString(), queryParams, standardSearchParameters, queryParts, STANDARD);
    addParamValueAndFormat(
        domain.toString(), queryParams, sourceSearchParameters, queryParts, SOURCE);
    addParamValuePFHHAndFormat(queryParams, pfhhQuestionSearchParameters, queryParts);
    addParamValuePFHHAndFormat(queryParams, pfhhAnswerSearchParameters, queryParts);

    String queryPartsSql;
    if ((SOURCE_STANDARD_DOMAINS.contains(domain)
        && !sourceSearchParameters.isEmpty()
        && !standardSearchParameters.isEmpty())) {
      // Using an OR is inefficient when dealing with source and standard concepts together
      // To combat this we do a UNION ALL between source and standard concepts
      queryPartsSql =
          CONCEPT_ID_IN_SQL
              + " ("
              + CB_SEARCH_ALL_EVENTS_WHERE
              + String.join(UNION_TEMPLATE + CB_SEARCH_ALL_EVENTS_WHERE, queryParts)
              + ")";
    } else if (Domain.SURVEY.equals(domain)) {
      // Using an OR between survey/question/answer query parts is really inefficient(10-12secs
      // slower)
      // To combat this we do a UNION ALL between survey/question/answer query parts but this means
      // we have to adapt when adding the potential modifiers(age at event or cati) and return early
      // in this method.
      String ageCatiModifierSql =
          getAgeDateAndEncounterSql(queryParams, searchGroupItem.getModifiers());
      queryPartsSql =
          PERSON_ID_IN
              + CB_SEARCH_ALL_EVENTS_PERSON_ID_WHERE
              + String.join(
                  ageCatiModifierSql + UNION_TEMPLATE + CB_SEARCH_ALL_EVENTS_PERSON_ID_WHERE,
                  queryParts)
              + ageCatiModifierSql
              + ")";
      return CB_SEARCH_ALL_EVENTS_PERSON_ID_WHERE + queryPartsSql;
    } else {
      // besides the special cases above, it's ok to OR query parts together
      queryPartsSql = "(" + String.join(OR + "\n", queryParts) + ")";
    }
    // format the base sql with all query parts
    String baseSql = BASE_SQL + queryPartsSql;
    // build modifier sql if modifiers exists
    String modifiedSql = buildModifierSql(baseSql, queryParams, searchGroupItem.getModifiers());
    // build the inner temporal sql if this search group item is temporal
    // otherwise return modifiedSql
    return buildInnerTemporalQuery(
        modifiedSql, queryPartsSql, queryParams, searchGroupItem.getModifiers(), mention);
  }

  /** Build sql statement for demographics */
  private static String buildDemoSql(
      Map<String, QueryParameterValue> queryParams, SearchGroupItem searchGroupItem) {
    List<SearchParameter> parameters = searchGroupItem.getSearchParameters();
    SearchParameter param = parameters.get(0);
    switch (CriteriaType.valueOf(param.getType())) {
      case AGE:
        List<String> queryParts = new ArrayList<>();
        parameters.forEach(
            searchParameter -> {
              Attribute attribute = searchParameter.getAttributes().get(0);
              String ageNamedParameter =
                  QueryParameterUtil.addQueryParameterValue(
                      queryParams,
                      QueryParameterValue.int64(Long.valueOf(attribute.getOperands().get(0))));
              if (attribute.getOperands().size() > 1) {
                String ageNamedParameter1 =
                    QueryParameterUtil.addQueryParameterValue(
                        queryParams,
                        QueryParameterValue.int64(Long.valueOf(attribute.getOperands().get(1))));
                ageNamedParameter = ageNamedParameter + AND + ageNamedParameter1;
              }
              String ageSql =
                  String.format(
                      AGE_SQL,
                      AGE_COLUMN_SQL_MAP.get(attribute.getName()),
                      OperatorUtils.getSqlOperator(attribute.getOperator()),
                      ageNamedParameter);
              queryParts.add(
                  AttrName.AGE_AT_CONSENT.equals(attribute.getName())
                      ? ageSql
                      : ageSql + AGE_DEC_SQL);
            });
        return String.join(UNION_TEMPLATE, queryParts);
      case GENDER:
      case SEX:
      case ETHNICITY:
      case RACE:
        // Gender, Sex, Ethnicity and Race all share the same implementation
        Long[] conceptIds =
            searchGroupItem.getSearchParameters().stream()
                .map(SearchParameter::getConceptId)
                .toArray(Long[]::new);
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.array(conceptIds, Long.class));

        CriteriaType criteriaType = CriteriaType.fromValue(param.getType());
        return DEMO_BASE
            + String.format(DEMO_IN_SQL, DEMO_COLUMN_SQL_MAP.get(criteriaType), namedParameter);
      case DECEASED:
        return DEMO_BASE + DEC_SQL;
      default:
        throw new BadRequestException(
            "Search unsupported for demographics type " + param.getType());
    }
  }

  /** Build sql statement for SNP Indel Variants */
  private static String buildVariantSql(
      Map<String, QueryParameterValue> queryParams, SearchGroupItem searchGroupItem) {
    String[] variantIds =
        searchGroupItem.getSearchParameters().stream()
            .map(SearchParameter::getVariantId)
            .toArray(String[]::new);
    String namedParameter =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.array(variantIds, String.class));
    return String.format(VARIANT_SQL, namedParameter);
  }

  /**
   * Implementation of temporal CB queries. Please reference the following google doc for details:
   * <a href="https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8">CB
   * Temporal Search Groups</a>
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
    conditionsSql = conditionsSql + getAgeDateAndEncounterSql(queryParams, modifiers);
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
   * The temporal group functionality description is here: <a
   * href="https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8">CB
   * Temporal Search Groups</a>
   */
  private static String buildOuterTemporalQuery(
      Map<String, QueryParameterValue> params, SearchGroup searchGroup) {
    List<String> temporalQueryParts1 = new ArrayList<>();
    List<String> temporalQueryParts2 = new ArrayList<>();
    ListMultimap<Integer, SearchGroupItem> temporalGroups = getTemporalGroups(searchGroup);
    for (Integer key : temporalGroups.keySet()) {
      List<SearchGroupItem> tempGroups = temporalGroups.get(key);
      // key of zero indicates belonging to the first temporal group
      // key of one indicates belonging to the second temporal group
      boolean isFirstGroup = key == 0;
      for (SearchGroupItem tempGroup : tempGroups) {
        String query = buildBaseQuery(params, tempGroup, searchGroup.getMention());
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
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(WITHIN_X_DAYS_OF, parameterName, parameterName);
    } else if (TemporalTime.X_DAYS_BEFORE.equals(searchGroup.getTime())) {
      String parameterName =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(searchGroup.getTimeValue()));
      conditions = String.format(X_DAYS_BEFORE, parameterName);
    } else if (TemporalTime.X_DAYS_AFTER.equals(searchGroup.getTime())) {
      String parameterName =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(searchGroup.getTimeValue()));
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
                      "Bad Request: search group item temporal group {0} is not valid.",
                      item.getTemporalGroup());
              itemMap.put(item.getTemporalGroup(), item);
            });
    from(notZeroAndNotOne())
        .test(itemMap)
        .throwException(
            "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).");
    return itemMap;
  }

  /** Helper method to build blood pressure sql. */
  private static String processBloodPressureSql(
      Map<String, QueryParameterValue> queryParams, List<Attribute> attributes) {
    StringBuilder sqlBuilder = new StringBuilder();
    for (Attribute attribute : attributes) {
      if (!AttrName.ANY.equals(attribute.getName())) {
        // this makes an assumption that the UI adds systolic attribute first. Otherwise, we will
        // have to hard code the conceptId which is not optimal.
        String sqlTemplate =
            sqlBuilder.toString().contains("systolic") ? DIASTOLIC_SQL : SYSTOLIC_SQL;
        sqlBuilder.append(
            String.format(
                sqlTemplate,
                OperatorUtils.getSqlOperator(attribute.getOperator()),
                getOperandsExpression(queryParams, attribute)));
      }
    }
    return sqlBuilder.toString();
  }

  private static String processAttributeSql(
      Map<String, QueryParameterValue> queryParams, SearchParameter parameter) {
    parameter.getAttributes().forEach(SearchGroupItemQueryBuilder::validateAttribute);
    String catsParam;
    String versionParam;
    List<Long> conceptIds =
        parameter.getAttributes().stream()
            .filter(attr -> attr.getConceptId() != null)
            .map(Attribute::getConceptId)
            .collect(Collectors.toList());
    List<Attribute> cats =
        parameter.getAttributes().stream()
            .filter(attr -> attr.getName().equals(AttrName.CAT))
            .collect(Collectors.toList());
    List<Attribute> nums =
        parameter.getAttributes().stream()
            .filter(attr -> attr.getName().equals(AttrName.NUM))
            .collect(Collectors.toList());
    List<Attribute> any =
        parameter.getAttributes().stream()
            .filter(attr -> attr.getName().equals(AttrName.ANY))
            .collect(Collectors.toList());
    List<Attribute> versions =
        parameter.getAttributes().stream()
            .filter(attr -> attr.getName().equals(AttrName.SURVEY_VERSION_CONCEPT_ID))
            .collect(Collectors.toList());
    String standardParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.int64(parameter.isStandard() ? 1 : 0));
    String conceptIdParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams,
            QueryParameterValue.array(
                conceptIds.isEmpty()
                    ? new Long[] {parameter.getConceptId()}
                    : conceptIds.toArray(new Long[0]),
                Long.class));
    StringBuilder sqlBuilder =
        new StringBuilder(String.format(STANDARD_OR_SOURCE_SQL, conceptIdParam, standardParam));
    if (!nums.isEmpty()) {
      if (!conceptIds.isEmpty()) {
        // attribute.conceptId is unique to blood pressure attributes
        // this indicates we need to build a blood pressure sql statement
        sqlBuilder.append(processBloodPressureSql(queryParams, parameter.getAttributes()));
      } else {
        String parens = cats.isEmpty() ? "" : "(";
        sqlBuilder.append(
            String.format(
                AND + parens + VALUE_AS_NUMBER,
                OperatorUtils.getSqlOperator(nums.get(0).getOperator()),
                getOperandsExpression(queryParams, nums.get(0))));
      }
    }
    if (!cats.isEmpty()) {
      String andOrSql = nums.isEmpty() ? AND : OR;
      String parens = nums.isEmpty() ? "" : ")";
      catsParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams,
              QueryParameterValue.array(
                  cats.get(0).getOperands().stream().map(Long::parseLong).toArray(Long[]::new),
                  Long.class));
      String catsSql =
          Domain.SURVEY.toString().equals(parameter.getDomain())
              ? VALUE_SOURCE_CONCEPT_ID
              : VALUE_AS_CONCEPT_ID;
      sqlBuilder.append(
          String.format(
              andOrSql + catsSql + parens,
              OperatorUtils.getSqlOperator(cats.get(0).getOperator()),
              catsParam));
    }
    if (!versions.isEmpty()) {
      versionParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams,
              QueryParameterValue.array(
                  versions.get(0).getOperands().stream().map(Long::parseLong).toArray(Long[]::new),
                  Long.class));
      sqlBuilder.append(
          String.format(
              SOURCE_CONCEPT_SURVEY_ID,
              OperatorUtils.getSqlOperator(versions.get(0).getOperator()),
              versionParam));
    }
    if (!any.isEmpty() && parameter.getDomain().equals(Domain.SURVEY.toString())) {
      sqlBuilder.append(VALUE_AS_NUMBER_IS_NOT_NULL);
    }

    return sqlBuilder.toString();
  }

  /** Helper method to build the operand sql expression. */
  private static String getOperandsExpression(
      Map<String, QueryParameterValue> queryParams, Attribute attribute) {
    String operandsParam1 =
        QueryParameterUtil.addQueryParameterValue(
            queryParams,
            QueryParameterValue.float64(Double.valueOf(attribute.getOperands().get(0))));
    String valueExpression;
    if (attribute.getOperator().equals(Operator.BETWEEN)) {
      String operandsParam2 =
          QueryParameterUtil.addQueryParameterValue(
              queryParams,
              QueryParameterValue.float64(Double.valueOf(attribute.getOperands().get(1))));
      valueExpression = operandsParam1 + AND + operandsParam2;
    } else {
      valueExpression = operandsParam1;
    }
    return valueExpression;
  }

  /** Helper method to build modifier sql if needed. */
  private static String buildModifierSql(
      String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    validateModifiers(modifiers);
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
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.CATI));
    StringBuilder modifierSql = new StringBuilder();
    for (Modifier modifier : ageDateAndEncounterModifiers) {
      if (modifier == null) {
        continue;
      }
      List<String> modifierParamList = new ArrayList<>();
      for (String operand : modifier.getOperands()) {
        String modifierParameter =
            QueryParameterUtil.addQueryParameterValue(
                queryParams,
                (isAgeAtEvent(modifier) || isEncounters(modifier) || isCati(modifier))
                    ? QueryParameterValue.int64(Long.valueOf(operand))
                    : QueryParameterValue.date(operand));
        modifierParamList.add(modifierParameter);
      }
      if (isAgeAtEvent(modifier)) {
        modifierSql.append(AGE_AT_EVENT_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" ")
            .append(String.join(AND, modifierParamList))
            .append("\n");
      } else if (isEncounters(modifier)) {
        modifierSql.append(ENCOUNTERS_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" (")
            .append(modifierParamList.get(0))
            .append(")\n");
      } else if (isCati(modifier)) {
        modifierSql.append(CATI_FLAG_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" (")
            .append(modifierParamList.get(0))
            .append(")\n");
      } else {
        modifierSql.append(EVENT_DATE_SQL_TEMPLATE);
        modifierSql
            .append(OperatorUtils.getSqlOperator(modifier.getOperator()))
            .append(" ")
            .append(String.join(AND, modifierParamList))
            .append("\n");
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
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.int64(Long.valueOf(operand)));
        modifierParamList.add(modifierParameter);
      }
      modifierSql
          .append(OCCURRENCES_SQL_TEMPLATE)
          .append(OperatorUtils.getSqlOperator(occurrences.getOperator()))
          .append(" ")
          .append(String.join(AND, modifierParamList))
          .append("\n");
    }
    return modifierSql.toString();
  }

  /** Add source or standard concept ids and set params * */
  private static void addParamValueAndFormat(
      String domain,
      Map<String, QueryParameterValue> queryParams,
      Set<SearchParameter> searchParameters,
      List<String> queryParts,
      int standardOrSource) {
    if (!searchParameters.isEmpty()) {
      String standardOrSourceParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams, QueryParameterValue.int64(standardOrSource));

      Map<Boolean, List<SearchParameter>> parentsAndChildren =
          searchParameters.stream().collect(Collectors.partitioningBy(SearchParameter::isGroup));
      List<Long> parents =
          parentsAndChildren.get(true).stream()
              .map(SearchParameter::getConceptId)
              .collect(Collectors.toList());

      String conceptIdsParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams,
              QueryParameterValue.array(
                  searchParameters.stream().map(SearchParameter::getConceptId).toArray(Long[]::new),
                  Long.class));
      if (!parents.isEmpty() || Domain.DRUG.toString().equals(domain)) {
        // Lookup child nodes
        queryParts.add(
            String.format(
                Domain.DRUG.toString().equals(domain) ? DRUG_SQL : PARENT_STANDARD_OR_SOURCE_SQL,
                conceptIdsParam,
                standardOrSourceParam,
                standardOrSourceParam));
      } else {
        // Children only
        queryParts.add(
            String.format(STANDARD_OR_SOURCE_SQL, conceptIdsParam, standardOrSourceParam));
      }
    }
  }

  /** Add source or standard concept ids and set params * */
  private static void addParamValuePFHHAndFormat(
      Map<String, QueryParameterValue> queryParams,
      Set<SearchParameter> searchParameters,
      List<String> queryParts) {
    if (!searchParameters.isEmpty()) {
      String subtype = searchParameters.stream().findFirst().get().getSubtype();
      if (subtype.equals(CriteriaSubType.ANSWER.toString())) {
        Long[] conceptIds =
            searchParameters.stream()
                .flatMap(param -> param.getAttributes().stream())
                .flatMap(attribute -> attribute.getOperands().stream())
                .filter(conceptId -> !conceptId.equals("1740639"))
                .map(Long::valueOf)
                .toArray(Long[]::new);
        String conceptIdsParam =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.array(conceptIds, Long.class));
        queryParts.add(String.format(VALUE_SOURCE_CONCEPT_ID, "IN", conceptIdsParam));
      } else {
        Long[] conceptIds =
            searchParameters.stream().map(SearchParameter::getConceptId).toArray(Long[]::new);
        String conceptIdsParam =
            QueryParameterUtil.addQueryParameterValue(
                queryParams, QueryParameterValue.array(conceptIds, Long.class));
        queryParts.add(String.format(PFHH_QUESTION_SQL, conceptIdsParam));
      }
    }
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
    return modifierList.get(0);
  }

  private static boolean isAgeAtEvent(Modifier modifier) {
    return modifier.getName().equals(ModifierType.AGE_AT_EVENT);
  }

  private static boolean isEncounters(Modifier modifier) {
    return modifier.getName().equals(ModifierType.ENCOUNTERS);
  }

  private static boolean isCati(Modifier modifier) {
    return modifier.getName().equals(ModifierType.CATI);
  }

  /** Validate attributes */
  private static void validateAttribute(Attribute attr) {
    if (!AttrName.ANY.equals(attr.getName())) {
      from(operatorNull())
          .test(attr)
          .throwException("Bad Request: attribute operator {0} is not valid.", attr.getOperator());
      from(operandsEmpty()).test(attr).throwException("Bad Request: attribute operands are empty.");
      from(notBetweenAndNotInOperator().and(operandsNotOne()))
          .test(attr)
          .throwException(
              "Bad Request: attribute {0} must have one operand when using the {1} operator.",
              attr.getName().toString(), attr.getOperator().toString());
      from(betweenOperator().and(operandsNotTwo()))
          .test(attr)
          .throwException(
              "Bad Request: attribute {0} can only have 2 operands when using the {1} operator",
              attr.getName().toString(), attr.getOperator().toString());
      from(operandsNotNumbers())
          .test(attr)
          .throwException(
              "Bad Request: attribute {0} operands must be numeric.", attr.getName().toString());
    }
  }

  private static void validateModifiers(List<Modifier> modifiers) {
    modifiers.forEach(
        modifier -> {
          from(operatorNull())
              .test(modifier)
              .throwException(
                  "Bad Request: modifier operator {0} is not valid.", modifier.getOperator());
          from(operandsEmpty())
              .test(modifier)
              .throwException("Bad Request: modifier operands are empty.");
          from(notBetweenAndNotInOperator().and(operandsNotOne()))
              .test(modifier)
              .throwException(
                  "Bad Request: modifier {0} must have one operand when using the {1} operator.",
                  modifier.getName().toString(), modifier.getOperator().toString());
          from(betweenOperator().and(operandsNotTwo()))
              .test(modifier)
              .throwException(
                  "Bad Request: modifier {0} can only have 2 operands when using the {1} operator",
                  modifier.getName().toString(), modifier.getOperator().toString());
          if (ModifierType.EVENT_DATE.equals(modifier.getName())) {
            from(operandsNotDates())
                .test(modifier)
                .throwException(
                    "Bad Request: modifier {0} must be a valid date.",
                    modifier.getName().toString());
          } else {
            from(operandsNotNumbers())
                .test(modifier)
                .throwException(
                    "Bad Request: modifier {0} operands must be numeric.",
                    modifier.getName().toString());
          }
        });
  }

  private static boolean hasDataDomains(SearchGroupItem searchGroupItem) {
    Domain domain = Domain.fromValue(searchGroupItem.getType());
    return (HAS_DATA_DOMAINS.containsKey(domain) && !Domain.PHYSICAL_MEASUREMENT.equals(domain))
        || (searchGroupItem.getSearchParameters().size() == 1
            && searchGroupItem.getSearchParameters().stream()
                .allMatch(
                    sp ->
                        Domain.PHYSICAL_MEASUREMENT.toString().equals(sp.getDomain())
                            && sp.getConceptId() == null
                            && sp.getAttributes().isEmpty()));
  }

  private static Attribute getPFHHAttribute() {
    return new Attribute()
        .name(AttrName.PERSONAL_FAMILY_HEALTH_HISTORY)
        .operator(Operator.IN)
        .operands(ImmutableList.of("1740639"));
  }
}
