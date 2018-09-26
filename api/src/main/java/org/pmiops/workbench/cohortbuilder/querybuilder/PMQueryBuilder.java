package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.AttributePredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.ParameterPredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.Validation.from;

@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String CONCEPTID = "conceptId";
  public static final String BP_TWO_ATTRIBUTE_MESSAGE =
    "Bad Request: Attribute Blood Pressure must provide two attributes(systolic and diastolic).";

  private static final List<String> PM_TYPES_WITH_ATTR =
    Arrays.asList(TreeSubType.BP.name(),
      TreeSubType.HR_DETAIL.toString(),
      TreeSubType.HEIGHT.name(),
      TreeSubType.WEIGHT.name(),
      TreeSubType.BMI.name(),
      TreeSubType.WC.name(),
      TreeSubType.HC.name());

  private static final String UNION_ALL = " union all\n";
  private static final String INTERSECT_DISTINCT = "intersect distinct\n";
  private static final String VALUE_AS_NUMBER = "and value_as_number ${operator} ${value}\n";

  private static final String BP_INNER_SQL_TEMPLATE =
    "select person_id, measurement_date from `${projectId}.${dataSetId}.${tableName}`\n" +
    "   where measurement_source_concept_id = ${conceptId}\n";

  private static final String BP_SQL_TEMPLATE =
    "select person_id from( ${bpInnerSqlTemplate} )";

  private static final String BASE_SQL_TEMPLATE =
    "select person_id from `${projectId}.${dataSetId}.${tableName}`\n" +
      "where measurement_source_concept_id = ${conceptId}\n";

  private static final String VALUE_AS_NUMBER_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + VALUE_AS_NUMBER;

  private static final String VALUE_AS_CONCEPT_ID_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + "and value_as_concept_id = ${value}\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    from(parametersEmpty()).test(parameters.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    for (SearchParameter parameter : parameters.getParameters()) {
      validateSearchParameter(parameter);
      List<String> tempQueryParts = new ArrayList<String>();
      boolean isBP = parameter.getSubtype().equals(TreeSubType.BP.name());
      if (PM_TYPES_WITH_ATTR.contains(parameter.getSubtype())) {
        for (Attribute attribute : parameter.getAttributes()) {
          validateAttribute(attribute);
          String namedParameterConceptId = CONCEPTID + getUniqueNamedParameterPostfix();
          String namedParameter1 = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
          String namedParameter2 = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
          if (attribute.getName().equals(ANY)) {
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE : BASE_SQL_TEMPLATE;
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId)
              .replace("${tableName}", parameter.getDomain().toLowerCase()));
            queryParams.put(namedParameterConceptId, QueryParameterValue.int64(attribute.getConceptId()));
          } else {
            boolean isBetween = attribute.getOperator().equals(Operator.BETWEEN);
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE + VALUE_AS_NUMBER : VALUE_AS_NUMBER_SQL_TEMPLATE;
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId)
              .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
              .replace("${value}", isBetween ? "@" + namedParameter1 + " and " + "@" + namedParameter2
                : "@" + namedParameter1)
              .replace("${tableName}", parameter.getDomain().toLowerCase()));
            queryParams.put(namedParameterConceptId, QueryParameterValue.int64(attribute.getConceptId()));
            queryParams.put(namedParameter1, QueryParameterValue.int64(new Long(attribute.getOperands().get(0))));
            if (isBetween) {
              queryParams.put(namedParameter2, QueryParameterValue.int64(new Long(attribute.getOperands().get(1))));
            }
          }
        }
        if (isBP) {
          queryParts.add(BP_SQL_TEMPLATE.replace("${bpInnerSqlTemplate}", String.join(INTERSECT_DISTINCT, tempQueryParts)));
        } else {
          queryParts.addAll(tempQueryParts);
        }
      } else {
        String namedParameterConceptId = CONCEPTID + getUniqueNamedParameterPostfix();
        String namedParameter = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
        queryParts.add(VALUE_AS_CONCEPT_ID_SQL_TEMPLATE.replace("${conceptId}", "@" + namedParameterConceptId)
          .replace("${value}","@" + namedParameter)
          .replace("${tableName}", parameter.getDomain().toLowerCase()));
        queryParams.put(namedParameterConceptId, QueryParameterValue.int64(parameter.getConceptId()));
        queryParams.put(namedParameter, QueryParameterValue.int64(new Long(parameter.getValue())));
      }
    }
    String finalSql = String.join(UNION_ALL, queryParts);
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.PM;
  }

  private String getParameterPrefix(String subtype) {
    return subtype.toLowerCase().replace("-", "");
  }

  private void validateSearchParameter(SearchParameter param) {
    String type = param.getType();
    String subtype = param.getSubtype();
    from(typeBlank().or(pmTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, type);
    from(subtypeBlank().or(pmSubtypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, subtype);
    if (PM_TYPES_WITH_ATTR.contains(param.getSubtype())) {
      from(notAnyAttr().and(attributesEmpty())).test(param).throwException(EMPTY_MESSAGE, ATTRIBUTES);
      if (param.getSubtype().equals(TreeSubType.BP.name())) {
        from(notAnyAttr().and(notTwoAttributes())).test(param).throwException(BP_TWO_ATTRIBUTE_MESSAGE);
        from(notAnyAttr().and(notSystolicAndDiastolic())).test(param).throwException(BP_TWO_ATTRIBUTE_MESSAGE);
      }
    } else {
      String domain = param.getDomain();
      String value = param.getValue();
      Long conceptId = param.getConceptId();
      from(domainBlank().or(domainNotMeasurement())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, DOMAIN, domain);
      from(valueNull().or(valueNotNumber())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, VALUE, value);
      from(conceptIdNull()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, conceptId);
    }
  }

  private void validateAttribute(Attribute attr) {
    if (!ANY.equals(attr.getName())) {
      String name = attr.getName();
      String oper = operatorText.get(attr.getOperator());
      Long conceptId = attr.getConceptId();
      from(nameBlank()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, NAME, name);
      from(operatorNull()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, oper);
      from(operandsEmpty()).test(attr).throwException(EMPTY_MESSAGE, OPERANDS);
      from(attrConceptIdNull()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, CONCEPT_ID, conceptId);
      from(notBetweenOperator().and(operandsNotOne())).test(attr).throwException(ONE_OPERAND_MESSAGE, ATTRIBUTE, name, oper);
      from(betweenOperator().and(operandsNotTwo())).test(attr).throwException(TWO_OPERAND_MESSAGE, ATTRIBUTE, name, oper);
      from(operandsNotNumbers()).test(attr).throwException(OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE, name);
    }
  }
}