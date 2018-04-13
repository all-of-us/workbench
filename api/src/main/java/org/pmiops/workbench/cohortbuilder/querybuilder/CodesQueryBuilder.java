package org.pmiops.workbench.cohortbuilder.querybuilder;
//org.pmiops.workbench.api.cohortbuilder.querybuilder

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.UnmodifiableIterator;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CodesQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the following criteria types:
 * ICD9, ICD10 and CPT.
 */
@Service
public class CodesQueryBuilder extends AbstractQueryBuilder {

  private static final ImmutableMap<String, String> TYPE_PROC =
    ImmutableMap.of("ICD9", "ICD9Proc", "CPT", "CPT4");

  private static final ImmutableMap<String, String> TYPE_CM =
    ImmutableMap.of("ICD9", "ICD9CM", "CPT", "CPT4");

  public static final String ICD_10 = "ICD10";

  public enum GroupType {
    GROUP, NOT_GROUP
  }

  private static final String INNER_SQL_TEMPLATE =
    "select distinct a.person_id, a.${entryDate} as entry_date, b.concept_code\n" +
      "from `${projectId}.${dataSetId}.${tableName}` a, `${projectId}.${dataSetId}.concept` b\n" +
      "where a.${tableId} = b.concept_id\n";

  private static final String ICD9_VOCABULARY_ID_IN_CLAUSE_TEMPLATE = "and b.vocabulary_id in (${cm},${proc})\n";

  private static final String ICD10_VOCABULARY_ID_IN_CLAUSE_TEMPLATE = "and b.vocabulary_id = ${cmOrProc}\n";

  private static final String CHILD_CODE_IN_CLAUSE_TEMPLATE = "and b.concept_code in unnest(${conceptCodes})\n";

  private static final String GROUP_CODE_LIKE_TEMPLATE = "and b.concept_code like ${conceptCodes}\n";

  private static final String UNION_TEMPLATE = " union all\n";

  private static final String OUTER_SQL_TEMPLATE = "select person_id\n" +
    "from `${projectId}.${dataSetId}.person` p\n" +
    "where person_id in (${middleSqlTemplate})\n";

  private static final String MIDDLE_SQL_TEMPLATE =
    "select criteria.person_id from (${innerSqlTemplate}) criteria\n" +
      "join `${projectId}.${dataSetId}.person` p on (criteria.person_id = p.person_id)\n";

  private static final String AGE_AT_EVENT_SQL_TEMPLATE =
    "CAST(FLOOR(DATE_DIFF(criteria.entry_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64)\n";

  private static final String EVENT_DATE_SQL_TEMPLATE = "criteria.entry_date\n";

