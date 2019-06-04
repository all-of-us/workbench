package org.pmiops.workbench.cohortbuilder.querybuilder;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.codeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.codeSubtypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.codeTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.paramChild;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.paramParent;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.subtypeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeICD;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CODE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SUBTYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TreeType;
import org.springframework.stereotype.Service;

/**
 * CodesQueryBuilder builds SQL for BigQuery for the following criteria types: ICD9, ICD10, CPT and
 * SNOMED.
 */
@Service
public class CodesQueryBuilder extends AbstractQueryBuilder {

  private static Set<String> SOURCE_TREES =
      ImmutableSet.of(TreeType.ICD9.toString(), TreeType.ICD10.toString(), TreeType.CPT.toString());

  private static final String CODES_SQL_TEMPLATE =
      "select distinct person_id, entry_date, concept_id\n"
          + "from `${projectId}.${dataSetId}."
          + TABLE_ID
          + "`\n"
          + "where ";

  private static final String MULTIPLE_TEMPLATE =
      "concept_id in (select concept_id\n"
          + "  from `${projectId}.${dataSetId}.criteria`\n"
          + "  where ${innerParentAndChildSql}"
          + ")\n"
          + AGE_DATE_AND_ENCOUNTER_VAR;

  private static final String PARENT_TEMPLATE =
      "(type = ${type}\n"
          + "  and subtype = ${subtype}\n"
          + "  and REGEXP_CONTAINS(code, ${code})\n"
          + "  and is_selectable = 1\n"
          + "  and concept_id is not null)\n";

  private static final String CHILD_TEMPLATE = "(concept_id in unnest(${conceptIds}))\n";

  private static final String CHILD_ONLY_TEMPLATE =
      "concept_id in unnest(${conceptIds})\n" + AGE_DATE_AND_ENCOUNTER_VAR;

  private static final String IS_STANDARD = " and is_standard = 1\n";
  private static final String IS_NOT_STANDARD = " and is_standard = 0\n";

  /** {@inheritDoc} */
  @Override
  public String buildQuery(
      Map<String, QueryParameterValue> queryParams,
      SearchGroupItem searchGroupItem,
      TemporalMention mention) {
    List<SearchParameter> parameters = searchGroupItem.getSearchParameters();
    from(parametersEmpty()).test(parameters).throwException(EMPTY_MESSAGE, PARAMETERS);

    ListMultimap<MultiKey, SearchParameter> paramMap = getMappedParameters(parameters);
    List<String> queryParts = new ArrayList<String>();
    String conceptIdsNamedParameter = "";
    boolean isStandard = false;

    for (MultiKey key : paramMap.keySet()) {
      final List<SearchParameter> paramList = paramMap.get(key);
      final SearchParameter parameter = paramList.get(0);
      isStandard = !SOURCE_TREES.contains(parameter.getType());
      if (key.getType().equals(CHILD)) {
        List<Long> conceptIds = new ArrayList<>();
        conceptIds.addAll(
            parameters.stream().map(SearchParameter::getConceptId).collect(Collectors.toList()));
        QueryParameterValue cids =
            QueryParameterValue.array(conceptIds.stream().toArray(Long[]::new), Long.class);
        conceptIdsNamedParameter = addQueryParameterValue(queryParams, cids);
        String sqlPart = CHILD_TEMPLATE.replace("${conceptIds}", "@" + conceptIdsNamedParameter);
        queryParts.add(sqlPart);
      } else {
        List<String> codes = new ArrayList<>();
        codes.addAll(
            paramList.stream().map(SearchParameter::getValue).collect(Collectors.toList()));
        String codeParam = "^(" + String.join("|", codes) + ")";
        String codeNamedParameter =
            addQueryParameterValue(queryParams, QueryParameterValue.string(codeParam));
        String typeNamedParameter =
            addQueryParameterValue(queryParams, QueryParameterValue.string(parameter.getType()));
        String subtypeNamedParameter =
            addQueryParameterValue(queryParams, QueryParameterValue.string(parameter.getSubtype()));
        String sqlPart =
            PARENT_TEMPLATE
                .replace("${code}", "@" + codeNamedParameter)
                .replace("${type}", "@" + typeNamedParameter)
                .replace("${subtype}", "@" + subtypeNamedParameter);
        queryParts.add(sqlPart);
      }
    }

    String standardSql = (isStandard) ? IS_STANDARD : IS_NOT_STANDARD;
    String bodySql =
        queryParts.size() == 1 && !conceptIdsNamedParameter.isEmpty()
            ? CHILD_ONLY_TEMPLATE.replace("${conceptIds}", "@" + conceptIdsNamedParameter)
            : MULTIPLE_TEMPLATE.replace("${innerParentAndChildSql}", String.join(OR, queryParts));
    String baseSql = CODES_SQL_TEMPLATE + bodySql + standardSql;
    String modifiedSql = buildModifierSql(baseSql, queryParams, searchGroupItem.getModifiers());
    return buildTemporalSql(
        modifiedSql, bodySql + standardSql, queryParams, searchGroupItem.getModifiers(), mention);
  }

  private void validateSearchParameter(SearchParameter param) {
    from(typeBlank().or(codeTypeInvalid()))
        .test(param)
        .throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(typeICD().and(subtypeBlank().or(codeSubtypeInvalid())))
        .test(param)
        .throwException(NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, param.getSubtype());
    from(paramChild().and(conceptIdNull()))
        .test(param)
        .throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
    from(paramParent().and(codeBlank()))
        .test(param)
        .throwException(NOT_VALID_MESSAGE, PARAMETER, CODE, param.getValue());
  }

  /** {@inheritDoc} */
  @Override
  public FactoryKey getType() {
    return FactoryKey.CODES;
  }

  protected ListMultimap<MultiKey, SearchParameter> getMappedParameters(
      List<SearchParameter> searchParameters) {
    ListMultimap<MultiKey, SearchParameter> fullMap = ArrayListMultimap.create();
    searchParameters.forEach(
        param -> {
          validateSearchParameter(param);
          fullMap.put(new MultiKey(param), param);
        });
    return fullMap;
  }

  public class MultiKey {
    private String group;
    private String type;

    public MultiKey(SearchParameter searchParameter) {
      this.group = searchParameter.getGroup() ? PARENT : CHILD;
      this.type = searchParameter.getGroup() ? searchParameter.getType() : CHILD;
    }

    public String getKey() {
      return this.group + this.type;
    }

    public String getGroup() {
      return this.group;
    }

    public String getType() {
      return this.type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MultiKey multiKey = (MultiKey) o;
      return Objects.equals(group, multiKey.group) && Objects.equals(type, multiKey.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, type);
    }
  }
}
