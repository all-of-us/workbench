package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * TODO: this is temporary and will be removed when we figure out the conceptId mappings
 */
@Service
public class GroupCodesQueryBuilder extends AbstractQueryBuilder {

    private static final String GROUP_CODES_QUERY =
            "select code,\n" +
                    "domain_id as domainId\n" +
                    "from `${projectId}.${dataSetId}.${tableName}`\n" +
                    "where (${codes})\n" +
                    "and is_selectable = TRUE and is_group = FALSE order by code asc";

    @Override
    public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {

        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();
        final List<SearchParameter> codes = parameters.getParameters();
        IntStream.range(0, codes.size())
                .forEach(i -> {
                    queryParams.put(String.format("code%d", i), QueryParameterValue.string(codes.get(i).getValue() + "%"));
                    queryParts.add("code like @" + String.format("code%d", i++));
                });
        String finalSql = GROUP_CODES_QUERY.replace("${codes}", String.join(" or ", queryParts));

        return QueryJobConfiguration
                .newBuilder(finalSql.replace("${tableName}", parameters.getType().toLowerCase() + "_criteria"))
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)     // required for queries that use named parameters
                .build();
    }

    @Override
    public FactoryKey getType() {
        return FactoryKey.GROUP_CODES;
    }
}
