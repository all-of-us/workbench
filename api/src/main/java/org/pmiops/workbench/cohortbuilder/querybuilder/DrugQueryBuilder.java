package org.pmiops.workbench.cohortbuilder.querybuilder;

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

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.springframework.stereotype.Service;

/** DrugQueryBuilder builds SQL for BigQuery for drug criteria type. */
@Service
public class DrugQueryBuilder extends AbstractQueryBuilder {

  private static final String DRUG_SQL_TEMPLATE =
      "select distinct person_id, entry_date, concept_id\n"
          + "from `${projectId}.${dataSetId}."
          + TABLE_ID
          + "`\n"
          + "where concept_id in (select descendant_id\n"
          + "from `${projectId}.${dataSetId}.criteria_ancestor`\n"
          + "where ancestor_id in ${innerSql})\n";

  private static final String CHILD_ONLY_TEMPLATE = "unnest(${childConceptIds})\n";

  private static final String PARENT_CRITERIA =
      "select a.concept_id from\n"
          + "`${projectId}.${dataSetId}.criteria` a\n"
          + "join (select CONCAT( '%.', CAST(id as STRING), '%') as path\n"
          + "from `${projectId}.${dataSetId}.criteria`\n"
          + "where concept_id in unnest(${parentConceptIds})) b\n"
          + "on a.path like b.path\n"
          + "and is_group = 0\n"
          + "and is_selectable = 1\n"
          + "and type = 'DRUG'\n"
          + "and subtype = 'ATC'";

  private static final String BOTH_TEMPLATE =
      "(" + PARENT_CRITERIA + " or\n" + "concept_id in " + CHILD_ONLY_TEMPLATE + ")\n";

  private static final String PARENT_ONLY_TEMPLATE = "(" + PARENT_CRITERIA + ")\n";

  private static final String IS_STANDARD = " and is_standard = 1\n";

  /** {@inheritDoc} */
  @Override
  public String buildQuery(
      Map<String, QueryParameterValue> queryParams,
      SearchGroupItem searchGroupItem,
      TemporalMention mention) {
    from(parametersEmpty())
        .test(searchGroupItem.getSearchParameters())
        .throwException(EMPTY_MESSAGE, PARAMETERS);
    ListMultimap<String, Long> paramMap =
        getMappedParameters(searchGroupItem.getSearchParameters());
    String baseSql = DRUG_SQL_TEMPLATE;
    String conceptIdSql = "";

    // Parent and child nodes generate different sql statements
    // Parent nodes match the parent id in the path of the children and use the child conceptId
    // Child nodes are looked up by conceptId
    Long[] parentIds = paramMap.get(PARENT).stream().toArray(Long[]::new);
    Long[] childIds = paramMap.get(CHILD).stream().toArray(Long[]::new);
    if (Arrays.asList(CHILD).containsAll(paramMap.keySet())) {
      String childParameter =
          addQueryParameterValue(queryParams, QueryParameterValue.array(childIds, Long.class));
      conceptIdSql =
          "concept_id in "
              + CHILD_ONLY_TEMPLATE.replace("${childConceptIds}", "@" + childParameter);
      baseSql =
          baseSql.replace(
              "${innerSql}",
              CHILD_ONLY_TEMPLATE.replace("${childConceptIds}", "@" + childParameter));
    } else if (Arrays.asList(PARENT).containsAll(paramMap.keySet())) {
      String parentParameter =
          addQueryParameterValue(queryParams, QueryParameterValue.array(parentIds, Long.class));
      conceptIdSql =
          "concept_id in "
              + PARENT_ONLY_TEMPLATE.replace("${parentConceptIds}", "@" + parentParameter);
      baseSql =
          baseSql.replace(
              "${innerSql}",
              PARENT_ONLY_TEMPLATE.replace("${parentConceptIds}", "@" + parentParameter));
    } else {
      String childParameter =
          addQueryParameterValue(queryParams, QueryParameterValue.array(childIds, Long.class));
      String parentParameter =
          addQueryParameterValue(queryParams, QueryParameterValue.array(parentIds, Long.class));
      conceptIdSql =
          "concept_id in "
              + BOTH_TEMPLATE
                  .replace("${parentConceptIds}", "@" + parentParameter)
                  .replace("${childConceptIds}", "@" + childParameter);
      baseSql =
          baseSql.replace(
              "${innerSql}",
              BOTH_TEMPLATE
                  .replace("${parentConceptIds}", "@" + parentParameter)
                  .replace("${childConceptIds}", "@" + childParameter));
    }
    baseSql = baseSql + AGE_DATE_AND_ENCOUNTER_VAR + IS_STANDARD;
    List<Modifier> modifiers = searchGroupItem.getModifiers();
    String modifiedSql = buildModifierSql(baseSql, queryParams, modifiers);
    return buildTemporalSql(
        modifiedSql, conceptIdSql + IS_STANDARD, queryParams, modifiers, mention);
  }

  private ListMultimap<String, Long> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<String, Long> fullMap = ArrayListMultimap.create();
    searchParameters.stream()
        .forEach(
            param -> {
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
    from(typeBlank().or(drugTypeInvalid()))
        .test(param)
        .throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(conceptIdNull())
        .test(param)
        .throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
  }

  /** {@inheritDoc} */
  @Override
  public FactoryKey getType() {
    return FactoryKey.DRUG;
  }
}
