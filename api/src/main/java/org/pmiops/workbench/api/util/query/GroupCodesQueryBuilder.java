package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
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
    public QueryRequest buildQueryRequest(QueryParameters parameters) {

        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();
        final List<String> codes = parameters.getCodes();
        IntStream.range(0, codes.size())
                .forEach(i -> {
                    queryParams.put(String.format("code%d", i), QueryParameterValue.string(codes.get(i)));
                    queryParts.add("code like @" + String.format("code%d", i++));
                });
        String finalSql = GROUP_CODES_QUERY.replace("${codes}", String.join(" or ", queryParts));

        return QueryRequest.newBuilder(filterBigQueryConfig(finalSql, parameters.getType().toLowerCase() + "_criteria"))
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)     // required for queries that use named parameters
                .build();
    }

    @Override
    public String getType() {
        return FactoryKey.GROUP_CODES.getName();
    }
}
