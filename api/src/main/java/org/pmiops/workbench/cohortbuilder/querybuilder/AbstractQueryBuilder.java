package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.ModifierPredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.Validation.from;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

  public static final String EMPTY_MESSAGE = "Bad Request: Search {0} are empty.";
  public static final String NOT_VALID_MESSAGE = "Bad Request: {0} {1} \"{2}\" is not valid.";
  public static final String ONE_OPERAND_MESSAGE = "Bad Request: {0} {1} must have one operand when using the {2} operator.";
  public static final String TWO_OPERAND_MESSAGE = "Bad Request: {0} {1} can only have 2 operands when using the {2} operator";
  public static final String OPERANDS_NUMERIC_MESSAGE = "Bad Request: {0} {1} operands must be numeric.";
  public static final String ONE_MODIFIER_MESSAGE = "Bad Request: Please provide one {0} modifier.";
  public static final String DATE_MODIFIER_MESSAGE = "Bad Request: {0} {1} must be a valid date.";
  public static final String NOT_IN_MODIFIER_MESSAGE = "Bad Request: Modifier {0} must provide {1} operator.";
  public static final String PARAMETERS = "Parameters";
  public static final String ATTRIBUTES = "Attributes";
  public static final String PARAMETER = "Search Parameter";
  public static final String ATTRIBUTE = "Attribute";
  public static final String MODIFIER = "Modifier";
  public static final String OPERANDS = "Operands";
  public static final String OPERATOR = "Operator";
  public static final String TYPE = "Type";
  public static final String SUBTYPE = "Subtype";
  public static final String DOMAIN = "Domain";
  public static final String CONCEPT_ID = "Concept Id";
  public static final String CODE = "Code";
  public static final String NAME = "Name";
  public static final String VALUE = "Value";

  public static ImmutableMap<ModifierType, String> modifierText = ImmutableMap.<ModifierType, String>builder()
    .put(ModifierType.AGE_AT_EVENT, "Age at Event")
    .put(ModifierType.EVENT_DATE, "Event Date")
    .put(ModifierType.NUM_OF_OCCURRENCES, "Number of Occurrences")
    .put(ModifierType.ENCOUNTERS, "Visit Type")
    .build();

  public static ImmutableMap<Operator, String> operatorText = ImmutableMap.<Operator, String>builder()
    .put(Operator.EQUAL, "Equal")
    .put(Operator.NOT_EQUAL, "Not Equal")
    .put(Operator.LESS_THAN, "Less Than")
    .put(Operator.GREATER_THAN, "Greater Than")
    .put(Operator.LESS_THAN_OR_EQUAL_TO, "Less Than Or Equal To")
    .put(Operator.GREATER_THAN_OR_EQUAL_TO, "Greater Than Or Equal To")
    .put(Operator.LIKE, "Like")
    .put(Operator.IN, "In")
    .put(Operator.BETWEEN, "Between")
    .build();

  public static final String AGE_AT_EVENT_PREFIX = "age";
  public static final String EVENT_DATE_PREFIX = "event";
  public static final String OCCURRENCES_PREFIX = "occ";
  public static final String ENCOUNTER_PREFIX = "enc";
  public static final String ANY = "ANY";

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
  public abstract QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters);

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

  protected String getUniqueNamedParameterPostfix() {
    return UUID.randomUUID().toString().replaceAll("-", "");
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
          String modifierParameter = isAgeAtEvent ? AGE_AT_EVENT_PREFIX : EVENT_DATE_PREFIX +
            getUniqueNamedParameterPostfix();
          modifierParamList.add("@" + modifierParameter);
          queryParams.put(modifierParameter, isAgeAtEvent ?
            QueryParameterValue.int64(new Long(operand)) : QueryParameterValue.date(operand));
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
        String modifierParameter = OCCURRENCES_PREFIX + getUniqueNamedParameterPostfix();
        modifierParamList.add("@" + modifierParameter);
        queryParams.put(modifierParameter, QueryParameterValue.int64(new Long(operand)));
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
    String modifierParameter = ENCOUNTER_PREFIX + getUniqueNamedParameterPostfix();
    Long[] operands = modifier.getOperands().stream().map(Long::new).toArray(Long[]::new);
    queryParams.put(modifierParameter, QueryParameterValue.array(operands, Long.class));
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
