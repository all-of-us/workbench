package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;

import java.util.UUID;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

    /**
     * Build a {@link QueryJobConfiguration} from the specified
     * {@link QueryParameters} provided.
     *
     * @param parameters
     * @return
     */
    public abstract QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters);

    public abstract FactoryKey getType();

    protected String getUniqueNamedParameterPostfix() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
