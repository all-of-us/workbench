package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DemoQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the following criteria types:
 * DEMO_GEN, DEMO_AGE, DEMO_RACE and DEMO_DEC.
 */
@Service
public class DemoQueryBuilder extends AbstractQueryBuilder {

  private static final String DECEASED = "Deceased";

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

  public enum DemoType {
    GEN, AGE, DEC, RACE, ETH;

    public static DemoType fromValue(String subtype) {
      for (DemoType demoType : DemoType.values()) {
        if (demoType.name().equals(subtype)) {
          return demoType;
        }
      }
      return null;
    }
  }

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    ListMultimap<DemoType, Object> paramMap = getMappedParameters(parameters.getParameters());
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    List<String> queryParts = new ArrayList<>();
    boolean containsAge = false;

    for (DemoType key : paramMap.keySet()) {
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
          Optional<Attribute> attribute = Optional.ofNullable((Attribute) paramMap.get(key).get(0));
          if (attribute.isPresent() && !CollectionUtils.isEmpty(attribute.get().getOperands())) {
            List<String> operandParts = new ArrayList<>();
            for (String operand : attribute.get().getOperands()) {
              String ageNamedParameter = key.name().toLowerCase() + getUniqueNamedParameterPostfix();
              operandParts.add("@" + ageNamedParameter);
              queryParams.put(ageNamedParameter, QueryParameterValue.int64(new Long(operand)));
            }
            queryParts.add(DEMO_AGE.replace("${operator}", OperatorUtils.getSqlOperator(attribute.get().getOperator()))
              + String.join(" and ", operandParts) + "\n");
            queryParts.add(AGE_NOT_EXISTS_DEATH);
          } else {
            throw new BadRequestException("Age must provide an operator and operands.");
          }
          break;
        case DEC:
          if (DECEASED.equals(paramMap.get(key).get(0))) {
            queryParts.add(DEMO_DEC);
          } else {
            throw new BadRequestException("Dec must provide a value of: " + DECEASED);
          }
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

  protected ListMultimap<DemoType, Object> getMappedParameters(List<SearchParameter> searchParameters) {
    ListMultimap<DemoType, Object> mappedParameters = ArrayListMultimap.create();
    for (SearchParameter parameter : searchParameters)
      if (parameter.getSubtype().equals(DemoType.AGE.name())) {
        mappedParameters.put(DemoType.AGE, parameter.getAttributes().isEmpty() ? null : parameter.getAttributes().get(0));
      } else if (parameter.getSubtype().equals(DemoType.DEC.name())) {
        mappedParameters.put(DemoType.DEC, parameter.getValue());
      } else {
        mappedParameters.put(DemoType.fromValue(parameter.getSubtype()), parameter.getConceptId());
      }

    return mappedParameters;
  }
}
