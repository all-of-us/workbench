package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * PPIQueryBuilder builds SQL for BigQuery for the survey criteria type.
 */
@Service
public class PPIQueryBuilder extends AbstractQueryBuilder {

  private static final String UNION_ALL = " union all\n";
  private static final String PPI_SQL_TEMPLATE =
    "select person_id from `${projectId}.${dataSetId}." + TABLE_ID + "`\n" +
      "where concept_id\n";

  private static final String SURVEY_IN_CLAUSE =
    "in (select concept_id\n" +
      "from `${projectId}.${dataSetId}.criteria`\n" +
      "where path like (\n" +
      "select concat('%', CAST(id as STRING), '%') as path\n" +
      "from `${projectId}.${dataSetId}.criteria`\n" +
      "where subtype = ${subtype}\n" +
      "and parent_id = 0))";

  private static final String QUESTION_ANSWER_IN_CLAUSE =
    "in (${conceptId}) ";

  private static final String VALUE_AS_NUMBER_SQL_TEMPLATE =
    "and value_as_number ${operator} ${value}\n";

  private static final String VALUE_AS_CONCEPT_ID_SQL_TEMPLATE =
    "and value_as_concept_id ${operator} (${value})\n";

  /**
   * {@inheritDoc}
   */
  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           TemporalMention temporalMention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<>();
    for (SearchParameter parameter : searchGroupItem.getSearchParameters()) {
      from(typeBlank().or(ppiTypeInvalid())).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, parameter.getType());
      StringBuilder sqlTemplate = new StringBuilder(PPI_SQL_TEMPLATE);
      if (parameter.getConceptId() == null && parameter.getGroup()) {
        String subtype = addQueryParameterValue(queryParams,
          QueryParameterValue.string(parameter.getSubtype()));
        sqlTemplate.append(SURVEY_IN_CLAUSE
          .replace("${subtype}", "@" + subtype));
      } else {
        from(conceptIdNull()).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, parameter.getConceptId());
        String namedParameterConceptId = addQueryParameterValue(queryParams,
          QueryParameterValue.int64(parameter.getConceptId()));
        sqlTemplate.append(QUESTION_ANSWER_IN_CLAUSE
          .replace("${conceptId}", "@" + namedParameterConceptId));
        if (!parameter.getGroup()) {
          from(attributesEmpty()).test(parameter).throwException(EMPTY_MESSAGE, ATTRIBUTES);
          Attribute attr = parameter.getAttributes().get(0);
          validateAttribute(attr);
          boolean isValueAsNum = attr.getName().equals("NUM");
          String namedParameter = addQueryParameterValue(queryParams,
            QueryParameterValue.int64(new Long(attr.getOperands().get(0))));
          sqlTemplate.append(isValueAsNum ?
            VALUE_AS_NUMBER_SQL_TEMPLATE
              .replace("${operator}", OperatorUtils.getSqlOperator(attr.getOperator()))
              .replace("${value}","@" + namedParameter) :
            VALUE_AS_CONCEPT_ID_SQL_TEMPLATE
              .replace("${operator}", OperatorUtils.getSqlOperator(attr.getOperator()))
              .replace("${value}","@" + namedParameter));
        }
      }
      queryParts.add(sqlTemplate.toString());
    }
    return String.join(UNION_ALL, queryParts);
  }

  private void validateAttribute(Attribute attr) {
      String name = attr.getName() == null ? null : attr.getName().name();
      String oper = operatorText.get(attr.getOperator());
      from(nameBlank()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, NAME, name);
      from(operatorNull()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, oper);
      from(operandsEmpty()).test(attr).throwException(EMPTY_MESSAGE, OPERANDS);
      from(categoricalAndNotIn()).test(attr).throwException(CATEGORICAL_MESSAGE);
      from(operandsNotOne()).test(attr).throwException(ONE_OPERAND_MESSAGE, ATTRIBUTE, name, Operator.EQUAL);
      from(operandsNotNumbers()).test(attr).throwException(OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE, name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FactoryKey getType() {
    return FactoryKey.PPI;
  }
}
