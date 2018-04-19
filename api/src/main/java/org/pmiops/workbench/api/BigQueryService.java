package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.ServerUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BigQueryService {

    private static final Logger logger = Logger.getLogger(BigQueryService.class.getName());

    @Autowired
    private BigQuery bigquery;

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfigProvider;

    /**
     * Execute the provided query using bigquery.
     */
    public QueryResult executeQuery(QueryJobConfiguration query) {

        // Execute the query
        QueryResponse response = null;
        if (workbenchConfigProvider.get().cdr.debugQueries) {
            logger.log(Level.INFO, "Executing query ({0}) with parameters ({1})",
                new Object[] { query.getQuery(), query.getNamedParameters()});
        }
        try {
            response = bigquery.query(query, BigQuery.QueryOption.of(BigQuery.QueryResultsOption.maxWaitTime(60000L)));
        } catch (InterruptedException e) {
            throw new BigQueryException(500, "Something went wrong with BigQuery: " + e.getMessage());
        } catch (BigQueryException e) {
            if (e.getCode() == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
                throw new ServerUnavailableException("BigQuery was temporarily unavailable, try again later", e);
            } else if (e.getCode() == HttpServletResponse.SC_FORBIDDEN) {
                throw new ForbiddenException("Access to the CDR is denied", e);
            } else {
                throw new ServerErrorException(
                    String.format("An unexpected error occurred querying against BigQuery with "
                            + "query = (%s), params = (%s)", query.getQuery(),
                        query.getNamedParameters()), e);
            }
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

    public String getDateTime(List<FieldValue> row, int index) {
        if (row.get(index).isNull()) {
            throw new BigQueryException(500, "FieldValue is null at position: " + index);
        }
        DateTimeFormatter df =
                DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss zzz").withZoneUTC();
        return df.print(row.get(index).getTimestampValue() / 1000L);
    }

    public String getDate(List<FieldValue> row, int index) {
        if (row.get(index).isNull()) {
            throw new BigQueryException(500, "FieldValue is null at position: " + index);
        }
        return row.get(index).getStringValue();
    }
}