  private static final String OCCURRENCES_SQL_TEMPLATE = "group by criteria.person_id\n" +
    "having count(criteria.person_id)";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters params) {
    Map<GroupType, ListMultimap<String, SearchParameter>> paramMap = getMappedParameters(params.getParameters());
    List<String> queryParts = new ArrayList<String>();
    List<String> modifierQueryParts = new ArrayList<String>();
    String groupByModifier = "";
    Map<String, QueryParameterValue> queryParams = new HashMap<>();

    for (GroupType group : paramMap.keySet()) {
      ListMultimap<String, SearchParameter> domainMap = paramMap.get(group);

      for (String domain : domainMap.keySet()) {
        final List<SearchParameter> searchParameterList = domainMap.get(domain);
        final SearchParameter parameter = searchParameterList.get(0);
        final String type = parameter.getType();
        final String subtype = parameter.getSubtype();
        List<String> codes =
          searchParameterList.stream().map(SearchParameter::getValue).collect(Collectors.toList());
        if (group.equals(GroupType.NOT_GROUP)) {
          buildNotGroupQuery(type, subtype, queryParts, queryParams, domain, codes);
        } else {
          buildGroupQuery(type, subtype, queryParts, queryParams, domain, codes);
        }
      }
    }

    for (Modifier modifier : params.getModifiers()) {
      if (modifier.getName().equals(ModifierType.AGE_AT_EVENT)) {
        String ageAtEventParameter = "age" + getUniqueNamedParameterPostfix();
        modifierQueryParts.add(AGE_AT_EVENT_SQL_TEMPLATE + " "
          + OperatorUtils.getSqlOperator(modifier.getOperator()) + " @" + ageAtEventParameter + "\n");
        queryParams.put(ageAtEventParameter,
          QueryParameterValue.int64(new Long(modifier.getOperands().get(0))));
      } else if (modifier.getName().equals(ModifierType.NUM_OF_OCCURRENCES)) {
        String occurrencesParameter = "occ" + getUniqueNamedParameterPostfix();
        groupByModifier = OCCURRENCES_SQL_TEMPLATE + " "
          + OperatorUtils.getSqlOperator(modifier.getOperator()) + " @" + occurrencesParameter + "\n";
        queryParams.put(occurrencesParameter,
          QueryParameterValue.int64(new Long(modifier.getOperands().get(0))));
      } else if (modifier.getName().equals(ModifierType.EVENT_DATE)) {
        String eventParameter = "event" + getUniqueNamedParameterPostfix();
        modifierQueryParts.add(EVENT_DATE_SQL_TEMPLATE + " "
          + OperatorUtils.getSqlOperator(modifier.getOperator()) + " @" + eventParameter + "\n");
        queryParams.put(eventParameter,
          QueryParameterValue.date(modifier.getOperands().get(0)));
      }
    }
    String middleSql = MIDDLE_SQL_TEMPLATE.replace("${innerSqlTemplate}", String.join(UNION_TEMPLATE, queryParts));
    if (!modifierQueryParts.isEmpty()) {
      middleSql = middleSql + " where " + String.join(" and ", modifierQueryParts);
    }
    if (!groupByModifier.isEmpty()) {
      middleSql = middleSql + groupByModifier;
    }
    String finalSql = OUTER_SQL_TEMPLATE.replace("${middleSqlTemplate}", middleSql);

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
    if (type.equals(ICD_10)) {
      queryParams.put(cmOrProcUniqueParam, QueryParameterValue.string(subtype));
      inClauseSql = ICD10_VOCABULARY_ID_IN_CLAUSE_TEMPLATE;
      paramNames = new ImmutableMap.Builder<String, String>()
        .put("${tableName}", DomainTableEnum.getTableName(domain))
        .put("${tableId}", DomainTableEnum.getSourceConceptId(domain))
        .put("${entryDate}", DomainTableEnum.getEntryDate(domain))
        .put("${conceptCodes}", "@" + namedParameter)
        .put("${cmOrProc}", "@" + cmOrProcUniqueParam)
        .build();
    } else {
      queryParams.put(cmUniqueParam, QueryParameterValue.string(TYPE_CM.get(type)));
      queryParams.put(procUniqueParam, QueryParameterValue.string(TYPE_PROC.get(type)));
      inClauseSql = ICD9_VOCABULARY_ID_IN_CLAUSE_TEMPLATE;
      paramNames = new ImmutableMap.Builder<String, String>()
        .put("${tableName}", DomainTableEnum.getTableName(domain))
        .put("${tableId}", DomainTableEnum.getSourceConceptId(domain))
        .put("${entryDate}", DomainTableEnum.getEntryDate(domain))
        .put("${conceptCodes}", "@" + namedParameter)
        .put("${cm}", "@" + cmUniqueParam)
        .put("${proc}", "@" + procUniqueParam)
        .build();
    }

    queryParts.add(filterSql(INNER_SQL_TEMPLATE + inClauseSql +
      groupOrChildSql, paramNames));
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.CODES;
  }

  protected Map<GroupType, ListMultimap<String, SearchParameter>> getMappedParameters(List<SearchParameter> searchParameters) {
    Map<GroupType, ListMultimap<String, SearchParameter>> fullMap = new LinkedHashMap<>();
    ListMultimap<String, SearchParameter> groupParameters = ArrayListMultimap.create();
    ListMultimap<String, SearchParameter> notGroupParameters = ArrayListMultimap.create();
    for (SearchParameter parameter : searchParameters) {
      if (parameter.getGroup()) {
        groupParameters.put(parameter.getDomain(), parameter);
      } else {
        notGroupParameters.put(parameter.getDomain(), parameter);
      }
    }
    if (!groupParameters.isEmpty()) {
      fullMap.put(GroupType.GROUP, groupParameters);
    }
    if (!notGroupParameters.isEmpty()) {
      fullMap.put(GroupType.NOT_GROUP, notGroupParameters);
    }
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
}
