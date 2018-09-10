package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.exceptions.BadRequestException;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String CONCEPT_ID = "conceptId";
  private static final List<String> PM_TYPES_WITH_ATTR =
    Arrays.asList(TreeSubType.BP.name(),
      TreeSubType.HR_DETAIL.name(),
      TreeSubType.HEIGHT.name(),
      TreeSubType.WEIGHT.name(),
      TreeSubType.BMI.name(),
      TreeSubType.WC.name(),
      TreeSubType.HC.name());
  ImmutableMap<String, String> exceptionText = ImmutableMap.<String, String>builder()
    .put(TreeSubType.HR_DETAIL.name(), "Heart Rate")
    .put(TreeSubType.HEIGHT.name(), "Height")
    .put(TreeSubType.WEIGHT.name(), "Weight")
    .put(TreeSubType.BMI.name(), "BMI")
    .put(TreeSubType.WC.name(), "Waist Circumference")
    .put(TreeSubType.HC.name(), "Hip Circumference")
    .put(TreeSubType.HR.name(), "Heart Rate")
    .put(TreeSubType.PREG.name(), "Pregnancy")
    .put(TreeSubType.WHEEL.name(), "Wheel Chair User")
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

  private static final String VALUE_AS_CONCEPT_ID_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + "and value_as_concept_id = ${value}\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    for (SearchParameter parameter : parameters.getParameters()) {
      List<String> tempQueryParts = new ArrayList<String>();
      boolean isBP = parameter.getSubtype().equals(TreeSubType.BP.name());
      if (PM_TYPES_WITH_ATTR.contains(parameter.getSubtype())) {
        validateAttributes(parameter);
        for (Attribute attribute : parameter.getAttributes()) {
          String namedParameterConceptId = CONCEPT_ID + getUniqueNamedParameterPostfix();
          String namedParameter1 = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
          String namedParameter2 = getParameterPrefix(parameter.getSubtype()) + getUniqueNamedParameterPostfix();
          if (attribute.getName().equals(ANY)) {
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE : BASE_SQL_TEMPLATE;
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId));
            queryParams.put(namedParameterConceptId, QueryParameterValue.int64(attribute.getConceptId()));
          } else {
            boolean isBetween = attribute.getOperator().equals(Operator.BETWEEN);
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
        queryParts.add(VALUE_AS_CONCEPT_ID_SQL_TEMPLATE.replace("${conceptId}", "@" + namedParameterConceptId)
          .replace("${value}","@" + namedParameter));
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

  private void validateAttributes(SearchParameter parameter) {
    List<Attribute> attrs = parameter.getAttributes();
    Predicate<Attribute> systolic = nameIsSystolic().and(operatorWithCorrectOperands()).and(conceptIdNotNull());
    Predicate<Attribute> diastolic = nameIsDiastolic().and(operatorWithCorrectOperands()).and(conceptIdNotNull());
    if (parameter.getSubtype().equals(TreeSubType.BP.name())) {
      boolean systolicAttrs =
        attrs.stream().filter(systolic::test).collect(Collectors.toList()).size() != 1;
      boolean diastolicAttrs =
        attrs.stream().filter(diastolic::test).collect(Collectors.toList()).size() != 1;
      boolean anyAttrs =
        attrs.stream().filter(nameIsAny()::test).collect(Collectors.toList()).size() != 2;
      if ((systolicAttrs || diastolicAttrs) && anyAttrs) {
        throw new BadRequestException("Please provide valid search attributes" +
          "(name, operator, operands and conceptId) for Systolic and Diastolic.");
      }
    } else if (PM_TYPES_WITH_ATTR.contains(parameter.getSubtype())) {
      Predicate<Attribute> allTypes = operatorWithCorrectOperands().and(conceptIdNotNull());
      boolean allAttrs =
        parameter.getAttributes().stream().filter(allTypes::test).collect(Collectors.toList()).size() != 1;
      if (allAttrs) {
        throw new BadRequestException("Please provide valid search attributes(operator, operands) for "
          + exceptionText.get(parameter.getSubtype()) + ".");
      }
    }
  }

  private void validateSearchParameter(SearchParameter parameter) {
    if (parameter.getConceptId() == null || parameter.getValue() == null) {
      throw new BadRequestException("Please provide valid conceptId and value for "
        + exceptionText.get(parameter.getSubtype()) + ".");
    } else if (!NumberUtils.isNumber(parameter.getValue())) {
      throw new BadRequestException("Please provide valid value for "
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

  private static Predicate<Attribute> nameIsAny() { return attribute -> isNameAny(attribute); }

  private static Predicate<Attribute> operatorWithCorrectOperands() {
    return attribute -> isNameAny(attribute)
      || isOperatorBetween(attribute)
      || isOperatorAnyEquals(attribute)
      || isOperatorIn(attribute);
  }
}