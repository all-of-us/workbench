package org.pmiops.workbench.cohortbuilder.querybuilder;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.betweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.notBetweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.notOneModifier;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operandsEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operandsNotDates;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operandsNotOne;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operandsNotTwo;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operatorNotIn;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ModifierPredicates.operatorNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.DATE_MODIFIER_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.MODIFIER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_IN_MODIFIER_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ONE_MODIFIER_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ONE_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS_NUMERIC_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERATOR;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TWO_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.modifierText;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.operatorText;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.utils.OperatorUtils;

/**
 * AbstractQueryBuilder is a superclass that all query builders extend. This abstract class is used
 * to state or define general characteristics about the CB query builders. It provides
 * implementations of the modifier and temporal queries used by the CB. The extending class just
 * needs to call the modifier/temporal methods with proper args to get generated query for
 * specified.
 */
// TODO:Remove when new search is finished - freemabd
public abstract class AbstractQueryBuilder {

  public static final String TABLE_ID = "search_all_domains";
  public static final String PARENT = "parent";
  public static final String CHILD = "child";
  public static final String AND = " and ";
  public static final String OR = " or\n";
  public static final String AGE_DATE_AND_ENCOUNTER_VAR = "${ageDateAndEncounterSql}";
  private static final String MODIFIER_SQL_TEMPLATE =
      "select criteria.person_id from (${innerSql}) criteria\n";
  private static final String DESC = " desc";
  private static final String RANK_1_SQL_TEMPLATE =
      ", rank() over (partition by person_id order by entry_date${descSql}) rn";
  private static final String TEMPORAL_SQL_TEMPLATE =
      "select person_id, visit_occurrence_id, entry_date${rank1Sql}\n"
          + "from `${projectId}.${dataSetId}."
          + TABLE_ID
          + "`\n"
          + "where ${conceptIdSql}"
          + "and person_id in (${innerSql})\n";
  private static final String TEMPORAL_RANK_1_SQL_TEMPLATE =
      "select person_id, visit_occurrence_id, entry_date\n"
          + "from (${innerTemporalSql}) a\n"
          + "where rn = 1\n";
  private static final String OCCURRENCES_SQL_TEMPLATE =
      "group by criteria.person_id, criteria.concept_id\n" + "having count(criteria.person_id) ";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE = "and age_at_event ";
  private static final String EVENT_DATE_SQL_TEMPLATE = "and entry_date ";
  private static final String ENCOUNTERS_SQL_TEMPLATE = "and visit_concept_id ";

  /**
   * Build a query from the specified parameters provided.
   *
   * @param queryParams
   * @param searchGroupItem
   * @param temporalMention
   */
  public abstract String buildQuery(
      Map<String, QueryParameterValue> queryParams,
      SearchGroupItem searchGroupItem,
      TemporalMention temporalMention);

  /**
   * The {@link FactoryKey} helps determine which implementation of AbstractQueryBuilder to use:
   *
   * <p>FactoryKey#CODES - {@link CodesQueryBuilder} FactoryKey#DEMO - {@link DemoQueryBuilder}
   * FactoryKey#VISIT - {@link VisitsQueryBuilder} FactoryKey#PM - {@link PMQueryBuilder}
   * FactoryKey#DRUG - {@link DrugQueryBuilder} FactoryKey#MEAS - {@link MeasurementQueryBuilder}
   * FactoryKey#PPI - {@link PPIQueryBuilder}
   */
  public abstract FactoryKey getType();

