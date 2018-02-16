package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
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

    private static final String DEMO_GEN =
            "select distinct person_id\n" +
                    "from `${projectId}.${dataSetId}.person` p\n" +
                    "where p.gender_concept_id = ${gen}\n";

    private static final String DEMO_AGE =
            "select distinct person_id\n" +
                    "from `${projectId}.${dataSetId}.person` p\n" +
                    "where CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) ${operator}\n";

    private static final String UNION_TEMPLATE = "union distinct\n";

    public enum DEMOTYPE { GEN, AGE, DEC, RACE };

    @Override
    public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {

        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();

        for (SearchParameter parameter : parameters.getParameters()) {
            if (parameter.getSubtype().equals(DEMOTYPE.GEN.name())) {
                final String namedParameter = getUniqueNamedParameter(parameter.getSubtype().toLowerCase());
                queryParts.add(DEMO_GEN.replace("${gen}", "@" + namedParameter));
                queryParams.put(namedParameter, QueryParameterValue.int64(parameter.getConceptId()));
            } else if (parameter.getSubtype().equals(DEMOTYPE.AGE.name())) {
                Optional<Attribute> attribute = Optional.ofNullable(parameter.getAttribute());
                if (attribute.isPresent() && !CollectionUtils.isEmpty(attribute.get().getOperands())) {
                    List<String> operandParts = new ArrayList<>();
                    for (String operand : attribute.get().getOperands()) {
                        final String namedParameter = getUniqueNamedParameter(parameter.getSubtype().toLowerCase());
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

        String finalSql = String.join(UNION_TEMPLATE, queryParts);

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
}
