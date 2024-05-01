package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    // If a query is being executed in the context of a CDR, it must be run within that project as
    // well. By default, the query would run in the Workbench App Engine project, which would
    // violate VPC-SC restrictions.
    return CdrVersionContext.maybeGetBigQueryProject()
        .map(projectId -> BigQueryOptions.newBuilder().setProjectId(projectId).build().getService())
        .orElse(defaultBigQuery);
  }

  public TableResult filterBigQueryConfigAndExecuteQuery(QueryJobConfiguration query) {
    return executeQuery(filterBigQueryConfig(query), defaultBigQueryTimeout.toMillis());
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
    DbCdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
    String returnSql =
        queryJobConfiguration
            .getQuery()
            .replace("${projectId}", cdrVersion.getBigqueryProject())
            .replace("${dataSetId}", cdrVersion.getBigqueryDataset());
    return queryJobConfiguration.toBuilder().setQuery(returnSql).build();
  }

  public FieldList getTableFieldsFromDomain(Domain domain) {
    DbCdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
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