  /**
   * Implementation of modifier CB queries.
   *
   * @param baseSql
   * @param queryParams
   * @param modifiers
   */
  public String buildModifierSql(
      String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    String ageDateAndEncounterSql = getAgeDateAndEncounterSql(queryParams, modifiers);
    // Number of Occurrences has to be last because of the group by
    String occurrenceSql =
        buildOccurrencesSql(queryParams, getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES));
    return MODIFIER_SQL_TEMPLATE.replace(
            "${innerSql}", baseSql.replace("${ageDateAndEncounterSql}", ageDateAndEncounterSql))
        + occurrenceSql;
  }

  /**
   * Implementation of temporal CB queries. Please reference the following google doc for details:
   * https://docs.google.com/document/d/1OFrG7htm8gT0QOOvzHa7l3C3Qs0JnoENuK1TDAB_1A8/edit#heading=h.zbvt2sup9sys
   *
   * @param innerSql
   * @param conceptIdsSql
   * @param queryParams
   * @param modifiers
   * @param mention
   */
  public String buildTemporalSql(
      String innerSql,
      String conceptIdsSql,
      Map<String, QueryParameterValue> queryParams,
      List<Modifier> modifiers,
      TemporalMention mention) {
    if (mention != null) {
      String temporalSql =
          TEMPORAL_SQL_TEMPLATE
              .replace("${innerSql}", innerSql)
              .replace("${conceptIdSql}", conceptIdsSql)
              .replace(
                  "${ageDateAndEncounterSql}", getAgeDateAndEncounterSql(queryParams, modifiers));
      if (TemporalMention.ANY_MENTION.equals(mention)) {
        return temporalSql.replace("${rank1Sql}", "");
      } else if (TemporalMention.FIRST_MENTION.equals(mention)) {
        temporalSql =
            temporalSql.replace("${rank1Sql}", RANK_1_SQL_TEMPLATE.replace("${descSql}", ""));
        return TEMPORAL_RANK_1_SQL_TEMPLATE.replace("${innerTemporalSql}", temporalSql);
      } else {
        temporalSql =
            temporalSql.replace("${rank1Sql}", RANK_1_SQL_TEMPLATE.replace("${descSql}", DESC));
        return TEMPORAL_RANK_1_SQL_TEMPLATE.replace("${innerTemporalSql}", temporalSql);
      }
    } else {
      return innerSql;
    }
  }

  /**
   * Generate a unique parameter name and add it to the parameter map provided.
   *
   * @param queryParameterValueMap
   * @param queryParameterValue
   * @return
   */
  protected String addQueryParameterValue(
      Map<String, QueryParameterValue> queryParameterValueMap,
      QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return parameterName;
  }

  private String getAgeDateAndEncounterSql(
      Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    List<Modifier> ageDateAndEncounterModifiers = new ArrayList<>();
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.AGE_AT_EVENT));
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.EVENT_DATE));
    ageDateAndEncounterModifiers.add(getModifier(modifiers, ModifierType.ENCOUNTERS));
    return buildAgeDateAndEncounterSql(queryParams, ageDateAndEncounterModifiers);
  }

  private Modifier getModifier(List<Modifier> modifiers, ModifierType modifierType) {
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

  private String buildAgeDateAndEncounterSql(
      Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    StringBuilder modifierSql = new StringBuilder();
    for (Modifier modifier : modifiers) {
      if (modifier != null) {
        validateModifier(modifier);
        List<String> modifierParamList = new ArrayList<>();
        for (String operand : modifier.getOperands()) {
          String modifierParameter =
              addQueryParameterValue(
                  queryParams,
                  (isAgeAtEvent(modifier) || isEncounters(modifier))
                      ? QueryParameterValue.int64(new Long(operand))
                      : QueryParameterValue.date(operand));
          modifierParamList.add("@" + modifierParameter);
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

  private String buildOccurrencesSql(
      Map<String, QueryParameterValue> queryParams, Modifier occurrences) {
    StringBuilder modifierSql = new StringBuilder();
    if (occurrences != null) {
      List<String> modifierParamList = new ArrayList<>();
      validateModifier(occurrences);
      for (String operand : occurrences.getOperands()) {
        String modifierParameter =
            addQueryParameterValue(queryParams, QueryParameterValue.int64(new Long(operand)));
        modifierParamList.add("@" + modifierParameter);
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

  private void validateModifier(Modifier modifier) {
    String name = modifierText.get(modifier.getName());
    String oper = operatorText.get(modifier.getOperator());
    from(operatorNull()).test(modifier).throwException(NOT_VALID_MESSAGE, MODIFIER, OPERATOR, oper);
    from(operandsEmpty()).test(modifier).throwException(EMPTY_MESSAGE, OPERANDS);
    from(betweenOperator().and(operandsNotTwo()))
        .test(modifier)
        .throwException(TWO_OPERAND_MESSAGE, MODIFIER, name, oper);
    from(notBetweenOperator().and(operandsNotOne()))
        .test(modifier)
        .throwException(ONE_OPERAND_MESSAGE, MODIFIER, name, oper);
    if (ModifierType.AGE_AT_EVENT.equals(modifier.getName())
        || ModifierType.NUM_OF_OCCURRENCES.equals(modifier.getName())) {
      from(operandsNotNumbers())
          .test(modifier)
          .throwException(OPERANDS_NUMERIC_MESSAGE, MODIFIER, name);
    }
    if (ModifierType.EVENT_DATE.equals(modifier.getName())) {
      from(operandsNotDates()).test(modifier).throwException(DATE_MODIFIER_MESSAGE, MODIFIER, name);
    }
    if (ModifierType.ENCOUNTERS.equals(modifier.getName())) {
      from(operatorNotIn())
          .test(modifier)
          .throwException(NOT_IN_MODIFIER_MESSAGE, name, operatorText.get(Operator.IN));
      from(operandsNotNumbers())
          .test(modifier)
          .throwException(OPERANDS_NUMERIC_MESSAGE, MODIFIER, name);
    }
  }

  private boolean isAgeAtEvent(Modifier modifier) {
    return modifier.getName().equals(ModifierType.AGE_AT_EVENT);
  }

  private boolean isEncounters(Modifier modifier) {
    return modifier.getName().equals(ModifierType.ENCOUNTERS);
  }
}
