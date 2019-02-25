package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.visitTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * VisitsQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for visit criteria types.
 */
@Service
public class VisitsQueryBuilder extends AbstractQueryBuilder {

  private static final String VISIT_SELECT_CLAUSE_TEMPLATE =
    "select person_id, entry_date, concept_id\n" +
      "from `${projectId}.${dataSetId}." + TABLE_ID + "` a\n" +
      "where ";
  private static final String CONCEPT_ID_TEMPLATE =
    "concept_id in unnest(${visitConceptIds})\n" +
      AGE_DATE_AND_ENCOUNTER_VAR;

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem inputParameters,
                           TemporalMention mention) {
    from(parametersEmpty()).test(inputParameters.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<Long> conceptIdList = new ArrayList<>();
    for (SearchParameter parameter : inputParameters.getSearchParameters()) {
      from(typeBlank().or(visitTypeInvalid())).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, parameter.getType());
      from(conceptIdNull()).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, parameter.getConceptId());
      conceptIdList.add(parameter.getConceptId());
    }

    if (conceptIdList.isEmpty()) {
      throw new BadRequestException(
        "Bad Request: please provide valid concept ids with search parameters ");
    }
    String baseSql = VISIT_SELECT_CLAUSE_TEMPLATE + CONCEPT_ID_TEMPLATE;
    List<Modifier> modifiers = inputParameters.getModifiers();
    String modifiedSql = buildModifierSql(baseSql, queryParams, modifiers);
    String finalSql = buildTemporalSql(modifiedSql, CONCEPT_ID_TEMPLATE, queryParams, modifiers, mention);
    String namedParameter = addQueryParameterValue(queryParams,
      QueryParameterValue.array(conceptIdList.stream().toArray(Long[]::new), Long.class));
    return finalSql.replace("${visitConceptIds}", "@" + namedParameter);
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.VISIT;
  }
}
