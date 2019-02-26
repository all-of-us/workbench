package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;

import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.betweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.categoricalAndNotIn;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.nameBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotTwo;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operatorNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.attributesEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.measTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ANY;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTES;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.BOTH;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CATEGORICAL;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CATEGORICAL_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NAME;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NUMERICAL;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS_NUMERIC_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERATOR;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TWO_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.operatorText;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * MeasurementQueryBuilder builds SQL for BigQuery for measurement criteria types.
 */
@Service
public class MeasurementQueryBuilder extends AbstractQueryBuilder {

  private static final String MEASUREMENT_SQL_TEMPLATE =
    "select person_id, entry_date, concept_id\n" +
      "from `${projectId}.${dataSetId}." + TABLE_ID + "`\n" +
      "where ";
  private static final String CONCEPT_ID_EQUAL_TEMPLATE =
    "concept_id = ${conceptId}";
  private static final String CONCEPT_ID_IN_TEMPLATE =
    "concept_id in unnest(${conceptIds})";
  private static final String VALUE_AS_NUMBER =
    " and value_as_number ${operator} ${value}";
  private static final String VALUE_AS_CONCEPT_ID =
    " and value_as_concept_id ${operator} unnest(${values})";

  /**
   * {@inheritDoc}
   */
  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           TemporalMention mention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<String>();
    List<Long> conceptIds = new ArrayList<>();

    for (SearchParameter parameter : searchGroupItem.getSearchParameters()) {
      validateSearchParameter(parameter);
      for (Attribute attribute : parameter.getAttributes()) {
        validateAttribute(attribute);
        if (attribute.getName().equals(ANY)) {
          conceptIds.add(parameter.getConceptId());
          if (!queryParts.contains("(" + CONCEPT_ID_IN_TEMPLATE + ")\n")) {
            queryParts.add("(" + CONCEPT_ID_IN_TEMPLATE + ")\n");
          }
        } else {
          String namedParameter = addQueryParameterValue(queryParams,
            QueryParameterValue.int64(parameter.getConceptId()));
          String queryPartSql = CONCEPT_ID_EQUAL_TEMPLATE.replace("${conceptId}", "@" + namedParameter);
          if (attribute.getName().equals(NUMERICAL)) {
            queryParts.add(processNumericalSql(queryParams,"(" + queryPartSql + VALUE_AS_NUMBER + ")\n", attribute));
          } else if (attribute.getName().equals(CATEGORICAL)) {
            queryParts.add(processCategoricalSql(queryParams,"(" + queryPartSql + VALUE_AS_CONCEPT_ID + ")\n", attribute));
          } else if (attribute.getName().equals(BOTH) && attribute.getOperator().equals(Operator.IN)) {
            queryParts.add(processCategoricalSql(queryParams,"(" + queryPartSql + VALUE_AS_CONCEPT_ID + ")\n", attribute));
          } else {
            queryParts.add(processNumericalSql(queryParams,"(" + queryPartSql + VALUE_AS_NUMBER + ")\n", attribute));
          }
        }
      }
    }
    List<Modifier> modifiers = searchGroupItem.getModifiers();
    String idParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(conceptIds.stream().toArray(Long[]::new), Long.class));
    String conceptIdSql = String.join(OR, queryParts).replace("${conceptIds}", "@" + idParameter) + AGE_DATE_AND_ENCOUNTER_VAR;
    String baseSql = MEASUREMENT_SQL_TEMPLATE + conceptIdSql;
    String modifiedSql = buildModifierSql(baseSql, queryParams, modifiers);
    return buildTemporalSql(modifiedSql, conceptIdSql, queryParams, modifiers, mention);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FactoryKey getType() {
    return FactoryKey.MEAS;
  }

  private void validateSearchParameter(SearchParameter param) {
    from(attributesEmpty()).test(param).throwException(EMPTY_MESSAGE, ATTRIBUTES);
    from(typeBlank().or(measTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(conceptIdNull()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
  }

  private void validateAttribute(Attribute attr) {
    if (!ANY.equals(attr.getName())) {
      String name = attr.getName();
      String oper = operatorText.get(attr.getOperator());
      from(nameBlank()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, NAME, name);
      from(operatorNull()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, oper);
      from(operandsEmpty()).test(attr).throwException(EMPTY_MESSAGE, OPERANDS);
      from(categoricalAndNotIn()).test(attr).throwException(CATEGORICAL_MESSAGE);
      from(betweenOperator().and(operandsNotTwo())).test(attr).throwException(TWO_OPERAND_MESSAGE, ATTRIBUTE, name, oper);
      from(operandsNotNumbers()).test(attr).throwException(OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE, name);
    }
  }

  private String processNumericalSql(Map<String, QueryParameterValue> queryParams,
                                     String baseSql,
                                     Attribute attribute) {
    String namedParameter1 = addQueryParameterValue(queryParams,
      QueryParameterValue.float64(new Double(attribute.getOperands().get(0))));
    String valueExpression;
    if (attribute.getOperator().equals(Operator.BETWEEN)) {
      String namedParameter2 = addQueryParameterValue(queryParams,
        QueryParameterValue.float64(new Double(attribute.getOperands().get(1))));
      valueExpression = "@" + namedParameter1 + AND + "@" + namedParameter2;
    } else {
      valueExpression = "@" + namedParameter1;
    }

    return baseSql
      .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
      .replace("${value}", valueExpression);
  }

  private String processCategoricalSql(Map<String, QueryParameterValue> queryParams,
                                       String baseSql,
                                       Attribute attribute) {
    String namedParameter1 = addQueryParameterValue(queryParams,
      QueryParameterValue.array(attribute.getOperands().stream().map(s -> Long.parseLong(s)).toArray(Long[]::new),
        Long.class));
    return baseSql
      .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
      .replace("${values}", "@" + namedParameter1);
  }
}
