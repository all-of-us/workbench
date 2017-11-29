package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class BigQueryService {

    @Autowired
    private BigQuery bigquery;

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    /**
     * Execute the provided query using bigquery.
     *
     * @param query
     * @return
     */
    public QueryResult executeQuery(QueryJobConfiguration query) {

        // Execute the query
        QueryResponse response = null;
        try {
            response = bigquery.query(query);
        } catch (InterruptedException e) {
            throw new BigQueryException(500, "Something went wrong with BigQuery: " + e.getMessage());
        }

        // Wait for the job to finish
        while (!response.jobCompleted()) {
            response = bigquery.getQueryResults(response.getJobId());
        }

        // Check for errors.
        if (response.hasErrors()) {
            if (response.getExecutionErrors().size() != 0) {
                throw new BigQueryException(500, "Something went wrong with BigQuery: ", response.getExecutionErrors().get(0));
            }
        }

        return response.getResult();
    }

    public QueryJobConfiguration filterBigQueryConfig(QueryJobConfiguration queryJobConfiguration) {
        String returnSql = queryJobConfiguration.getQuery().replace("${projectId}", workbenchConfig.get().bigquery.projectId);
        returnSql = returnSql.replace("${dataSetId}", workbenchConfig.get().bigquery.dataSetId);
        return queryJobConfiguration
                .toBuilder()
                .setQuery(returnSql)
                .build();
    }

    public Map<String, Integer> getResultMapper(QueryResult result) {
        AtomicInteger index = new AtomicInteger();
        return result.getSchema().getFields().stream().collect(
                Collectors.toMap(Field::getName, s -> index.getAndIncrement()));
    }

    public Long getLong(List<FieldValue> row, int index) {
        return row.get(index).isNull() ? 0: row.get(index).getLongValue();
    }

    public String getString(List<FieldValue> row, int index) {
        return row.get(index).isNull() ? null : row.get(index).getStringValue();
    }

    public Boolean getBoolean(List<FieldValue> row, int index) {
        return row.get(index).getBooleanValue();
    }
}
