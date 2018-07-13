package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String BLOOD_PRESSURE = "BP";
  private static final String HEART_RATE_DETAIL = "HR-DETAIL";
  private static final String HEIGHT = "HEIGHT";
  private static final String WEIGHT = "WEIGHT";
  private static final String BMI = "BMI";
  private static final String WC = "WC";
  private static final String HC = "HC";
  private static final String HR = "HR";
  private static final String PREG = "PREG";
  private static final String WHEEL = "WHEEL";
  private static final String CONCEPT_ID = "conceptId";
  private static final List<String> PM_TYPES_WITH_ATTR =
    Arrays.asList(BLOOD_PRESSURE, HEART_RATE_DETAIL, HEIGHT, WEIGHT, BMI, WC, HC);
  ImmutableMap<String, String> exceptionText = ImmutableMap.<String, String>builder()
    .put(HEART_RATE_DETAIL, "Heart Rate")
    .put(HEIGHT, "Height")
    .put(WEIGHT, "Weight")
    .put(BMI, "BMI")
    .put(WC, "Waist Circumference")
    .put(HC, "Hip Circumference")
    .put(HR, "Heart Rate")
    .put(PREG, "Pregnancy")
    .put(WHEEL, "Wheel Chair User")
    .build();

  private static final String UNION_ALL = " union all\n";
  private static final String INTERSECT_DISTINCT = "intersect distinct\n";
  private static final String VALUE_AS_NUMBER = "and value_as_number ${operator} ${value}\n";

  private static final String BP_INNER_SQL_TEMPLATE =
    "select person_id, measurement_date from `${projectId}.${dataSetId}.measurement`\n" +
    "   where measurement_source_concept_id = ${conceptId}\n";

  private static final String BP_SQL_TEMPLATE =
    "select person_id from( ${bpInnerSqlTemplate} )";

  private static final String BASE_SQL_TEMPLATE =
    "select person_id from `${projectId}.${dataSetId}.measurement`\n" +
      "where measurement_source_concept_id = ${conceptId}\n";

  private static final String VALUE_AS_NUMBER_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + VALUE_AS_NUMBER;

  private static final String VALUE_SOURCE_VALUE_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + "and value_source_value = ${value}\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    for (SearchParameter parameter : parameters.getParameters()) {
      List<String> tempQueryParts = new ArrayList<String>();
      boolean isBP = parameter.getSubtype().equals(BLOOD_PRESSURE);
      if (PM_TYPES_WITH_ATTR.contains(parameter.getSubtype())) {
        validateAttributes(parameter);
        for (Attribute attribute : parameter.getAttributes()) {
          boolean isBetween = attribute.getOperator().equals(Operator.BETWEEN);
          String namedParameterConceptId = CONCEPT_ID + getUniqueNamedParameterPostfix();
          String namedParameter1 = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
          String namedParameter2 = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
          if (attribute.getOperator().equals(Operator.ANY)) {
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE : BASE_SQL_TEMPLATE;
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId));
            queryParams.put(namedParameterConceptId, QueryParameterValue.int64(attribute.getConceptId()));
          } else {
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE + VALUE_AS_NUMBER : VALUE_AS_NUMBER_SQL_TEMPLATE;
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId)
              .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
              .replace("${value}", isBetween ? "@" + namedParameter1 + " and " + "@" + namedParameter2
                : "@" + namedParameter1));
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
        validateSearchParameter(parameter);
        String namedParameterConceptId = CONCEPT_ID + getUniqueNamedParameterPostfix();
        String namedParameter = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
        queryParts.add(VALUE_SOURCE_VALUE_SQL_TEMPLATE.replace("${conceptId}", "@" + namedParameterConceptId)
          .replace("${value}","@" + namedParameter));
        queryParams.put(namedParameterConceptId, QueryParameterValue.int64(parameter.getConceptId()));
        queryParams.put(namedParameter, QueryParameterValue.string(parameter.getValue()));
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

  private void validateAttributes(SearchParameter parameter) {
    if (parameter.getSubtype().equals(BLOOD_PRESSURE)) {
      List<Attribute> systolicAttrs =
        parameter.getAttributes().stream()
          .filter(nameIsSystolic()
            .and(operatorWithCorrectOperands())
            .and(conceptIdNotNull())::test)
          .collect(Collectors.toList());
      List<Attribute> diastolicAttrs =
        parameter.getAttributes().stream()
          .filter(nameIsDiastolic()
            .and(operatorWithCorrectOperands())
            .and(conceptIdNotNull())::test)
          .collect(Collectors.toList());
      if (systolicAttrs.size() != 1 || diastolicAttrs.size() != 1) {
        throw new BadRequestException("Please provide valid search attributes(name, operator, operands and conceptId) for Systolic and Diastolic.");
      }
    } else if (PM_TYPES_WITH_ATTR.contains(parameter.getSubtype())) {
      List<Attribute> attrs =
        parameter.getAttributes().stream()
          .filter(operatorWithCorrectOperands()
            .and(conceptIdNotNull())::test)
          .collect(Collectors.toList());
      if (attrs.size() != 1) {
        throw new BadRequestException("Please provide valid search attributes(operator, operands) for "
          + exceptionText.get(parameter.getSubtype()) + ".");
      }
    }
  }

  private void validateSearchParameter(SearchParameter parameter) {
    if (parameter.getConceptId() == null || parameter.getValue() == null) {
      throw new BadRequestException("Please provide valid conceptId and value for "
        + exceptionText.get(parameter.getSubtype()) + ".");
    }
  }

  private static Predicate<Attribute> nameIsSystolic() {
    return attribute -> "Systolic".equals(attribute.getName());
  }

  private static Predicate<Attribute> nameIsDiastolic() {
    return attribute -> "Diastolic".equals(attribute.getName());
  }

  private static Predicate<Attribute> conceptIdNotNull() {
    return attribute -> attribute.getConceptId() != null;
  }

  private static Predicate<Attribute> operatorWithCorrectOperands() {
    return attribute ->
      (attribute.getOperator() != null
        && attribute.getOperator().equals(Operator.ANY)
        && attribute.getOperands().size() == 0)
          || (attribute.getOperator() != null
        && attribute.getOperator().equals(Operator.BETWEEN)
        && attribute.getOperands().size() == 2
        && StringUtils.isNumeric(attribute.getOperands().get(0)))
        && StringUtils.isNumeric(attribute.getOperands().get(1))
          || (attribute.getOperator() != null
        && !attribute.getOperator().equals(Operator.BETWEEN)
        && !attribute.getOperator().equals(Operator.ANY)
        && attribute.getOperands().size() == 1
        && StringUtils.isNumeric(attribute.getOperands().get(0)));
  }
}