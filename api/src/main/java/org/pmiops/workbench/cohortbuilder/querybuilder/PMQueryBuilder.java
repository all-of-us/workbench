package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.attrConceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.betweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.nameBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.notBetweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotOne;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotTwo;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operatorNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.attributesEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.notAnyAttr;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.notSystolicAndDiastolic;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.notTwoAttributes;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.pmSubtypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.pmTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.subtypeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.valueNotNumber;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.valueNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ANY;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTES;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.BP_TWO_ATTRIBUTE_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NAME;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ONE_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS_NUMERIC_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERATOR;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PM_TYPES_WITH_ATTR;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SUBTYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TWO_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.VALUE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.operatorText;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * PMQueryBuilder builds SQL for BigQuery for the physical measurement criteria types.
 */
@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String UNION_ALL = " union all\n";
  private static final String INTERSECT_DISTINCT = "intersect distinct\n";
  private static final String VALUE_AS_NUMBER = "and value_as_number ${operator} ${value}\n";

  private static final String BP_INNER_SQL_TEMPLATE =
    "select person_id from `${projectId}.${dataSetId}." + TABLE_ID + "`\n" +
    "   where concept_id = ${conceptId}\n";

  private static final String BP_SQL_TEMPLATE =
    "select person_id from( ${bpInnerSqlTemplate} )\n";

  private static final String BASE_SQL_TEMPLATE =
    "select person_id from `${projectId}.${dataSetId}." + TABLE_ID + "`\n" +
      "where concept_id = ${conceptId}\n";

  private static final String VALUE_AS_NUMBER_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + VALUE_AS_NUMBER;

  private static final String VALUE_AS_CONCEPT_ID_SQL_TEMPLATE =
    BASE_SQL_TEMPLATE + "and value_as_concept_id = ${value}\n";

  /**
   * {@inheritDoc}
   */
  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           TemporalMention temporalMention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<String>();
    for (SearchParameter parameter : searchGroupItem.getSearchParameters()) {
      validateSearchParameter(parameter);
      List<String> tempQueryParts = new ArrayList<String>();
      boolean isBP = parameter.getSubtype().equals(TreeSubType.BP.name());
      if (PM_TYPES_WITH_ATTR.contains(parameter.getSubtype())) {
        for (Attribute attribute : parameter.getAttributes()) {
          validateAttribute(attribute);
          if (attribute.getName().equals(ANY)) {
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE : BASE_SQL_TEMPLATE;
            String namedParameterConceptId = addQueryParameterValue(queryParams,
                QueryParameterValue.int64(attribute.getConceptId()));
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId));
          } else {
            boolean isBetween = attribute.getOperator().equals(Operator.BETWEEN);
            String tempSql = isBP ? BP_INNER_SQL_TEMPLATE + VALUE_AS_NUMBER : VALUE_AS_NUMBER_SQL_TEMPLATE;
            String namedParameterConceptId = addQueryParameterValue(queryParams,
                QueryParameterValue.int64(attribute.getConceptId()));
            String namedParameter1 = addQueryParameterValue(queryParams,
                QueryParameterValue.float64(new Float(attribute.getOperands().get(0))));
            String valueExpression;
            if (isBetween) {
              String namedParameter2 = addQueryParameterValue(queryParams,
                  QueryParameterValue.float64(new Float(attribute.getOperands().get(1))));
              valueExpression = "@" + namedParameter1 + " and " + "@" + namedParameter2;
            } else {
              valueExpression = "@" + namedParameter1;
            }
            tempQueryParts.add(tempSql
              .replace("${conceptId}", "@" + namedParameterConceptId)
              .replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
              .replace("${value}", valueExpression));
          }
        }
        if (isBP) {
          queryParts.add(BP_SQL_TEMPLATE.replace("${bpInnerSqlTemplate}", String.join(INTERSECT_DISTINCT, tempQueryParts)));
        } else {
          queryParts.addAll(tempQueryParts);
        }
      } else {
        String namedParameterConceptId = addQueryParameterValue(queryParams,
            QueryParameterValue.int64(parameter.getConceptId()));
        String namedParameter = addQueryParameterValue(queryParams,
            QueryParameterValue.int64(new Long(parameter.getValue())));
        queryParts.add(VALUE_AS_CONCEPT_ID_SQL_TEMPLATE.replace("${conceptId}", "@" + namedParameterConceptId)
          .replace("${value}","@" + namedParameter));
      }
    }
    return String.join(UNION_ALL, queryParts);
  }

  /**
   * {@inheritDoc}
   */
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
      String value = param.getValue();
      Long conceptId = param.getConceptId();
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