package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.BigQueryDataSetTableInfo;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.ServerUnavailableException;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BigQueryService {

  private static final Logger logger = Logger.getLogger(BigQueryService.class.getName());

  @Autowired private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Autowired private BigQuery defaultBigQuery;
  @Autowired private Duration defaultBigQueryTimeout;

  @VisibleForTesting
  protected BigQuery getBigQueryService() {
    DbCdrVersion cdrVersion = CdrVersionContext.nullableGetCdrVersion();
    if (cdrVersion == null) {
      return defaultBigQuery;
    }
    // If a query is being executed in the context of a CDR, it must be run within that project as
    // well. By default, the query would run in the Workbench App Engine project, which would
    // violate VPC-SC restrictions.
    return BigQueryOptions.newBuilder()
        .setProjectId(cdrVersion.getBigqueryProject())
        .build()
        .getService();
  }

  /** Execute the provided query using bigquery and wait for completion. */
  public TableResult executeQuery(QueryJobConfiguration query) {
    return executeQuery(query, defaultBigQueryTimeout.toMillis());
  }

  /** Execute the provided query using bigquery and wait for completion. */
  public TableResult executeQuery(QueryJobConfiguration query, long waitTime) {
    try {
      return startQuery(query).getQueryResults(BigQuery.QueryResultsOption.maxWaitTime(waitTime));
    } catch (InterruptedException e) {
      throw new BigQueryException(500, "Something went wrong with BigQuery: " + e.getMessage());
    }
  }

  /** Execute the provided query. */
  public Job startQuery(QueryJobConfiguration query) {
    if (workbenchConfigProvider.get().cdr.debugQueries) {
      logger.log(
          Level.INFO,
          "Executing query ({0}) with parameters ({1})",
          new Object[] {query.getQuery(), query.getNamedParameters()});
    }
    try {
      return getBigQueryService().create(JobInfo.of(query));
    } catch (BigQueryException e) {
      if (e.getCode() == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
        throw new ServerUnavailableException(
            "BigQuery was temporarily unavailable, try again later", e);
      } else if (e.getCode() == HttpServletResponse.SC_FORBIDDEN) {
        throw new ForbiddenException("BigQuery access denied", e);
      } else {
        throw new ServerErrorException(
            String.format(
                "An unexpected error occurred querying against BigQuery with "
                    + "query = (%s), params = (%s)",
                query.getQuery(), query.getNamedParameters()),
            e);
      }
    }
  }

  public QueryJobConfiguration filterBigQueryConfig(QueryJobConfiguration queryJobConfiguration) {
    DbCdrVersion cdrVersion = CdrVersionContext.getCdrVersionNotNull();
    String returnSql =
        queryJobConfiguration.getQuery().replace("${projectId}", cdrVersion.getBigqueryProject());
    returnSql = returnSql.replace("${dataSetId}", cdrVersion.getBigqueryDataset());
    return queryJobConfiguration.toBuilder().setQuery(returnSql).build();
  }

  public Map<String, Integer> getResultMapper(TableResult result) {
    if (result.getTotalRows() == 0) {
      return Collections.emptyMap();
    }
    AtomicInteger index = new AtomicInteger();
    return result.getSchema().getFields().stream()
        .collect(Collectors.toMap(Field::getName, s -> index.getAndIncrement()));
  }

  // TODO(jaycarlton): replace or merge these with FieldValues methods.
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
      return null;
    }
    DateTimeFormatter df =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz").withZone(ZoneId.of("UTC"));
    return df.format(FieldValues.getInstant(row.get(index)));
  }

  public String getDate(List<FieldValue> row, int index) {
    if (row.get(index).isNull()) {
      return null;
    }
    return row.get(index).getStringValue();
  }

  public FieldList getTableFieldsFromDomain(Domain domain) {
    DbCdrVersion cdrVersion = CdrVersionContext.getCdrVersionNotNull();
    TableId tableId =
        TableId.of(
            cdrVersion.getBigqueryProject(),
            cdrVersion.getBigqueryDataset(),
            BigQueryDataSetTableInfo.getTableName(domain));

    return getBigQueryService().getTable(tableId).getDefinition().getSchema().getFields();
  }

  public InsertAllResponse insertAll(InsertAllRequest insertAllRequest) {
    return defaultBigQuery.insertAll(insertAllRequest);
  }
}
