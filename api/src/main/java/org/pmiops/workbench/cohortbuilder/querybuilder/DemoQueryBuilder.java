package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.SearchParameter;
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

    private static final String SELECT = "select distinct person_id\n" +
            "from `${projectId}.${dataSetId}.person` p\n" +
            "where\n";

    private static final String DEMO_GEN =
            "p.gender_concept_id in (${gen})\n";

    private static final String DEMO_AGE =
            "CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) ${operator}\n";

    private static final String DEMO_RACE =
            "p.race_concept_id in (${race})\n";

    private static final String AND_TEMPLATE = "and\n";

    public enum DEMOTYPE { GEN, AGE, DEC, RACE };

    @Override
    public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
        ListMultimap<String, Object> paramMap = getMappedParameters(parameters.getParameters());
        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();

        for (String key : paramMap.keySet()) {
            paramMap.get(key);

            if (key.equals(DEMOTYPE.GEN.name())) {
                final String namedParameter = key.toLowerCase() + getUniqueNamedParameterPostfix();
                queryParts.add(DEMO_GEN.replace("${gen}", "@" + namedParameter));
                queryParams.put(namedParameter, QueryParameterValue.array(paramMap.get(key).stream().toArray(Long[]::new), Long.class));
            } else if (key.equals(DEMOTYPE.RACE.name())) {
                final String namedParameter = key.toLowerCase() + getUniqueNamedParameterPostfix();
                queryParts.add(DEMO_RACE.replace("${race}", "@" + namedParameter));
                queryParams.put(namedParameter, QueryParameterValue.array(paramMap.get(key).stream().toArray(Long[]::new), Long.class));
            } else if (key.equals(DEMOTYPE.AGE.name())) {
                Optional<Attribute> attribute = Optional.ofNullable((Attribute)paramMap.get(key).get(0));
                if (attribute.isPresent() && !CollectionUtils.isEmpty(attribute.get().getOperands())) {
                    List<String> operandParts = new ArrayList<>();
                    for (String operand : attribute.get().getOperands()) {
                        final String namedParameter = key.toLowerCase() + getUniqueNamedParameterPostfix();
                        operandParts.add("@" + namedParameter);
                        queryParams.put(namedParameter, QueryParameterValue.int64(new Long(operand)));
                    }
                    queryParts.add(DEMO_AGE.replace("${operator}", attribute.get().getOperator())
                            + String.join(" and ", operandParts) + "\n");
                } else {
                    throw new IllegalArgumentException("Age must provide an operator and operands.");
                }
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

    protected ListMultimap<String, Object> getMappedParameters(List<SearchParameter> searchParameters) {
        ListMultimap<String, Object> mappedParameters = ArrayListMultimap.create();
        for (SearchParameter parameter : searchParameters)
            if (parameter.getSubtype().equals(DEMOTYPE.AGE.name())) {
                mappedParameters.put(parameter.getSubtype(), parameter.getAttribute());
            } else {
                mappedParameters.put(parameter.getSubtype(), parameter.getConceptId());
            }

        return mappedParameters;
    }
}
