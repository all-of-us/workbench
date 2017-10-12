package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryRequest;

import java.util.UUID;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryRequest}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

    /**
     * Build a {@link QueryRequest} from the specified
     * {@link QueryParameters} provided.
     *
     * @param parameters
     * @return
     */
    public abstract QueryRequest buildQueryRequest(QueryParameters parameters);

    public abstract FactoryKey getType();

    protected String getUniqueNamedParameter(String parameterName) {
        return parameterName + UUID.randomUUID().toString().replaceAll("-", "");
    }
}
