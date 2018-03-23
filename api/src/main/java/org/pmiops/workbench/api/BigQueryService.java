package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class BigQueryService {

    @Autowired
    private BigQuery bigquery;

    /**
     * Execute the provided query using bigquery.
     */
    public QueryResult executeQuery(QueryJobConfiguration query) {

        // Execute the query
        QueryResponse response = null;
        try {
            response = bigquery.query(query, BigQuery.QueryOption.of(BigQuery.QueryResultsOption.maxWaitTime(60000L)));
        } catch (InterruptedException e) {
            throw new BigQueryException(500, "Something went wrong with BigQuery: " + e.getMessage());
        }

        return response.getResult();
    }

    public QueryJobConfiguration filterBigQueryConfig(QueryJobConfiguration queryJobConfiguration) {
        CdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
        if (cdrVersion == null) {
            throw new ServerErrorException("No CDR version specified");
        }
        String returnSql = queryJobConfiguration.getQuery().replace("${projectId}",
            cdrVersion.getBigqueryProject());
        returnSql = returnSql.replace("${dataSetId}", cdrVersion.getBigqueryDataset());
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
        if (row.get(index).isNull()) {
            throw new BigQueryException(500, "FieldValue is null at position: " + index);
        }
        return row.get(index).getLongValue();
    }

    public boolean isNull(List<FieldValue> row, int index) {
      return row.get(index).isNull();
    }

    public String getString(List<FieldValue> row, int index) {
        return row.get(index).isNull() ? null : row.get(index).getStringValue();
    }

    public Boolean getBoolean(List<FieldValue> row, int index) {
        return row.get(index).getBooleanValue();
    }

    public Date getDate(List<FieldValue> row, int index) {
        if (row.get(index).isNull()) {
            throw new BigQueryException(500, "FieldValue is null at position: " + index);
        }
        return java.sql.Date.from(Instant.ofEpochMilli(Double.valueOf(row.get(index).getStringValue()).longValue() * 1000));
    }
}
