package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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

  private static final String CHILD_ONLY_TEMPLATE =
    "concept_id in unnest(${childConceptIds})\n";

  private static final String PARENT_CRITERIA = "select a.concept_id from\n" +
    "`${projectId}.${dataSetId}.criteria` a\n" +
    "join (select CONCAT( '%.', CAST(id as STRING), '%') as path\n" +
    "from `${projectId}.${dataSetId}.criteria`\n" +
    "where concept_id in unnest(${parentConceptIds})) b\n" +
    "on a.path like b.path\n" +
    "and is_group = 0\n" +
    "and is_selectable = 1\n" +
    "and type = 'DRUG'\n" +
    "and subtype = 'ATC'";

  private static final String BOTH_TEMPLATE =
    "concept_id in (" + PARENT_CRITERIA + " or\n" +
      CHILD_ONLY_TEMPLATE + ")\n";

  private static final String PARENT_ONLY_TEMPLATE =
    "concept_id in (" + PARENT_CRITERIA + ")\n";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           TemporalMention mention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    ListMultimap<String, Long> paramMap = getMappedParameters(searchGroupItem.getSearchParameters());
    StringBuilder baseSql = new StringBuilder(DRUG_SQL_TEMPLATE);
    StringBuilder conceptIdSql = new StringBuilder();

    //Parent and child nodes generate different sql statements
    //Parent nodes match the parent id in the path of the children and use the child conceptId
    //Child nodes are looked up by conceptId
    Long[] parentIds = paramMap.get(PARENT).stream().toArray(Long[]::new);
    Long[] childIds = paramMap.get(CHILD).stream().toArray(Long[]::new);
    if (Arrays.asList(CHILD).containsAll(paramMap.keySet())) {
      String childParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(childIds, Long.class));
      conceptIdSql.append(CHILD_ONLY_TEMPLATE.replace("${childConceptIds}", "@" + childParameter));
      baseSql.append(conceptIdSql.toString());
    } else if (Arrays.asList(PARENT).containsAll(paramMap.keySet())) {
      String parentParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(parentIds, Long.class));
      conceptIdSql.append(PARENT_ONLY_TEMPLATE.replace("${parentConceptIds}", "@" + parentParameter));
      baseSql.append(conceptIdSql.toString());
    } else {
      String childParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(childIds, Long.class));
      String parentParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(parentIds, Long.class));
      conceptIdSql.append(BOTH_TEMPLATE.replace("${parentConceptIds}", "@" + parentParameter)
        .replace("${childConceptIds}", "@" + childParameter));
      baseSql.append(conceptIdSql.toString());
    }
    baseSql.append(AGE_DATE_AND_ENCOUNTER_VAR);
    List<Modifier> modifiers = searchGroupItem.getModifiers();
    String modifiedSql = buildModifierSql(baseSql.toString(), queryParams, modifiers);
    return buildTemporalSql(TABLE_ID, modifiedSql, queryParams, modifiers, mention);
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
