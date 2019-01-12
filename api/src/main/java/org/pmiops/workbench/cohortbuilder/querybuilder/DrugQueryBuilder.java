package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.drugTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

@Service
public class DrugQueryBuilder extends AbstractQueryBuilder {

  private static final String DRUG_SQL_TEMPLATE =
    "select distinct person_id, entry_date, concept_id\n" +
      "from `${projectId}.${dataSetId}.search_drug`\n" +
      "where ";

  private static final String CHILD_IN_CLAUSE_TEMPLATE =
    "concept_id in unnest(${childConceptIds})\n" +
      "${ageDateAndEncounterSql}";

  private static final String GROUP_CODE_LIKE_TEMPLATE =
    "concept_id in (\n" +
    "   select a.concept_id from\n" +
    "   `${projectId}.${dataSetId}.criteria` a\n" +
    "    join (select CONCAT( '%.', CAST(id as STRING), '%') as path " +
    "    from `${projectId}.${dataSetId}.criteria` " +
    "    where concept_id in unnest(${parentConceptIds})) b \n" +
    "    on a.path like b.path\n" +
    "    and is_group = 0\n" +
    "    and is_selectable = 1\n" +
    "    and type = 'DRUG'\n" +
    "    and subtype = 'ATC')\n${ageDateAndEncounterSql}";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           boolean temporal) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    ListMultimap<String, Long> paramMap = getMappedParameters(searchGroupItem.getSearchParameters());
    List<String> queryParts = new ArrayList<>();
    for (String key : paramMap.keySet()) {
      Long[] conceptIds = paramMap.get(key).stream().toArray(Long[]::new);
      String namedParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(conceptIds, Long.class));
      if ("Children".equals(key)) {
        String drugSql = buildModifierSql(DRUG_SQL_TEMPLATE + CHILD_IN_CLAUSE_TEMPLATE
            .replace("${childConceptIds}", "@" + namedParameter),
          queryParams, searchGroupItem.getModifiers());
        if (temporal) {
          String temporalSql = buildTemporalSql(CHILD_IN_CLAUSE_TEMPLATE, "search_drug", drugSql, queryParams, searchGroupItem.getModifiers());
          queryParts.add(temporalSql.replace("${childConceptIds}", "@" + namedParameter));
        } else {
          queryParts.add(drugSql);
        }
      } else {
        String drugSql = buildModifierSql(DRUG_SQL_TEMPLATE + GROUP_CODE_LIKE_TEMPLATE
            .replace("${parentConceptIds}", "@" + namedParameter),
          queryParams, searchGroupItem.getModifiers());
        if (temporal) {
          String temporalSql = buildTemporalSql(GROUP_CODE_LIKE_TEMPLATE, "search_drug", drugSql, queryParams, searchGroupItem.getModifiers());
          queryParts.add(temporalSql.replace("${childConceptIds}", "@" + namedParameter));
        } else {
          queryParts.add(drugSql);
        }
      }
    }

    return String.join(UNION_TEMPLATE, queryParts);
  }

  private ListMultimap<String, Long> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<String, Long> fullMap = ArrayListMultimap.create();
    searchParameters
      .stream()
      .forEach(param -> {
        validateSearchParameter(param);
        if (param.getGroup()) {
          fullMap.put("Parents", param.getConceptId());
        } else {
          fullMap.put("Children", param.getConceptId());
        }
      });
    return fullMap;
  }

  private void validateSearchParameter(SearchParameter param) {
    from(typeBlank().or(drugTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(conceptIdNull()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.DRUG;
  }
}
