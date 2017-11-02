package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    "where DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), YEAR) = ${age}\n";

    private static final String UNION_TEMPLATE = "union distinct\n";

    @Override
    public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {

        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();

        for (SearchParameter parameter : parameters.getParameters()) {
            final String demoType = parameter.getSubtype().toLowerCase();
            final String parameterToReplace = "${" + demoType + "}";
            final String namedParameter = getUniqueNamedParameter(demoType);
            queryParts.add(getDemoSqlStatement(parameter.getSubtype())
                    .replace(parameterToReplace, "@" + namedParameter));

            queryParams.put(namedParameter,
                    parameter.getSubtype().equals("GEN") ?
                            QueryParameterValue.int64(parameter.getConceptId()) :
                            QueryParameterValue.int64(new Long(parameter.getValue())));
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

    private String getDemoSqlStatement(String subtype) {
        if (subtype.equals("GEN")) {
            return DEMO_GEN;
        } else if(subtype.equals("AGE")) {
            return DEMO_AGE;
        }
        return null;
    }
}
