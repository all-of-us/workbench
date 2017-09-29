package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;

public class CriteriaQueryBuilder extends AbstractQueryBuilder {

    private static final String CRITERIA_QUERY =
            "select id,\n" +
                    "type,\n" +
                    "code,\n" +
                    "name,\n" +
                    "est_count,\n" +
                    "is_group,\n" +
                    "is_selectable,\n" +
                    "concept_id,\n" +
                    "domain_id\n" +
                    "from `%s.%s.%s`\n" +
                    "where parent_id = @parentId\n" +
                    "order by id asc";

    @Override
    public QueryRequest buildQueryRequest(QueryParameters wrapper) {
        return QueryRequest
                .newBuilder(setBigQueryConfig(CRITERIA_QUERY, wrapper.getType() + "_criteria"))
                .addNamedParameter("parentId", QueryParameterValue.int64(wrapper.getParentId()))
                .setUseLegacySql(false)
                .build();
    }

    @Override
    public String getType() {
        return FactoryKey.CRITERIA.name();
    }
}
