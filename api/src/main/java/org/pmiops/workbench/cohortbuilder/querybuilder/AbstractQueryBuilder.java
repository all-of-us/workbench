package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
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

  private static final List<Operator> MODIFIER_OPERATORS =
    Arrays.asList(Operator.BETWEEN, Operator.GREATER_THAN_OR_EQUAL_TO,
      Operator.LESS_THAN_OR_EQUAL_TO);

  private static final String DISTINCT = "distinct";
  public static final String WHERE = " where ";
  public static final String AND = " and ";
  private static final String MODIFIER_COLUMNS_TEMPLATE = ", a.${entryDate} as entry_date, b.concept_code";
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

  public String buildModifierSql(String query, List<Modifier> modifiers, FactoryKey key) {
    String sql = "";
    String sqlDistinct = "";
    String sqlColumns = "";
    if (modifiers.isEmpty()) {
      return query.replace("${modifierDistinct}", "")
        .replace("${modifierColumns}", "");
    } else {
      Modifier ageAtEvent = getModifier(modifiers, ModifierType.AGE_AT_EVENT);
      Modifier eventDate = getModifier(modifiers, ModifierType.EVENT_DATE);
      Modifier occurrences = getModifier(modifiers, ModifierType.NUM_OF_OCCURRENCES);
      if (ageAtEvent != null) {
        sql = AGE_AT_EVENT_SQL_TEMPLATE;
      }
      if (eventDate != null) {
        if (sql.equals("")) {
          sql = WHERE + EVENT_DATE_SQL_TEMPLATE;
        } else {
          sql = sql + AND + EVENT_DATE_SQL_TEMPLATE;
        }
      }
      if (occurrences != null) {
        sqlDistinct = DISTINCT;
        sqlColumns = MODIFIER_COLUMNS_TEMPLATE;
        sql = sql + OCCURRENCES_SQL_TEMPLATE;
      }
    }
    return MODIFIER_SQL_TEMPLATE.replace("${innerSql}", query)
      .replace("${modifierDistinct}", sqlDistinct)
      .replace("${modifierColumns}", sqlColumns) + sql;
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

  public abstract FactoryKey getType();

  protected String getUniqueNamedParameterPostfix() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }
}
