package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

  ImmutableMap<ModifierType, String> exceptionText = ImmutableMap.<ModifierType, String>builder()
    .put(ModifierType.AGE_AT_EVENT, "age at event")
    .put(ModifierType.EVENT_DATE, "event date")
    .put(ModifierType.NUM_OF_OCCURRENCES, "number of occurrences")
    .build();

  public static final String AGE_AT_EVENT_PREFIX = "age";
  public static final String EVENT_DATE_PREFIX = "event";
  public static final String OCCURRENCES_PREFIX = "occ";

  public static final String WHERE = " where ";
  public static final String AND = " and ";
  private static final String MODIFIER_SQL_TEMPLATE = "select criteria.person_id from (${innerSql}) criteria\n";
  private static final String AGE_AT_EVENT_SQL_TEMPLATE =
    "join `${projectId}.${dataSetId}.person` p on (criteria.person_id = p.person_id)\n" +
    " where CAST(FLOOR(DATE_DIFF(criteria.entry_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64)\n";
  private static final String EVENT_DATE_SQL_TEMPLATE = "criteria.entry_date\n";
  private static final String OCCURRENCES_SQL_TEMPLATE = "group by criteria.person_id\n" +
    "having count(criteria.person_id)";

  /**
   * Build a {@link QueryJobConfiguration} from the specified
   * {@link QueryParameters} provided.
   *
   * @param parameters
   * @return
   */
  public abstract QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters);

  public QueryJobConfiguration buildModifierSql(String baseSql, Map<String, QueryParameterValue> queryParams, List<Modifier> modifiers, FactoryKey key) {
    String modifierSql = "";
    String finalSql = "";
    if (!modifiers.isEmpty()) {
      Modifier ageAtEvent = getModifier(modifiers, ModifierType.AGE_AT_EVENT);
      Modifier eventDate = getModifier(modifiers, ModifierType.EVENT_DATE);
      Modifier occurrences = getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES);
      if (ageAtEvent != null) {
        validateOperands(ageAtEvent);
        List<String> modifierParamList = new ArrayList<>();
        for (String operand : ageAtEvent.getOperands()) {
          String modifierParameter = AGE_AT_EVENT_PREFIX + getUniqueNamedParameterPostfix();
          modifierParamList.add("@" + modifierParameter);
          queryParams.put(modifierParameter, QueryParameterValue.int64(new Long(operand)));
        }
        if (modifierSql.equals("")) {
          modifierSql = AGE_AT_EVENT_SQL_TEMPLATE;
        } else {
          modifierSql = modifierSql + AND + AGE_AT_EVENT_SQL_TEMPLATE;
        }
        modifierSql = modifierSql +
          OperatorUtils.getSqlOperator(ageAtEvent.getOperator()) + " " +
          String.join(AND, modifierParamList);
      }
      if (eventDate != null) {
        List<String> modifierParamList = new ArrayList<>();
        validateOperands(eventDate);
        for (String operand : eventDate.getOperands()) {
          String modifierParameter = EVENT_DATE_PREFIX + getUniqueNamedParameterPostfix();
          modifierParamList.add("@" + modifierParameter);
          queryParams.put(modifierParameter, QueryParameterValue.date(operand));
        }
        if (modifierSql.equals("")) {
          modifierSql = WHERE + EVENT_DATE_SQL_TEMPLATE;
        } else {
          modifierSql = modifierSql + AND + EVENT_DATE_SQL_TEMPLATE;
        }
        modifierSql = modifierSql +
          OperatorUtils.getSqlOperator(eventDate.getOperator()) + " " +
          String.join(AND, modifierParamList);
      }
      if (occurrences != null) {
        modifierSql = modifierSql + OCCURRENCES_SQL_TEMPLATE;
      }
    }
    finalSql = MODIFIER_SQL_TEMPLATE.replace("${innerSql}", baseSql) +
      modifierSql;

    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  private void validateOperands(Modifier modifier) {
    if (modifier.getOperator().equals(Operator.BETWEEN) &&
      modifier.getOperands().size() != 2) {
      throw new BadRequestException(String.format(
        "Modifier: %s can only have 2 operands when using the %s operator",
        modifier.getName(),
        modifier.getOperator().name()));
    }
    if (modifier.getName().equals(ModifierType.AGE_AT_EVENT) &&
      modifier.getOperator().equals(Operator.BETWEEN)) {
      try {
        modifier.getOperands().stream().map(Long::new);
      } catch (NumberFormatException nfe) {
        throw new BadRequestException(String.format(
          "Please provide valid number for "
            + exceptionText.get(ModifierType.AGE_AT_EVENT)));
      }
    }
  }

  public abstract FactoryKey getType();

  protected String getUniqueNamedParameterPostfix() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  private Modifier getModifier(List<Modifier> modifiers, ModifierType modifierType) {
    List<Modifier> modifierList =  modifiers.stream()
      .filter(modifier -> modifier.getName().equals(modifierType))
      .collect(Collectors.toList());
    if (modifierList.isEmpty()) {
      return null;
    } else if (modifierList.size() == 1){
      return modifierList.get(0);
    }
    throw new BadRequestException("Please provide one " +
      exceptionText.get(modifierType) + " modifier.");
  }

  private void validateOperator(Modifier modifier) {
    if (modifier.getOperands().size() != 2) {
      throw new BadRequestException(String.format(
        "Modifier: %s can only have 2 operands when using the %s operator",
        modifier.getName(),
        modifier.getOperator().name()));
    }
  }

}
