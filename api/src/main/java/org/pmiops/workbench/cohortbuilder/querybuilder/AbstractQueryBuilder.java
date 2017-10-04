package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Provider;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryRequest}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    /**
     * Build a {@link QueryRequest} from the specified
     * {@link QueryParameters} provided.
     *
     * @param parameters
     * @return
     */
    public abstract QueryRequest buildQueryRequest(QueryParameters parameters);

    public abstract FactoryKey getType();

    protected String filterBigQueryConfig(String sqlStatement, String tableName) {
        String returnSql = sqlStatement.replace("${projectId}", workbenchConfig.get().bigquery.projectId);
        returnSql = returnSql.replace("${dataSetId}", workbenchConfig.get().bigquery.dataSetId);
        if (tableName != null) {
            returnSql = returnSql.replace("${tableName}", tableName);
        }
        return returnSql;
    }

    protected String filterBigQueryConfig(String sqlStatement) {
        return filterBigQueryConfig(sqlStatement, null);

    }
}
