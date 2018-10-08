package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
/**
 * AbstractQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

  public static final String WHERE = " where ";
  public static final String AND = " and ";
  private static final String MODIFIER_SQL_TEMPLATE = "select criteria.person_id from (${innerSql}) criteria\n";
  private static final String AGE_AT_EVENT_JOIN_TEMPLATE =
    "join `${projectId}.${dataSetId}.person` p on (criteria.person_id = p.person_id)\n";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE =
    "CAST(FLOOR(DATE_DIFF(criteria.entry_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64)\n";
  private static final String EVENT_DATE_SQL_TEMPLATE = "criteria.entry_date\n";
  private static final String OCCURRENCES_SQL_TEMPLATE = "group by criteria.person_id\n" +
    "having count(criteria.person_id) ";
  private static final String ENCOUNTERS_SQL_TEMPLATE = "and visit_occurrence_id in (\n" +
    "select visit_occurrence_id from `${projectId}.${dataSetId}.visit_occurrence`\n" +
    "where visit_concept_id in (\n" +
    "select descendant_concept_id\n" +
    "from `${projectId}.${dataSetId}.concept_ancestor`\n" +
    "where ancestor_concept_id ${encounterOperator} unnest(${encounterConceptId})))\n";

  /**
   * Build a {@link QueryJobConfiguration} from the specified
   * {@link QueryParameters} provided.
   *
   * @param parameters
   * @return
   */
  public abstract String buildQuery(Map<String, QueryParameterValue> queryParams, QueryParameters parameters);

  public abstract FactoryKey getType();

  public String buildModifierSql(String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    List<Modifier> ageAndEventModifiers = new ArrayList<>();
    ageAndEventModifiers.add(getModifier(modifiers, ModifierType.AGE_AT_EVENT));
    ageAndEventModifiers.add(getModifier(modifiers, ModifierType.EVENT_DATE));
    String encounterSql = buildEncountersSql(queryParams, getModifier(modifiers, ModifierType.ENCOUNTERS));
    String modifierSql = buildAgeAndEventSql(queryParams, ageAndEventModifiers);
    //Number of Occurrences has to be last because of the group by
    modifierSql = modifierSql + buildOccurrencesSql(queryParams, getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES));
    return MODIFIER_SQL_TEMPLATE
      .replace("${innerSql}", baseSql)
      .replace("${encounterSql}", encounterSql) + modifierSql;
  }

  protected String addQueryParameterValue(Map<String, QueryParameterValue> queryParameterValueMap,
                                          QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return parameterName;
  }

  private Modifier getModifier(List<Modifier> modifiers, ModifierType modifierType) {
    List<Modifier> modifierList = modifiers
      .stream()
      .filter(modifier -> modifier.getName().equals(modifierType))
      .collect(Collectors.toList());
    if (modifierList.isEmpty()) {
      return null;
    }
    from(notOneModifier()).test(modifierList).throwException(ONE_MODIFIER_MESSAGE, modifierText.get(modifierType));
    return modifierList.get(0);
  }

  private String buildAgeAndEventSql(Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers) {
    String modifierSql = "";
    for (Modifier modifier : modifiers) {
      if (modifier != null) {
        validateModifier(modifier);
        boolean isAgeAtEvent = modifier.getName().equals(ModifierType.AGE_AT_EVENT);
        List<String> modifierParamList = new ArrayList<>();
        for (String operand : modifier.getOperands()) {
          String modifierParameter = addQueryParameterValue(queryParams, isAgeAtEvent ?
              QueryParameterValue.int64(new Long(operand)) : QueryParameterValue.date(operand));
          modifierParamList.add("@" + modifierParameter);
        }
        if (isAgeAtEvent) {
          if (modifierSql.isEmpty()) {
            modifierSql = AGE_AT_EVENT_JOIN_TEMPLATE + WHERE + AGE_AT_EVENT_SQL_TEMPLATE;
          } else {
            modifierSql = modifierSql + AND + AGE_AT_EVENT_SQL_TEMPLATE;
          }
        } else {
          if (modifierSql.isEmpty()) {
            modifierSql = WHERE + EVENT_DATE_SQL_TEMPLATE;
          } else {
            modifierSql = modifierSql + AND + EVENT_DATE_SQL_TEMPLATE;
          }
        }
        modifierSql = modifierSql +
          OperatorUtils.getSqlOperator(modifier.getOperator()) + " " +
          String.join(AND, modifierParamList) + "\n";
      }
    }
    return modifierSql;
  }

  private  String buildOccurrencesSql(Map<String, QueryParameterValue> queryParams, Modifier occurrences) {
    String modifierSql = "";
    if (occurrences != null) {
      List<String> modifierParamList = new ArrayList<>();
      validateModifier(occurrences);
      for (String operand : occurrences.getOperands()) {
        String modifierParameter = addQueryParameterValue(queryParams, QueryParameterValue.int64(new Long(operand)));
        modifierParamList.add("@" + modifierParameter);
      }
      modifierSql = OCCURRENCES_SQL_TEMPLATE +
        OperatorUtils.getSqlOperator(occurrences.getOperator()) + " " +
        String.join(AND, modifierParamList) + "\n";
    }
    return modifierSql;
  }

  private String buildEncountersSql(Map<String, QueryParameterValue> queryParams, Modifier modifier) {
    if (modifier == null) {
      return "";
    }
    validateEncounctersModifier(modifier);
    Long[] operands = modifier.getOperands().stream().map(Long::new).toArray(Long[]::new);
    String modifierParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(operands, Long.class));
    return ENCOUNTERS_SQL_TEMPLATE
      .replace("${encounterOperator}", OperatorUtils.getSqlOperator(modifier.getOperator()))
      .replace( "${encounterConceptId}", "@" + modifierParameter);
  }

  private void validateModifier(Modifier modifier) {
    String name = modifierText.get(modifier.getName());
    String oper = operatorText.get(modifier.getOperator());
    from(operatorNull()).test(modifier).throwException(NOT_VALID_MESSAGE, MODIFIER, OPERATOR, oper);
    from(operandsEmpty()).test(modifier).throwException(EMPTY_MESSAGE, OPERANDS);
    from(betweenOperator().and(operandsNotTwo())).test(modifier).throwException(TWO_OPERAND_MESSAGE, MODIFIER, name, oper);
    from(notBetweenOperator().and(operandsNotOne())).test(modifier).throwException(ONE_OPERAND_MESSAGE, MODIFIER, name, oper);
    if (ModifierType.AGE_AT_EVENT.equals(modifier.getName()) || ModifierType.NUM_OF_OCCURRENCES.equals(modifier.getName())) {
      from(operandsNotNumbers()).test(modifier).throwException(OPERANDS_NUMERIC_MESSAGE, MODIFIER, name);
    }
    if (ModifierType.EVENT_DATE.equals(modifier.getName())) {
      from(operandsNotDates()).test(modifier).throwException(DATE_MODIFIER_MESSAGE, MODIFIER, name);
    }
  }

  private void validateEncounctersModifier(Modifier modifier) {
    String name = modifierText.get(modifier.getName());
    String oper = operatorText.get(Operator.IN);
    from(operatorNotIn()).test(modifier).throwException(NOT_IN_MODIFIER_MESSAGE, name, oper);
    from(operandsNotNumbers()).test(modifier).throwException(OPERANDS_NUMERIC_MESSAGE, MODIFIER, name);
  }

}
