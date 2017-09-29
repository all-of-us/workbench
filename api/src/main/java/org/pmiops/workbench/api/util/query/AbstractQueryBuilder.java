package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;

@Service
public abstract class AbstractQueryBuilder {

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    public abstract QueryRequest buildQueryRequest(QueryParameters wrapper);

    public abstract String getType();

    protected String setBigQueryConfig(String sqlStatement, String tableName) {
        return String.format(sqlStatement,
                workbenchConfig.get().bigquery.projectId,
                workbenchConfig.get().bigquery.dataSetId,
                tableName);
    }
}
