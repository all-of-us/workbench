package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.nameNotNumber;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.ppiTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.valueNotNumber;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NAME;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.VALUE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

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
    "and value_as_number = ${value}\n";

  private static final String VALUE_AS_CONCEPT_ID_SQL_TEMPLATE =
    "and value_as_concept_id = ${value}\n";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           TemporalMention temporalMention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<String>();
    for (SearchParameter parameter : searchGroupItem.getSearchParameters()) {
      validateSearchParameter(parameter);
      StringBuilder sqlTemplate = new StringBuilder(PPI_SQL_TEMPLATE);
      if (parameter.getConceptId() == null && parameter.getGroup()) {
        String subtype = addQueryParameterValue(queryParams,
          QueryParameterValue.string(parameter.getSubtype()));
        sqlTemplate.append(SURVEY_IN_CLAUSE
          .replace("${subtype}", "@" + subtype));
      } else {
        String namedParameterConceptId = addQueryParameterValue(queryParams,
          QueryParameterValue.int64(parameter.getConceptId()));
        sqlTemplate.append(QUESTION_ANSWER_IN_CLAUSE
          .replace("${conceptId}", "@" + namedParameterConceptId));
        if (!parameter.getGroup()) {
          boolean isValueAsNum = StringUtils.isBlank(parameter.getValue());
          String value = isValueAsNum ? parameter.getName() : parameter.getValue();
          String namedParameter = addQueryParameterValue(queryParams,
            QueryParameterValue.int64(new Long(value)));
          sqlTemplate.append(isValueAsNum ?
            VALUE_AS_NUMBER_SQL_TEMPLATE.replace("${value}","@" + namedParameter) :
            VALUE_AS_CONCEPT_ID_SQL_TEMPLATE.replace("${value}","@" + namedParameter));
        }
      }
      queryParts.add(sqlTemplate.toString());
    }
    return String.join(UNION_ALL, queryParts);
  }

  private void validateSearchParameter(SearchParameter param) {
    from(typeBlank().or(ppiTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    if (!param.getGroup()) {
      if (StringUtils.isBlank(param.getValue())) {
        from(nameNotNumber()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, NAME, param.getName());
      } else {
        from(valueNotNumber()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, VALUE, param.getValue());
      }
    }
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.PPI;
  }
}
