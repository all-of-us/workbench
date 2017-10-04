package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoQueryBuilder extends AbstractQueryBuilder {

    private static final String DEMO_GEN =
            "select distinct concat(cast(p.person_id as string), ',',\n" +
                    "p.gender_source_value, ',',\n" +
                    "p.race_source_value) as val\n" +
                    "FROM `${projectId}.${dataSetId}.person` p\n" +
                    "where p.gender_concept_id = @genderConceptId\n";

    private static final String DEMO_AGE =
            "select distinct concat(cast(p.person_id as string), ',',\n" +
                    "p.gender_source_value, ',',\n" +
                    "p.race_source_value) as val\n" +
                    "FROM `${projectId}.${dataSetId}.person` p\n" +
                    "where DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), YEAR) = @age\n";

    private static final String UNION_TEMPLATE = "union distinct\n";

    private static final Map<String, String> DemoParamMap = new HashMap<>();
    static {
        DemoParamMap.put("DEMO_GEN", "genderConceptId");
        DemoParamMap.put("DEMO_AGE", "age");
    }

    @Override
    public QueryRequest buildQueryRequest(QueryParameters parameters) {

        Map<String, QueryParameterValue> queryParams = new HashMap<>();

        List<String> queryParts = new ArrayList<>();
        for (SearchParameter parameter : parameters.getParameters()) {
            queryParts.add(getDemoSqlStatement(parameter.getDomain()));

            queryParams.put(DemoParamMap.get(parameter.getDomain()),
                    parameter.getDomain().equals("DEMO_GEN") ?
                            QueryParameterValue.int64(parameter.getConceptId()) :
                            QueryParameterValue.int64(new Long(parameter.getValue())));
        }

        String finalSql = String.join(UNION_TEMPLATE, queryParts);

        return QueryRequest
                .newBuilder(filterBigQueryConfig(finalSql))
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)
                .build();
    }

    @Override
    public String getType() {
        return FactoryKey.DEMO.getName();
    }

    private String getDemoSqlStatement(String domain) {
        if (domain.equals("DEMO_GEN")) {
            return DEMO_GEN;
        } else if(domain.equals("DEMO_AGE")) {
            return DEMO_AGE;
        }
        return null;
    }
}
