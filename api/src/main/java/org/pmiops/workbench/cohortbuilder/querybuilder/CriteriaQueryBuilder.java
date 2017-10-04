package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.springframework.stereotype.Service;

/**
 * CriteriaQueryBuilder is an object that builds {@link QueryRequest}
 * for BigQuery for all of the cohort builder criteria trees.
 */
@Service
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
                    "from `${projectId}.${dataSetId}.${tableName}`\n" +
                    "where parent_id = @parentId\n" +
                    "order by id asc";

    @Override
    public QueryRequest buildQueryRequest(QueryParameters parameters) {
        return QueryRequest
                .newBuilder(filterBigQueryConfig(CRITERIA_QUERY, parameters.getType().toLowerCase() + "_criteria"))
                .addNamedParameter("parentId", QueryParameterValue.int64(parameters.getParentId()))
                .setUseLegacySql(false)
                .build();
    }

    @Override
    public String getType() {
        return FactoryKey.CRITERIA.getName();
    }
}
