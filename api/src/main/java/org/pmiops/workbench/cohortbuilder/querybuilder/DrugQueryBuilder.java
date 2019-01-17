package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
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

  private static final String TABLE_ID = "search_drug";
  private static final String DRUG_SQL_TEMPLATE =
    "select person_id, entry_date, concept_id\n" +
      "from `${projectId}.${dataSetId}." + TABLE_ID + "`\n" +
      "where ";

  private static final String CONCEPT_ID_IN_TEMPLATE =
    "concept_id in (\n" +
      "${innerSql})\n";

  private static final String CHILD_TEMPLATE =
    "concept_id in unnest(${childConceptIds})\n";

  private static final String PARENT_TEMPLATE =
    "select a.concept_id from\n" +
    "`${projectId}.${dataSetId}.criteria` a\n" +
    "join (select CONCAT( '%.', CAST(id as STRING), '%') as path\n" +
    "from `${projectId}.${dataSetId}.criteria`\n" +
    "where concept_id in unnest(${parentConceptIds})) b\n" +
    "on a.path like b.path\n" +
    "and is_group = 0\n" +
    "and is_selectable = 1\n" +
    "and type = 'DRUG'\n" +
    "and subtype = 'ATC'\n";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           String mention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    ListMultimap<String, Long> paramMap = getMappedParameters(searchGroupItem.getSearchParameters());
    String childSql = "";
    String parentSql = "";
    for (String key : paramMap.keySet()) {
      Long[] conceptIds = paramMap.get(key).stream().toArray(Long[]::new);
      String namedParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(conceptIds, Long.class));
      if (CHILD.equals(key)) {
       childSql = CHILD_TEMPLATE.replace("${childConceptIds}", "@" + namedParameter);
      } else {
       parentSql = PARENT_TEMPLATE.replace("${parentConceptIds}", "@" + namedParameter);
      }
    }
    List<Modifier> modifiers = searchGroupItem.getModifiers();
    String conceptIdSql = "";
    if (childSql.isEmpty()) {
      conceptIdSql = CONCEPT_ID_IN_TEMPLATE.replace("${innerSql}", parentSql);
    } else if (parentSql.isEmpty()) {
      conceptIdSql = childSql;
    } else {
      conceptIdSql = CONCEPT_ID_IN_TEMPLATE.replace("${innerSql}", parentSql + OR + childSql);
    }
    String baseSql = DRUG_SQL_TEMPLATE + conceptIdSql + AGE_DATE_AND_ENCOUNTER_VAR;
    String modifiedSql = buildModifierSql(baseSql, queryParams, modifiers);
    return buildTemporalSql(TABLE_ID, modifiedSql, conceptIdSql, queryParams, modifiers, mention);
  }

  private ListMultimap<String, Long> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<String, Long> fullMap = ArrayListMultimap.create();
    searchParameters
      .stream()
      .forEach(param -> {
        validateSearchParameter(param);
        if (param.getGroup()) {
          fullMap.put(PARENT, param.getConceptId());
        } else {
          fullMap.put(CHILD, param.getConceptId());
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
