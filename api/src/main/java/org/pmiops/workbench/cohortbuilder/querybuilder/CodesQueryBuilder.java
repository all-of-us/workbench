package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.UnmodifiableIterator;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.*;
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

  public enum GroupType {
    GROUP, NOT_GROUP
  }

  private static final List<Operator> MODIFIER_OPERATORS =
    Arrays.asList(Operator.BETWEEN, Operator.GREATER_THAN_OR_EQUAL_TO,
      Operator.LESS_THAN_OR_EQUAL_TO);

  private static final List<ModifierType> INCLUDED_MODIFIER_TYPES =
    Arrays.asList(ModifierType.AGE_AT_EVENT, ModifierType.EVENT_DATE);

  public static final String ICD_10 = "ICD10";
  public static final String WHERE = " where ";
  public static final String AND = " and ";
  public static final String DISTINCT = "distinct";
  public static final String AGE_AT_EVENT_PREFIX = "age";
  public static final String EVENT_DATE_PREFIX = "event";
  public static final String OCCURRENCES_PREFIX = "occ";

  private static final String INNER_SQL_TEMPLATE =
    "select ${modifierDistinct} a.person_id ${modifierColumns}\n" +
      "from `${projectId}.${dataSetId}.${tableName}` a, `${projectId}.${dataSetId}.concept` b\n" +
      "where a.${tableId} = b.concept_id\n";

  private static final String ICD9_VOCABULARY_ID_IN_CLAUSE_TEMPLATE = "and b.vocabulary_id in (${cm},${proc})\n";

  private static final String ICD10_VOCABULARY_ID_IN_CLAUSE_TEMPLATE = "and b.vocabulary_id = ${cmOrProc}\n";

  private static final String CHILD_CODE_IN_CLAUSE_TEMPLATE = "and b.concept_code in unnest(${conceptCodes})\n";

  private static final String GROUP_CODE_LIKE_TEMPLATE = "and b.concept_code like ${conceptCodes}\n";

  private static final String UNION_TEMPLATE = " union all\n";

  private static final String OUTER_SQL_TEMPLATE = "select person_id\n" +
    "from `${projectId}.${dataSetId}.person` p\n" +
    "where person_id in (${innerSqlTemplate})\n";

  private static final String MODIFIER_COLUMNS_TEMPLATE = ", a.${entryDate} as entry_date, b.concept_code";

  private static final String MODIFIER_SQL_TEMPLATE =
    "select criteria.person_id from (${innerSqlTemplate}) criteria\n" +
      "join `${projectId}.${dataSetId}.person` p on (criteria.person_id = p.person_id)\n";

  private static final String AGE_AT_EVENT_SQL_TEMPLATE =
    "CAST(FLOOR(DATE_DIFF(criteria.entry_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64)\n";

  private static final String EVENT_DATE_SQL_TEMPLATE = "criteria.entry_date\n";

  private static final String OCCURRENCES_SQL_TEMPLATE = "group by criteria.person_id\n" +
    "having count(criteria.person_id)";

  @Override
  public FactoryKey getType() {
    return FactoryKey.CODES;
  }

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters params) {
    Map<GroupType, ListMultimap<String, SearchParameter>> paramMap = getMappedParameters(params.getParameters());
    List<String> queryParts = new ArrayList<String>();
    List<String> modifierQueryParts = new ArrayList<String>();
    List<String> groupByModifier = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();

    for (GroupType group : paramMap.keySet()) {
      ListMultimap<String, SearchParameter> domainMap = paramMap.get(group);

      for (String domain : domainMap.keySet()) {
        final List<SearchParameter> searchParameterList = domainMap.get(domain);
        final SearchParameter parameter = searchParameterList.get(0);
        final boolean modifiersExist = !params.getModifiers().isEmpty();
        List<String> codes =
          searchParameterList.stream().map(SearchParameter::getValue).collect(Collectors.toList());
        if (group.equals(GroupType.NOT_GROUP)) {
          buildNotGroupQuery(parameter, queryParts, queryParams, codes, modifiersExist);
        } else {
          buildGroupQuery(parameter, queryParts, queryParams, codes, modifiersExist);
        }
      }
    }

    for (Modifier modifier : params.getModifiers()) {
      buildModifierQuery(modifier, modifierQueryParts, groupByModifier, queryParams);
    }
    String finalSql = params.getModifiers().isEmpty() ? OUTER_SQL_TEMPLATE : MODIFIER_SQL_TEMPLATE;
    finalSql = finalSql.replace("${innerSqlTemplate}", String.join(UNION_TEMPLATE, queryParts));
    if (!modifierQueryParts.isEmpty()) {
      String modifierSql = finalSql + WHERE + String.join(AND, modifierQueryParts);
      if (!groupByModifier.isEmpty()) {
        modifierSql = modifierSql + groupByModifier.get(0);
      }
      finalSql = OUTER_SQL_TEMPLATE.replace("${innerSqlTemplate}", modifierSql);
    }

    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  private void buildModifierQuery(Modifier modifier,
                                  List<String> modifierQueryParts,
                                  List<String> groupByModifier,
                                  Map<String, QueryParameterValue> queryParams) {
    if (INCLUDED_MODIFIER_TYPES.contains(modifier.getName())) {
      ModifierType modifierType = modifier.getName();
      if (MODIFIER_OPERATORS.contains(modifier.getOperator())
        && modifier.getOperator().equals(Operator.BETWEEN)) {
        validateBetweenOperator(modifier);
        List<String> modifierParams = new ArrayList<>();
        for (String operand : modifier.getOperands()) {
          String modifierParameter = getModifierTypePrefix(modifierType)
            + getUniqueNamedParameterPostfix();
          modifierParams.add(" @" + modifierParameter);
          queryParams.put(modifierParameter, getQueryParameterValue(modifierType, operand));
        }
        modifierQueryParts.add(getModifierSqlTemplate(modifierType) + " "
          + OperatorUtils.getSqlOperator(modifier.getOperator())
          + String.join(AND, modifierParams) + "\n");
      } else if (MODIFIER_OPERATORS.contains(modifier.getOperator())
        && !modifier.getOperator().equals(Operator.BETWEEN)) {
        validateNotBetweenOperator(modifier);
        for (String operand : modifier.getOperands()) {
          String modifierParameter = getModifierTypePrefix(modifierType)
            + getUniqueNamedParameterPostfix();
          modifierQueryParts.add(getModifierSqlTemplate(modifierType) + " "
            + OperatorUtils.getSqlOperator(modifier.getOperator()) + " @" + modifierParameter + "\n");
          queryParams.put(modifierParameter, getQueryParameterValue(modifierType, operand));
        }
      } else {
        createBadRequest(modifier);
      }
    } else if (modifier.getName().equals(ModifierType.NUM_OF_OCCURRENCES)) {
      if (!groupByModifier.isEmpty()) {
        throw new BadRequestException(String.format(
          "%s modifier can only be used once.", modifier.getName()));
      }
      ModifierType modifierType = modifier.getName();
      for (String operand : modifier.getOperands()) {
        String occurrencesParameter = getModifierTypePrefix(modifierType)
          + getUniqueNamedParameterPostfix();
        groupByModifier.add(getModifierSqlTemplate(modifierType) + " "
          + OperatorUtils.getSqlOperator(modifier.getOperator()) + " @" + occurrencesParameter + "\n");
        queryParams.put(occurrencesParameter, getQueryParameterValue(modifierType, operand));
      }
    }
  }

  private Long newLong(String operand) {
    try {
      return new Long(operand);
    } catch (NumberFormatException nfe) {
      throw new BadRequestException(String.format(
        "Operand has to be convertable to long: %s", operand));
    }
  }

  private void validateBetweenOperator(Modifier modifier) {
    if (modifier.getOperands().size() != 2) {
      throw new BadRequestException(String.format(
        "Modifier: %s can only have 2 operands when using the %s operator",
        modifier.getName(),
        modifier.getOperator().name()));
    }
  }

  private void validateNotBetweenOperator(Modifier modifier) {
    if (modifier.getOperands().size() != 1) {
      throw new BadRequestException(String.format(
        "Modifier: %s can only have 1 operand when using the %s operator",
        modifier.getName(),
        modifier.getOperator().name()));
    }
  }

  private String getModifierTypePrefix(ModifierType modifierType){
    if (modifierType.equals(ModifierType.AGE_AT_EVENT)) {
      return AGE_AT_EVENT_PREFIX;
    } else if(modifierType.equals(ModifierType.EVENT_DATE)) {
      return EVENT_DATE_PREFIX;
    } else if(modifierType.equals(ModifierType.NUM_OF_OCCURRENCES)) {
      return OCCURRENCES_PREFIX;
    }
    return null;
  }

  private String getModifierSqlTemplate(ModifierType modifierType){
    if (modifierType.equals(ModifierType.AGE_AT_EVENT)) {
      return AGE_AT_EVENT_SQL_TEMPLATE;
    } else if(modifierType.equals(ModifierType.EVENT_DATE)) {
      return EVENT_DATE_SQL_TEMPLATE;
    } else if(modifierType.equals(ModifierType.NUM_OF_OCCURRENCES)) {
      return OCCURRENCES_SQL_TEMPLATE;
    }
    return null;
  }

  private QueryParameterValue getQueryParameterValue(ModifierType modifierType, String operand){
    if (modifierType.equals(ModifierType.AGE_AT_EVENT)) {
      return QueryParameterValue.int64(newLong(operand));
    } else if(modifierType.equals(ModifierType.EVENT_DATE)) {
      return QueryParameterValue.date(operand);
    } else if(modifierType.equals(ModifierType.NUM_OF_OCCURRENCES)) {
      return QueryParameterValue.int64(newLong(operand));
    }
    return null;
  }

  private void createBadRequest(Modifier modifier) {
    throw new BadRequestException(String.format(
      "Modifier: %s can only use the following operators: %s",
      modifier.getName(), String.join(", ",
        MODIFIER_OPERATORS.stream().map(Operator::name).collect(Collectors.toList()))));
  }

  private void buildGroupQuery(SearchParameter parameter,
                               List<String> queryParts,
                               Map<String, QueryParameterValue> queryParams,
                               List<String> codes,
                               boolean modifiersExist) {
    for (String code : codes) {
      buildInnerQuery(parameter,
        queryParts,
        queryParams,
        QueryParameterValue.string(code + "%"),
        GROUP_CODE_LIKE_TEMPLATE,
        modifiersExist);
    }
  }

  private void buildNotGroupQuery(SearchParameter parameter,
                                  List<String> queryParts,
                                  Map<String, QueryParameterValue> queryParams,
                                  List<String> codes,
                                  boolean modifiersExist) {
    buildInnerQuery(parameter,
      queryParts,
      queryParams,
      QueryParameterValue.array(codes.stream().toArray(String[]::new), String.class),
      CHILD_CODE_IN_CLAUSE_TEMPLATE,
      modifiersExist);
  }

  private void buildInnerQuery(SearchParameter parameter,
                               List<String> queryParts,
                               Map<String, QueryParameterValue> queryParams,
                               QueryParameterValue codes,
                               String groupOrChildSql,
                               boolean modifiersExist) {
    String domain = parameter.getDomain();
    String inClauseSql = "";
    final String uniqueName = getUniqueNamedParameterPostfix();
    final String cmUniqueParam = "cm" + uniqueName;
    final String procUniqueParam = "proc" + uniqueName;
    final String cmOrProcUniqueParam = "cmOrProc" + uniqueName;
    final String codesParameter = domain + uniqueName;

    ImmutableMap.Builder<String, String> paramNames = ImmutableMap.builder();
    paramNames.put("${tableName}", DomainTableEnum.getTableName(domain));
    paramNames.put("${tableId}", DomainTableEnum.getSourceConceptId(domain));
    paramNames.put("${conceptCodes}", "@" + codesParameter);
    if (modifiersExist) {
      paramNames.put("${entryDate}", DomainTableEnum.getEntryDate(domain));
    }

    queryParams.put(codesParameter, codes);
    if (parameter.getType().equals(ICD_10)) {
      queryParams.put(cmOrProcUniqueParam, QueryParameterValue.string(parameter.getSubtype()));
      inClauseSql = ICD10_VOCABULARY_ID_IN_CLAUSE_TEMPLATE;
      paramNames.put("${cmOrProc}", "@" + cmOrProcUniqueParam);
    } else {
      queryParams.put(cmUniqueParam, QueryParameterValue.string(TYPE_CM.get(parameter.getType())));
      queryParams.put(procUniqueParam, QueryParameterValue.string(TYPE_PROC.get(parameter.getType())));
      inClauseSql = ICD9_VOCABULARY_ID_IN_CLAUSE_TEMPLATE;
      paramNames.put("${cm}", "@" + cmUniqueParam);
      paramNames.put("${proc}", "@" + procUniqueParam);
    }

    String modifierSql = modifiersExist ? MODIFIER_COLUMNS_TEMPLATE : "";
    String modifierDistinctSql = modifiersExist ? DISTINCT : "";
    String innerSql = INNER_SQL_TEMPLATE.replace("${modifierColumns}", modifierSql);
    innerSql = innerSql.replace("${modifierDistinct}", modifierDistinctSql);

    queryParts.add(filterSql(innerSql + inClauseSql + groupOrChildSql, paramNames.build()));
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
