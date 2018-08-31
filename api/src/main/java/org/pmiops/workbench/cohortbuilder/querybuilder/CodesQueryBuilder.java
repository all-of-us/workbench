package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.UnmodifiableIterator;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * CodesQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the following criteria types:
 * ICD9, ICD10 and CPT.
 */
@Service
public class CodesQueryBuilder extends AbstractQueryBuilder {

  private static final String GROUP = "group";
  private static final String NOT_GROUP = "notGroup";

  private static final ImmutableMap<String, String> TYPE_PROC =
    ImmutableMap.of(TreeType.ICD9.name(), "ICD9Proc", TreeType.CPT.name(), "CPT4");

  private static final ImmutableMap<String, String> TYPE_CM =
    ImmutableMap.of(TreeType.ICD9.name(), "ICD9CM", TreeType.CPT.name(), "CPT4");

  //If the querybuilder will use modifiers then this sql statement has to have
  //the distinct and ${modifierColumns}
  private static final String CODES_SQL_TEMPLATE =
    "select distinct a.person_id, ${modifierColumns}\n" +
      "from `${projectId}.${dataSetId}.${tableName}` a, `${projectId}.${dataSetId}.concept` b\n" +
      "where a.${tableId} = b.concept_id\n";

  private static final String ICD9_VOCABULARY_ID_IN_CLAUSE_TEMPLATE =
    "and b.vocabulary_id in (${cm},${proc})\n";

  private static final String ICD10_VOCABULARY_ID_IN_CLAUSE_TEMPLATE =
    "and b.vocabulary_id = ${cmOrProc}\n";

  private static final String CHILD_CODE_IN_CLAUSE_TEMPLATE =
    "and b.concept_code in unnest(${conceptCodes})\n" +
      "${encounterSql}";

  private static final String GROUP_CODE_LIKE_TEMPLATE =
    "and b.concept_code like ${conceptCodes}\n" +
      "${encounterSql}";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters params) {
    ListMultimap<MultiKey, SearchParameter> paramMap = getMappedParameters(params.getParameters());
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();

    for (MultiKey key : paramMap.keySet()) {
        final List<SearchParameter> searchParameterList = paramMap.get(key);
        final SearchParameter parameter = searchParameterList.get(0);
        List<String> codes =
          searchParameterList.stream().map(SearchParameter::getValue).collect(Collectors.toList());
        if (key.getKey().contains(NOT_GROUP)) {
          buildNotGroupQuery(parameter.getType(),
            parameter.getSubtype(),
            queryParts,
            queryParams,
            parameter.getDomain(),
            codes);
        } else {
          buildGroupQuery(parameter.getType(),
            parameter.getSubtype(),
            queryParts,
            queryParams,
            parameter.getDomain(),
            codes);
        }
    }

    String codesSql = String.join(UNION_TEMPLATE, queryParts);
    String finalSql = buildModifierSql(codesSql, queryParams, params.getModifiers());

    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  private void buildGroupQuery(String type,
                               String subtype,
                               List<String> queryParts,
                               Map<String, QueryParameterValue> queryParams,
                               String domain, List<String> codes) {
    for (String code : codes) {
      buildInnerQuery(type,
        subtype,
        queryParts,
        queryParams,
        domain,
        QueryParameterValue.string(code + "%"),
        GROUP_CODE_LIKE_TEMPLATE);
    }
  }

  private void buildNotGroupQuery(String type,
                                  String subtype,
                                  List<String> queryParts,
                                  Map<String, QueryParameterValue> queryParams,
                                  String domain, List<String> codes) {
    buildInnerQuery(type,
      subtype,
      queryParts,
      queryParams,
      domain,
      QueryParameterValue.array(codes.stream().toArray(String[]::new), String.class),
      CHILD_CODE_IN_CLAUSE_TEMPLATE);
  }

  private void buildInnerQuery(String type,
                               String subtype,
                               List<String> queryParts,
                               Map<String, QueryParameterValue> queryParams,
                               String domain, QueryParameterValue codes,
                               String groupOrChildSql) {
    String inClauseSql = "";
    ImmutableMap paramNames = null;
    final String uniqueName = getUniqueNamedParameterPostfix();
    final String cmUniqueParam = "cm" + uniqueName;
    final String procUniqueParam = "proc" + uniqueName;
    final String cmOrProcUniqueParam = "cmOrProc" + uniqueName;
    final String namedParameter = domain + uniqueName;

    queryParams.put(namedParameter, codes);
    if (type.equals(TreeType.ICD10.name())) {
      queryParams.put(cmOrProcUniqueParam, QueryParameterValue.string(subtype));
      inClauseSql = ICD10_VOCABULARY_ID_IN_CLAUSE_TEMPLATE;
      paramNames = new ImmutableMap.Builder<String, String>()
        .put("${tableName}", DomainTableEnum.getTableName(domain))
        .put("${modifierColumns}", DomainTableEnum.getEntryDate(domain) + " as entry_date, concept_code")
        .put("${tableId}", DomainTableEnum.getSourceConceptId(domain))
        .put("${conceptCodes}", "@" + namedParameter)
        .put("${cmOrProc}", "@" + cmOrProcUniqueParam)
        .build();
    } else {
      queryParams.put(cmUniqueParam, QueryParameterValue.string(TYPE_CM.get(type)));
      queryParams.put(procUniqueParam, QueryParameterValue.string(TYPE_PROC.get(type)));
      inClauseSql = ICD9_VOCABULARY_ID_IN_CLAUSE_TEMPLATE;
      paramNames = new ImmutableMap.Builder<String, String>()
        .put("${tableName}", DomainTableEnum.getTableName(domain))
        .put("${modifierColumns}", DomainTableEnum.getEntryDate(domain) + " as entry_date, concept_code")
        .put("${tableId}", DomainTableEnum.getSourceConceptId(domain))
        .put("${conceptCodes}", "@" + namedParameter)
        .put("${cm}", "@" + cmUniqueParam)
        .put("${proc}", "@" + procUniqueParam)
        .build();
    }

    queryParts.add(filterSql(CODES_SQL_TEMPLATE + inClauseSql +
      groupOrChildSql, paramNames));
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.CODES;
  }

  protected ListMultimap<MultiKey, SearchParameter> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<MultiKey, SearchParameter> fullMap = ArrayListMultimap.create();
    searchParameters
      .forEach(param -> {
          fullMap.put(new MultiKey(param), param);
        }
      );
    return fullMap;
  }

  private String filterSql(String sqlStatement, ImmutableMap replacements) {
    String returnSql = sqlStatement;
    for (UnmodifiableIterator iterator = replacements.keySet().iterator(); iterator.hasNext(); ) {
      String key = (String) iterator.next();
      returnSql = returnSql.replace(key, replacements.get(key).toString());
    }
    return returnSql;

  }

  public class MultiKey {
    private String group;
    private String type;
    private String domain;

    public MultiKey(SearchParameter searchParameter) {
      this.group = searchParameter.getGroup() ? GROUP : NOT_GROUP;
      this.type = searchParameter.getType();
      this.domain = searchParameter.getDomain();
    }

    public String getKey() {
      return this.group + this.type + this.domain;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MultiKey multiKey = (MultiKey) o;
      return Objects.equals(group, multiKey.group) &&
        Objects.equals(type, multiKey.type) &&
        Objects.equals(domain, multiKey.domain);
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, type, domain);
    }
  }
}
