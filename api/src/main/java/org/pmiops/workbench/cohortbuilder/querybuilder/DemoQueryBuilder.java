package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.betweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.notBetweenOperator;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotNumbers;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotOne;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operandsNotTwo;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.AttributePredicates.operatorNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.attributesEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.containsAgeAndDec;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.demoSubtypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.demoTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.subTypeGenRaceEth;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.subtypeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.AGE_DEC_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ATTRIBUTES;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.ONE_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERANDS_NUMERIC_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.OPERATOR;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.SUBTYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TWO_OPERAND_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.operatorText;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * DemoQueryBuilder builds SQL for BigQuery for the following criteria types: DEMO_GEN, DEMO_AGE, DEMO_RACE and DEMO_DEC.
 */
@Service
public class DemoQueryBuilder extends AbstractQueryBuilder {

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

  /**
   * {@inheritDoc}
   */
  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem searchGroupItem,
                           TemporalMention temporalMention) {
    from(parametersEmpty()).test(searchGroupItem.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    from(containsAgeAndDec()).test(searchGroupItem.getSearchParameters()).throwException(AGE_DEC_MESSAGE);
    ListMultimap<TreeSubType, Object> paramMap = getMappedParameters(searchGroupItem.getSearchParameters());
    List<String> queryParts = new ArrayList<>();

    for (TreeSubType key : paramMap.keySet()) {

      switch (key) {
        case GEN:
          Long[] demoIds = paramMap.get(key).stream().filter(Long.class::isInstance).map(Long.class::cast).toArray(Long[]::new);
          String genParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(demoIds, Long.class));
          queryParts.add(DEMO_GEN.replace("${gen}", "@" + genParameter));
          break;
        case RACE:
          Long[] raceIds = paramMap.get(key).stream().filter(Long.class::isInstance).map(Long.class::cast).toArray(Long[]::new);
          String raceParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(raceIds, Long.class));
          queryParts.add(DEMO_RACE.replace("${race}", "@" + raceParameter));
          break;
        case AGE:
          Attribute attribute = (Attribute) paramMap.get(key).get(0);
          List<String> operandParts = new ArrayList<>();
          for (String operand : attribute.getOperands()) {
            String ageNamedParameter = addQueryParameterValue(queryParams, QueryParameterValue.int64(new Long(operand)));
            operandParts.add("@" + ageNamedParameter);
          }
          queryParts.add(DEMO_AGE.replace("${operator}", OperatorUtils.getSqlOperator(attribute.getOperator()))
            + String.join(" and ", operandParts) + "\n");
          queryParts.add(AGE_NOT_EXISTS_DEATH);
          break;
        case DEC:
          queryParts.add(DEMO_DEC);
          break;
        case ETH:
          Long[] ethIds = paramMap.get(key).stream().filter(Long.class::isInstance).map(Long.class::cast).toArray(Long[]::new);
          String ethParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(ethIds, Long.class));
          queryParts.add(DEMO_ETH.replace("${eth}", "@" + ethParameter));
          break;
        default:
          break;
      }
    }
    return SELECT + String.join(AND_TEMPLATE, queryParts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FactoryKey getType() {
    return FactoryKey.DEMO;
  }

  private void validateSearchParameter(SearchParameter param) {
    from(typeBlank().or(demoTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(subtypeBlank().or(demoSubtypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, param.getSubtype());
    from(subTypeGenRaceEth().and(conceptIdNull())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
  }

  private void validateAttributes(SearchParameter param) {
    from(attributesEmpty()).test(param).throwException(EMPTY_MESSAGE, ATTRIBUTES);
    param.getAttributes().forEach(attr -> {
      String name = attr.getName();
      String oper = operatorText.get(attr.getOperator());
      from(operatorNull()).test(attr).throwException(NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, oper);
      from(operandsEmpty()).test(attr).throwException(EMPTY_MESSAGE, OPERANDS);
      from(notBetweenOperator().and(operandsNotOne())).test(attr).throwException(ONE_OPERAND_MESSAGE, ATTRIBUTE, name, oper);
      from(betweenOperator().and(operandsNotTwo())).test(attr).throwException(TWO_OPERAND_MESSAGE, ATTRIBUTE, name, oper);
      from(operandsNotNumbers()).test(attr).throwException(OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE, name);
    });
  }

  protected ListMultimap<TreeSubType, Object> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<TreeSubType, Object> mappedParameters = ArrayListMultimap.create();
    for (SearchParameter parameter : searchParameters) {
      validateSearchParameter(parameter);
      if (parameter.getSubtype().equals(TreeSubType.AGE.name())) {
        validateAttributes(parameter);
        mappedParameters.put(TreeSubType.AGE, parameter.getAttributes().isEmpty() ? null : parameter.getAttributes().get(0));
      } else if (parameter.getSubtype().equals(TreeSubType.DEC.name())) {
        mappedParameters.put(TreeSubType.DEC, parameter.getValue());
      } else {
        mappedParameters.put(TreeSubType.fromValue(parameter.getSubtype()), parameter.getConceptId());
      }
    }
    return mappedParameters;
  }
}
