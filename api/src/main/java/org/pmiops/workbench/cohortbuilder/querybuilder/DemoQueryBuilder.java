package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.ParameterPredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.AttributePredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.Validation.from;

/**
 * DemoQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the following criteria types:
 * DEMO_GEN, DEMO_AGE, DEMO_RACE and DEMO_DEC.
 */
@Service
public class DemoQueryBuilder extends AbstractQueryBuilder {

  protected static final String AGE_DEC_MESSAGE = "Bad Request: Cannot search age and deceased together.";
  protected static final String DEC = "Dec";

  private static final String SELECT = "select person_id\n" +
    "from `${projectId}.${dataSetId}.person` p\n" +
    "where\n";

  private static final String DEMO_GEN =
    "p.gender_concept_id in unnest(${gen})\n";

  private static final String DEMO_AGE =
    "CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) ${operator}\n";

  private static final String DEMO_RACE =
    "p.race_concept_id in unnest(${race})\n";

  private static final String DEMO_DEC =
    "exists (\n" +
      "SELECT 'x' FROM `${projectId}.${dataSetId}.death` d\n" +
      "where d.person_id = p.person_id)\n";

  private static final String AGE_NOT_EXISTS_DEATH =
    "not exists (\n" +
      "SELECT 'x' FROM `${projectId}.${dataSetId}.death` d\n" +
      "where d.person_id = p.person_id)\n";

  private static final String DEMO_ETH =
    "p.ethnicity_concept_id in unnest(${eth})\n";

  private static final String AND_TEMPLATE = "and\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    validateSearchParameters(parameters.getParameters());
    ListMultimap<TreeSubType, Object> paramMap = getMappedParameters(parameters.getParameters());
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    List<String> queryParts = new ArrayList<>();

    for (TreeSubType key : paramMap.keySet()) {
      String namedParameter = key.name().toLowerCase() + getUniqueNamedParameterPostfix();

      switch (key) {
        case GEN:
          queryParts.add(DEMO_GEN.replace("${gen}", "@" + namedParameter));
          Long[] demoIds = paramMap.get(key).stream().filter(Long.class::isInstance).map(Long.class::cast).toArray(Long[]::new);
          queryParams.put(namedParameter, QueryParameterValue.array(demoIds, Long.class));
          break;
        case RACE:
          queryParts.add(DEMO_RACE.replace("${race}", "@" + namedParameter));
          Long[] raceIds = paramMap.get(key).stream().filter(Long.class::isInstance).map(Long.class::cast).toArray(Long[]::new);
          queryParams.put(namedParameter, QueryParameterValue.array(raceIds, Long.class));
          break;
        case AGE:
          Attribute attribute = (Attribute) paramMap.get(key).get(0);
          List<String> operandParts = new ArrayList<>();
          for (String operand : attribute.getOperands()) {
            String ageNamedParameter = key.name().toLowerCase() + getUniqueNamedParameterPostfix();
            operandParts.add("@" + ageNamedParameter);
            queryParams.put(ageNamedParameter, QueryParameterValue.int64(new Long(operand)));
          }
          queryParts.add(DEMO_AGE.replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
            + String.join(" and ", operandParts) + "\n");
          queryParts.add(AGE_NOT_EXISTS_DEATH);
          break;
        case DEC:
          queryParts.add(DEMO_DEC);
          break;
        case ETH:
          queryParts.add(DEMO_ETH.replace("${eth}", "@" + namedParameter));
          Long[] ethIds = paramMap.get(key).stream().filter(Long.class::isInstance).map(Long.class::cast).toArray(Long[]::new);
          queryParams.put(namedParameter, QueryParameterValue.array(ethIds, Long.class));
          break;
        default:
          break;
      }
    }

    String finalSql = SELECT + String.join(AND_TEMPLATE, queryParts);

    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.DEMO;
  }

  private void validateSearchParameters(List<SearchParameter> parameters) {
    from(parametersEmpty()).test(parameters).throwException(EMPTY_MESSAGE, PARAMETERS);
    from(containsAgeAndDec()).test(parameters).throwException(AGE_DEC_MESSAGE);
    parameters
      .forEach(param -> {
        from(typeBlank().or(demoTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, TYPE, param.getType());
        from(subtypeBlank().or(demoSubtypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, SUBTYPE, param.getSubtype());
        from(subtypeDec().and(valueNotDec())).test(param).throwException(NOT_VALID_MESSAGE, DEC, param.getValue());
        if (subtypeAge().test(param)) {
          from(attributesEmpty()).test(param).throwException(EMPTY_MESSAGE, ATTRIBUTES);
          param.getAttributes().forEach(attr -> {
            from(operatorNull()).test(attr).throwException(NOT_VALID_MESSAGE, OPERATOR, attr.getOperator());
            from(operandsEmpty()).test(attr).throwException(EMPTY_MESSAGE, OPERANDS);
            from(notBetweenOperator().and(operandsNotOne())).test(attr).throwException(ONE_OPERAND_MESSAGE);
            from(betweenOperator().and(operandsNotTwo())).test(attr).throwException(TWO_OPERAND_MESSAGE);
            from(operandsNotNumbers()).test(attr).throwException(OPERANDS_NUMERIC_MESSAGE);
          });
        }
      });
  }

  protected ListMultimap<TreeSubType, Object> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<TreeSubType, Object> mappedParameters = ArrayListMultimap.create();
    for (SearchParameter parameter : searchParameters)
      if (parameter.getSubtype().equals(TreeSubType.AGE.name())) {
        mappedParameters.put(TreeSubType.AGE, parameter.getAttributes().isEmpty() ? null : parameter.getAttributes().get(0));
      } else if (parameter.getSubtype().equals(TreeSubType.DEC.name())) {
        mappedParameters.put(TreeSubType.DEC, parameter.getValue());
      } else {
        mappedParameters.put(TreeSubType.fromValue(parameter.getSubtype()), parameter.getConceptId());
      }
    return mappedParameters;
  }
}
