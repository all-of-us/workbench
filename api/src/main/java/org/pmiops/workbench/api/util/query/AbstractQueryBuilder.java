package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Provider;

public abstract class AbstractQueryBuilder {

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    public abstract QueryRequest buildQueryRequest(QueryParameters wrapper);

    public abstract String getType();

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
